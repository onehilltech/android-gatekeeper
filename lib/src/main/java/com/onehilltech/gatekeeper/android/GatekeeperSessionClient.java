package com.onehilltech.gatekeeper.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.google.gson.reflect.TypeToken;
import com.onehilltech.backbone.app.Promise;
import com.onehilltech.backbone.http.HttpError;
import com.onehilltech.backbone.http.Resource;
import com.onehilltech.backbone.http.retrofit.ResourceEndpoint;
import com.onehilltech.backbone.http.retrofit.gson.GsonResourceManager;
import com.onehilltech.gatekeeper.android.http.JsonAccount;
import com.onehilltech.gatekeeper.android.http.JsonBearerToken;
import com.onehilltech.gatekeeper.android.http.JsonChangePassword;
import com.onehilltech.gatekeeper.android.model.ClientToken;
import com.onehilltech.gatekeeper.android.model.UserToken;
import com.onehilltech.gatekeeper.android.model.UserToken$Table;
import com.raizlabs.android.dbflow.runtime.FlowContentObserver;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.structure.BaseModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Converter;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * @class GatekeeperSessionClient
 */
public class GatekeeperSessionClient
{
  /**
   * @class Builder
   *
   * Build a GatekeeperSessionClient object.
   */
  public static final class Builder
  {
    private GatekeeperClient client_;
    private Context context_;
    private String userAgent_;

    public Builder (Context context)
    {
      this.context_ = context;
      this.client_ = new GatekeeperClient.Builder (context).build ();
    }

    public Builder setGatekeeperClient (GatekeeperClient client)
    {
      this.client_ = client;
      return this;
    }

    public Builder setUserAgent (String userAgent)
    {
      this.userAgent_ = userAgent;
      return this;
    }

    public GatekeeperSessionClient build ()
    {
      GatekeeperSessionClient client = new GatekeeperSessionClient (this.context_, this.client_);

      if (this.userAgent_ != null)
        client.setUserAgent (this.userAgent_);

      return client;
    }
  }

  /**
   * @interface Listener
   *
   * Listener that receives notifications about changes to the signIn state.
   */
  public interface Listener
  {
    /**
     * The client has signed in.
     *
     * @param client        Session client
     */
    void onSignedIn (GatekeeperSessionClient client);

    /**
     * The client has signed out.
     *
     * @param client        Session client
     */
    void onSignedOut (GatekeeperSessionClient client);

    /**
     * Let the application know it needs to reauthenticate the user.
     *
     * @param client
     */
    void onReauthenticate (GatekeeperSessionClient client, HttpError reason);
  }

  private OkHttpClient httpClient_;

  /// The user token for the current session.
  private UserToken userToken_;

  /// The client token for the session client.
  private ClientToken clientToken_;

  private Retrofit retrofit_;

  private Listener listener_;

  private final FlowContentObserver userTokenObserver_ = new FlowContentObserver ();

  private final Context context_;

  private final GatekeeperClient client_;

  private final UserMethods userMethods_;

  private String userAgent_;

  private OkHttpClient userClient_;

  private Retrofit userEndpoint_;

  private final Logger logger_ = LoggerFactory.getLogger (GatekeeperSessionClient.class);

  private final Converter<ResponseBody, Resource> resourceConverter_;

  private static final ArrayList <String> REAUTHENTICATE_ERROR_CODES = new ArrayList<> ();

  private boolean isSigningIn_ = false;

  /**
   * Initializing constructor.
   *
   * @param context         Target context
   * @param client          GatekeeperClient object
   */
  private GatekeeperSessionClient (Context context, GatekeeperClient client)
  {
    this.context_ = context;
    this.client_ = client;

    // Build a new HttpClient for the user session. This client is responsible for
    // adding the authentication header to each request.
    OkHttpClient.Builder builder = client.getHttpClient ().newBuilder ();
    builder.addInterceptor (this.userAuthorizationHeader_);
    builder.addInterceptor (this.responseInterceptor_);

    this.httpClient_ = builder.build ();

    // Build the Retrofit object for this client.
    this.retrofit_ = new Retrofit.Builder ()
        .baseUrl (client.getBaseUrlWithVersion ())
        .addConverterFactory (GsonConverterFactory.create (client.getGson ()))
        .client (this.httpClient_)
        .build ();

    this.resourceConverter_ = this.retrofit_.responseBodyConverter (Resource.class, new Annotation[0]);
    this.userMethods_ = this.retrofit_.create (UserMethods.class);

    // Load the one and only user token from the database. We also want to
    // observe the user token table for changes. These changes could be logging
    // out or refreshing the user token.
    String username = GatekeeperSession.get (context).getUsername ();

    if (username != null)
    {
      this.userToken_ =
          SQLite.select ()
                .from (UserToken.class)
                .where (UserToken$Table.username.eq (username))
                .querySingle ();
    }

    this.userTokenObserver_.registerForContentChanges (context, UserToken.class);
    this.userTokenObserver_.addModelChangeListener ((table, action, primaryKeyValues) -> {
      if (action == BaseModel.Action.DELETE)
      {
        this.logger_.info ("The user token has been deleted from the database");

        if (this.userToken_ != null)
          this.userToken_ = null;

        Message msg = this.uiHandler_.obtainMessage (MSG_ON_LOGOUT);
        msg.sendToTarget ();
      }
      else
      {
        // Get the username from the sql condition. We then need to load the
        // user token from the database that matches the username.
        String value = (String) primaryKeyValues[0].value ();

        if (this.userToken_ == null || !this.userToken_.username.equals (value))
        {
          this.logger_.info ("Loading token for the user");

          // Load the token for the user that was logged in.
          this.userToken_ =
              SQLite.select ()
                    .from (UserToken.class)
                    .where (UserToken$Table.username.eq (value))
                    .querySingle ();
        }

        if (action == BaseModel.Action.SAVE)
        {
          this.logger_.info ("Notifying client the user token has been saved");

          // The token is inserted into the database when the user is logged in. The
          // token is updated in the database when the it is refreshed from the server.

          Message msg = this.uiHandler_.obtainMessage (MSG_ON_LOGIN);
          msg.sendToTarget ();
        }
      }
    });

    this.userClient_ =
        this.httpClient_.newBuilder ()
                        .addInterceptor (this.userAuthorizationHeader_)
                        .build ();

    this.userEndpoint_ =
        new Retrofit.Builder ()
            .baseUrl (this.client_.getBaseUrlWithVersion ())
            .addConverterFactory (GsonConverterFactory.create (this.client_.getGson ()))
            .client (this.userClient_)
            .build ();
  }

  /**
   * Set the User-Agent for the client.
   *
   * @param userAgent
   */
  void setUserAgent (String userAgent)
  {
    this.userAgent_ = userAgent;
  }

  /**
   * Get the User-Agent value.
   *
   * @return
   */
  public String getUserAgent ()
  {
    return this.userAgent_;
  }

  /**
   * Get the username for the current session.
   *
   * @return
   */
  public String getUsername ()
  {
    return GatekeeperSession.get (this.context_).getUsername ();
  }

  /**
   * Set the listener object
   *
   * @param listener      Listener object
   */
  public void setListener (Listener listener)
  {
    this.listener_ = listener;
  }

  /**
   * Get the access token for the current session.
   *
   * @return
   */
  public String getAccessToken ()
  {
    return this.userToken_.accessToken;
  }

  /**
   * Get the listener object
   *
   * @return              A Listener object
   */
  public Listener getListener ()
  {
    return this.listener_;
  }

  /**
   * Get the OkHttpClient for the current user session.
   *
   * @return
   */
  public OkHttpClient getUserClient ()
  {
    return this.userClient_;
  }

  /**
   * Get the user endpoint for the client. This endpoint will adds the Authorization header
   * to all request for the current user.
   *
   * @return
   */
  public Retrofit getUserEndpoint ()
  {
    return this.userEndpoint_;
  }

  /**
   * Ensure the user is signed in to the session. If not, then we show the sign in
   * activity that prompts the user to sign in.
   *
   * @param activity        Parent activity
   * @param signIn          Sign in activity class
   * @return                True if signed in; otherwise false
   */
  public boolean ensureSignedIn (Activity activity, Class <? extends Activity> signIn)
  {
    return this.ensureSignedIn (activity, new Intent (this.context_, signIn));
  }

  /**
   * Ensure the user is signed in to the session. If not, then we show the sign in
   * activity that prompts the user to sign in.
   *
   * @param activity            Parent activity
   * @param signInIntent        Sign in activity intent
   * @return
   */
  public boolean ensureSignedIn (Activity activity, Intent signInIntent)
  {
    if (this.isSignedIn ())
      return true;

    this.forceSignIn (activity, signInIntent);
    return false;
  }

  /**
   * Force the session client to sign in the current user.
   *
   * @param activity
   * @param signIn
   */
  public void forceSignIn (Activity activity, Class <? extends Activity> signIn)
  {
    this.forceSignIn (activity, new Intent (activity, signIn));
  }

  /**
   * Force the session client to sign in the current user.
   *
   * @param activity
   * @param signInIntent
   */
  public void forceSignIn (Activity activity, Intent signInIntent)
  {
    if (this.isSigningIn_)
      return;

    this.isSigningIn_ = true;
    this.logger_.info ("Forcing sign in");

    // Force the user to signout.
    this.completeSignOut ();

    signInIntent.putExtra (GatekeeperSignInActivity.ARG_REDIRECT_INTENT, activity.getIntent ());
    this.context_.startActivity (signInIntent);

    // Finish the current activity.
    activity.finish ();
  }

  /**
   * The user is signing in.
   *
   * @return
   */
  public boolean isSigningIn ()
  {
    return this.isSigningIn_;
  }

  /**
   * Cleanup the object. This is to be called in the onDestroy() method of the
   * Context (e.g., Activity, Fragment, or Service) that created it.
   */
  public void onDestroy ()
  {
    this.userTokenObserver_.unregisterForContentChanges (this.context_);
  }

  /**
   * Get the underlying HTTP client.
   *
   * @return
   */
  public OkHttpClient getHttpClient ()
  {
    return this.httpClient_;
  }

  /**
   * Test is client has a user signed in.
   */
  public boolean isSignedIn ()
  {
    return this.userToken_ != null;
  }

  /**
   * Force the client to refresh its token.
   *
   * @return
   */
  public Promise <Void> refreshToken ()
  {
    if (!this.isSignedIn ())
      return Promise.reject (new IllegalStateException ("User must be signed in to refresh token"));

    return new Promise<> (settlement -> {
      this.client_.refreshToken (this.userToken_.refreshToken)
                  .then ((token, cont) -> {
                    // The token was refreshed. Let's save the token, and then resolve this
                    // promise so the client can continue on.
                    this.userToken_.accessToken = token.accessToken;
                    this.userToken_.refreshToken = token.refreshToken;
                    this.userToken_.update ();

                    settlement.resolve (null);
                  }, (reason, cont) -> settlement.reject (reason));
    });
  }

  /**
   * Complete the signOut process.
   */
  private void completeSignOut ()
  {
    if (this.userToken_ == null)
      return;

    // Delete the current session information.
    GatekeeperSession.getCurrent (this.context_).edit ().delete ();

    // Delete the token from the database. This will cause all session clients
    // listening for changes to be notified of the change.
    this.userToken_.delete ();
    this.userToken_ = null;
  }

  public HttpError getError (ResponseBody errorBody)
      throws IOException
  {
    return this.client_.getError (errorBody);
  }

  /**
   * Sign in a user.
   *
   * @param username          Username for the user
   * @param password          Password for the user
   */
  public Promise <Void> signIn (final String username, String password)
  {
    if (this.isSignedIn ())
      return Promise.reject (new IllegalStateException ("User is already signed in"));

    return new Promise<> (settlement -> {
      final Promise.OnResolved <JsonBearerToken, Void> completeSignIn =
          (token, cont) -> cont.with (this.completeSignIn (username, token));

      this.client_
          .getUserToken (username, password)
          .then (completeSignIn)
          .then ((value, cont) -> settlement.resolve (null),
                 (reason, cont) -> settlement.reject (reason));
    });
  }

  /**
   * Complete the signIn process by storing the information in the database, and
   * notifying all parties that the signIn is complete.
   *
   * @param username            Username that signed in
   * @param jsonToken           Access token for the user
   */
  private Promise <Void> completeSignIn (String username, JsonBearerToken jsonToken)
  {
    return new Promise<> (settlement -> {
      // Store the user token, but do not save it. We do not want to save the
      // access token until we are certain we have information about the current
      // user stored in the session.
      this.userToken_ = UserToken.fromToken (username, jsonToken);

      // Get information about the current user.
      this.logger_.info ("Requesting my account information");

      this.getMyAccount ()
          .then ((r, cont)-> {
            // Update the session information, and save the user token. At this point, we
            // are done with the login process and can return control to the client.
            this.logger_.info ("Saving session info");

            JsonAccount account = r.get ("account");

            GatekeeperSession session = GatekeeperSession.get (this.context_);

            session.edit ()
                   .setUsername (account.username)
                   .setUserId (account._id)
                   .commit ();

            // Save the user token to make the sign in complete.
            this.userToken_.save ();

            settlement.resolve (null);
          }, (reason, cont) -> settlement.reject (reason));
    });
  }

  /**
   * Sign out the current user
   */
  public Promise <Boolean> signOut ()
  {
    if (this.userToken_ == null)
      return Promise.reject (new IllegalStateException ("User is already signed out"));

    return new Promise<> (settlement -> {
      this.logger_.info ("Signing out current user");

      this.userMethods_.logout ().enqueue (new Callback<Boolean> ()
      {
        @Override
        public void onResponse (Call<Boolean> call, retrofit2.Response<Boolean> response)
        {
          // Complete the signOut process.
          if (response.isSuccessful ())
          {
            boolean result = response.body ();

            if (result)
              completeSignOut ();

            settlement.resolve (result);
          }
          else
          {
            settlement.reject (new IllegalStateException ("Failed to signout user"));
          }
        }

        @Override
        public void onFailure (Call<Boolean> call, Throwable t)
        {
          settlement.reject (t);
        }
      });
    });
  }

  /**
   * Get my account information.
   *
   * @return
   */
  public Promise <Resource> getMyAccount ()
  {
    if (!this.isSignedIn ())
      return Promise.reject (new IllegalStateException ("User not signed in"));

    return this.getAccountsEndpoint ().get ("me");
  }

  /**
   * Change the users current password.
   *
   * @param currentPassword
   * @param newPassword
   * @return
   */
  public Promise <Boolean> changePassword (String currentPassword, String newPassword)
  {
    return new Promise<> (settlement -> {
      JsonChangePassword change = new JsonChangePassword ();
      change.currentPassword = currentPassword;
      change.newPassword = newPassword;

      Resource r = new Resource ("change-password", change);

      this.userMethods_.changePassword (r).enqueue (new Callback<Boolean> ()
      {
        @Override
        public void onResponse (Call<Boolean> call, retrofit2.Response<Boolean> response)
        {
          if (response.isSuccessful ())
          {
            settlement.resolve (response.body ());
          }
          else
          {
            settlement.reject (new IllegalStateException (response.message ()));
          }
        }

        @Override
        public void onFailure (Call<Boolean> call, Throwable t)
        {
          settlement.reject (t);
        }
      });
    });
  }

  /**
   * Create a new account.
   *
   * @param username
   * @param password
   * @param email
   */
  public Promise<JsonAccount> createAccount (String username, String password, String email)
  {
    return new Promise<> (settlement -> {
      final Promise.OnResolved<JsonBearerToken, Resource> saveTokenAndCreateAccount = (token, cont) -> {
        this.clientToken_ = ClientToken.fromToken (this.client_.getClientId (), token);
        this.clientToken_.save ();

        // Make a call to create the account.
        JsonAccount account = new JsonAccount ();
        account.username = username;
        account.password = password;
        account.email = email;

        cont.with (this.getCreateAccountEndpoint ().create (account));
      };

      this.client_.getClientToken ()
                  .then (saveTokenAndCreateAccount)
                  .then ((r, cont) -> settlement.resolve (r.get ("account")),
                         (reason, cont) -> settlement.reject (reason));
    });
  }

  /**
   * Create a new account, and login the user.
   *
   * @param username
   * @param password
   * @param email
   * @param autoSignIn
   */
  public Promise <JsonAccount> createAccount (String username, String password, String email, boolean autoSignIn)
  {
    return new Promise<> (settlement -> {
      final Promise.OnResolved<JsonBearerToken, Resource> saveTokenAndCreateAccount = (token, cont) -> {
        this.clientToken_ = ClientToken.fromToken (this.client_.getClientId (), token);
        this.clientToken_.save ();

        // Make a call to create the account.
        JsonAccount account = new JsonAccount ();
        account.username = username;
        account.password = password;
        account.email = email;

        HashMap <String, Object> options = new HashMap<> ();
        options.put ("login", autoSignIn);

        cont.with (this.getCreateAccountEndpoint ().create (account, options));
      };

      final Promise.OnResolved<Resource, Void> completeSignIn = (r, cont) -> {
        JsonAccount account = r.get ("account");
        JsonBearerToken userToken = r.get ("token");

        this.completeSignIn (username, userToken)
            .then ((value, c) -> settlement.resolve (account),
                   (reason, c) -> settlement.reject (reason));
      };

      this.client_.getClientToken ()
                  .then (saveTokenAndCreateAccount)
                  .then (completeSignIn);
    });
  }

  private ResourceEndpoint <JsonAccount> getCreateAccountEndpoint ()
  {
    OkHttpClient clientClient =
        this.client_.getHttpClient ()
                    .newBuilder ()
                    .addInterceptor (clientAuthorizationHeader_)
                    .build ();

    Retrofit clientEndpoint =
        new Retrofit.Builder ()
            .baseUrl (client_.getBaseUrlWithVersion ())
            .addConverterFactory (GsonConverterFactory.create (client_.getGson ()))
            .client (clientClient)
            .build ();

    return ResourceEndpoint.create (clientEndpoint, "account", "accounts");
  }

  private ResourceEndpoint <JsonAccount> getAccountsEndpoint ()
  {
    return ResourceEndpoint.create (this.userEndpoint_, "account", "accounts");
  }

  // The messaging handler for this client. This handlers notifies interested
  // parties when a user is signed in and when a user is signed out.
  private static final int MSG_ON_LOGIN = 0;
  private static final int MSG_ON_LOGOUT = 1;

  private final Handler uiHandler_ = new Handler (Looper.getMainLooper ()) {
    @Override
    public void handleMessage (Message msg)
    {
      switch (msg.what)
      {
        case MSG_ON_LOGIN:
          if (listener_ != null)
            listener_.onSignedIn (GatekeeperSessionClient.this);

          break;

        case MSG_ON_LOGOUT:
          if (listener_ != null)
            listener_.onSignedOut (GatekeeperSessionClient.this);

          break;
      }
    }
  };

  /**
   * Interceptor that adds the user token as the Authorization header to the request.
   */
  private final Interceptor userAuthorizationHeader_ = new Interceptor ()
  {
    @Override
    public Response intercept (Chain chain) throws IOException
    {
      String authorization = "Bearer " + userToken_.accessToken;
      okhttp3.Request original = chain.request ();
      okhttp3.Request.Builder builder = original.newBuilder ();

      if (userAgent_ != null)
        builder.header ("User-Agent", userAgent_);

      builder.header ("Authorization", authorization)
             .method (original.method (), original.body ())
             .build ();

      return chain.proceed (builder.build ());
    }
  };

  boolean refreshTokenSync ()
  {
    try
    {
      retrofit2.Response<JsonBearerToken> response =
          this.client_.refreshTokenSync (this.userToken_.refreshToken)
                      .execute ();

      JsonBearerToken token = response.body ();
      this.userToken_.accessToken = token.accessToken;
      this.userToken_.refreshToken = token.refreshToken;
      this.userToken_.save ();

      return true;
    }
    catch (IOException e)
    {
      this.logger_.error (e.getLocalizedMessage (), e);
      return false;
    }
  }

  /**
   * Interceptor that add the client token as the Authorization header to the request.
   */
  private final Interceptor clientAuthorizationHeader_ = (chain)-> {
    okhttp3.Request original = chain.request ();
    Request.Builder builder = original.newBuilder ();

    if (userAgent_ != null)
      builder.header ("User-Agent", userAgent_);

    if (clientToken_ != null)
    {
      String authorization = "Bearer " + clientToken_.accessToken;
      builder.header ("Authorization", authorization);
    }

    builder.method (original.method (), original.body ())
           .build ();

    return chain.proceed (builder.build ());
  };

  /**
   * Interceptor that handles special cases for a response, such a refreshing
   * the token or for ending a session.
   */
  private final Interceptor responseInterceptor_ = new Interceptor ()
  {
    @Override
    public Response intercept (Chain chain) throws IOException
    {
      // Proceed with the original request. Check the status code for the response.
      // If the status code is 401, then we need to refresh the token. Otherwise,
      // we return control to the next interceptor.

      Request origRequest = chain.request ();
      Response origResponse = chain.proceed (origRequest);

      if (origResponse.isSuccessful ())
        return origResponse;

      int statusCode = origResponse.code ();

      if (statusCode == 401) {
        // Let's try to update the original token. If the response is not successful,
        // the return the original response. Otherwise, retry the same request.
        if (refreshTokenSync ())
          return chain.proceed (origRequest);
      }
      else if (statusCode == 403) {
        // Let's see what kind of error message we received. We may be able to handle
        // it here in the interceptor if it related to the token.
        Resource resource = resourceConverter_.convert (origResponse.body ());
        HttpError error = resource.get ("errors");

        // Since we can only consume a ResponseBody once, we need to replace the original
        // one with a new one.
        String origBody = client_.getGson ().toJson (resource);
        ResponseBody responseBody = ResponseBody.create (origResponse.body ().contentType (), origBody);

        origResponse =
            origResponse.newBuilder ()
                        .body (responseBody)
                        .build();

        if (REAUTHENTICATE_ERROR_CODES.contains (error.getCode ()))
        {
          // Notify the client to authenticate. This is optional. If the client
          // does not authenticate, then all calls will continue to fail.
          if (listener_ != null)
            listener_.onReauthenticate (GatekeeperSessionClient.this, error);
        }
      }

      return origResponse;
    }
  };

  interface UserMethods
  {
    @POST("oauth2/logout")
    Call <Boolean> logout ();

    @POST("accounts/me/password")
    Call <Boolean> changePassword (@Body Resource r);
  }

  static
  {
    GsonResourceManager.getInstance ().registerType ("account", new TypeToken<JsonAccount> () {}.getType ());
    GsonResourceManager.getInstance ().registerType ("accounts", new TypeToken<List<JsonAccount>> () {}.getType ());
    GsonResourceManager.getInstance ().registerType ("change-password", new TypeToken<JsonChangePassword> () {}.getType ());
    GsonResourceManager.getInstance ().registerType ("token", new TypeToken<JsonBearerToken> () {}.getType ());
    GsonResourceManager.getInstance ().registerType ("errors", new TypeToken<HttpError> () {}.getType ());

    REAUTHENTICATE_ERROR_CODES.add ("unknown_token");
    REAUTHENTICATE_ERROR_CODES.add ("invalid_token");
    REAUTHENTICATE_ERROR_CODES.add ("token_disabled");
    REAUTHENTICATE_ERROR_CODES.add ("unknown_client");
    REAUTHENTICATE_ERROR_CODES.add ("client_disabled");
    REAUTHENTICATE_ERROR_CODES.add ("unknown_account");
    REAUTHENTICATE_ERROR_CODES.add ("account_disabled");
  }
}
