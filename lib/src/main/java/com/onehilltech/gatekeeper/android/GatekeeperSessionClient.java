package com.onehilltech.gatekeeper.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.onehilltech.backbone.data.HttpError;
import com.onehilltech.backbone.data.Resource;
import com.onehilltech.backbone.data.ResourceEndpoint;
import com.onehilltech.gatekeeper.android.http.JsonAccount;
import com.onehilltech.gatekeeper.android.http.JsonBearerToken;
import com.onehilltech.gatekeeper.android.http.JsonChangePassword;
import com.onehilltech.gatekeeper.android.model.Account;
import com.onehilltech.gatekeeper.android.model.ClientToken;
import com.onehilltech.gatekeeper.android.model.GatekeeperStore;
import com.onehilltech.gatekeeper.android.model.UserToken;
import com.onehilltech.gatekeeper.android.model.UserToken$Table;
import com.onehilltech.promises.Promise;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.runtime.FlowContentObserver;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.structure.BaseModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

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

import static com.onehilltech.promises.Promise.rejected;
import static com.onehilltech.promises.Promise.resolved;

/**
 * Gatekeeper client bound to a user session.
 */
public class GatekeeperSessionClient
{
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

  public static GatekeeperSessionClient getInstance (Context context)
  {
    if (instance_ != null)
      return instance_;

    instance_ = new GatekeeperSessionClient (context);
    return instance_;
  }

  private static GatekeeperSessionClient instance_;

  private OkHttpClient httpClient_;

  /// The user token for the current session.
  private UserToken userToken_;

  /// The client token for the session client.
  private ClientToken clientToken_;

  private Retrofit retrofit_;

  private final LinkedList <Listener> listeners_ = new LinkedList<> ();

  private final FlowContentObserver userTokenObserver_ = new FlowContentObserver ();

  private final GatekeeperClient client_;

  private final GatekeeperSession session_;

  private final UserMethods userMethods_;

  private String userAgent_;

  private OkHttpClient userClient_;

  private Retrofit userEndpoint_;

  private final Logger logger_ = LoggerFactory.getLogger (GatekeeperSessionClient.class);

  private final Converter<ResponseBody, Resource> resourceConverter_;

  private static final ArrayList <String> REAUTHENTICATE_ERROR_CODES = new ArrayList<> ();

  private final Context context_;

  /**
   * Initializing constructor.
   *
   * @param context         Target context
   */
  private GatekeeperSessionClient (Context context)
  {
    this.context_ = context.getApplicationContext ();
    this.client_ = new GatekeeperClient.Builder (context).build ();
    this.session_ = GatekeeperSession.getCurrent (context);

    // Build a new HttpClient for the user session. This client is responsible for
    // adding the authentication header to each request.
    OkHttpClient.Builder builder = this.client_.getHttpClient ().newBuilder ();
    builder.addInterceptor (this.responseInterceptor_);

    this.httpClient_ = builder.build ();

    // Build the Retrofit object for this client.
    this.retrofit_ = new Retrofit.Builder ()
        .baseUrl (this.client_.getBaseUrlWithVersion ())
        .addConverterFactory (GsonConverterFactory.create (this.client_.getGson ()))
        .client (this.httpClient_)
        .build ();

    this.resourceConverter_ = this.retrofit_.responseBodyConverter (Resource.class, new Annotation[0]);
    this.userMethods_ = this.retrofit_.create (UserMethods.class);

    this.initUserToken (context);

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

  private void initUserToken (Context context)
  {
    String username = GatekeeperSession.getCurrent (context).getUsername ();

    if (username != null)
      this.userToken_ =
          SQLite.select ()
                .from (UserToken.class)
                .where (UserToken$Table.username.eq (username))
                .querySingle ();

    // Load the one and only user token from the database. We also want to
    // observe the user token table for changes. These changes could be logging
    // out or refreshing the user token.
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
  }

  public GatekeeperClient getClient ()
  {
    return this.client_;
  }

  public GatekeeperSession getSession ()
  {
    return this.session_;
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
   * Set the listener object
   *
   * @param listener      Listener object
   */
  public void addListener (Listener listener)
  {
    this.listeners_.add (listener);
  }

  public void removeListener (Listener listener)
  {
    this.listeners_.remove (listener);
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
    return this.ensureSignedIn (activity, new Intent (activity, signIn));
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
    // Force the user to sign out.
    this.completeSignOut ();

    signInIntent.putExtra (GatekeeperSignInActivity.ARG_REDIRECT_INTENT, activity.getIntent ());
    activity.startActivity (signInIntent);

    // Finish the current activity.
    activity.finish ();
  }

  /**
   * Cleanup the object. This is to be called in the onDestroy() method of the
   * Context (e.g., Activity, Fragment, or Service) that created it.
   */
  public void onDestroy (Context context)
  {
    this.userTokenObserver_.unregisterForContentChanges (context);
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

    return new Promise<> (settlement ->
      this.client_.refreshToken (this.userToken_.refreshToken)
                  .then (resolved (token -> {
                    // The token was refreshed. Let's save the token, and then resolve this
                    // promise so the client can continue on.
                    this.userToken_.accessToken = token.accessToken;
                    this.userToken_.refreshToken = token.refreshToken;

                    // Update the user token in the database.
                    FlowManager.getModelAdapter (UserToken.class).update (this.userToken_);

                    settlement.resolve (null);
                  }))
                  ._catch (rejected (settlement::reject))
    );
  }

  /**
   * Complete the signOut process.
   */
  private void completeSignOut ()
  {
    if (this.userToken_ == null)
      return;

    // Delete the current session information.
    this.session_.edit ().delete ();

    // Delete the token from the database. This will cause all session clients
    // listening for changes to be notified of the change.
    FlowManager.getModelAdapter (UserToken.class).delete (this.userToken_);
    GatekeeperStore.getInstance (this.context_).clearCache ();

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

    return new Promise<> (settlement ->
      this.client_
          .getUserToken (username, password)
          .then (token -> this.completeSignIn (username, token))
          .then (resolved (value -> settlement.resolve (null)))
          ._catch (rejected (settlement::reject))
    );
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
      // Save the user access token. We need it so we can
      this.userToken_ = UserToken.fromToken (username, jsonToken);

      FlowManager.getModelAdapter (UserToken.class).save (this.userToken_);

      GatekeeperStore.getInstance (this.context_)
                     .get (Account.class, "me")
                     .then (resolved (account -> {
                       this.session_.edit ()
                                    .setUsername (account.username)
                                    .setUserId (account._id.toString ())
                                    .commit ();


                       settlement.resolve (null);
                     }))
                     ._catch (rejected (reason -> {
                       // Delete the user token that we temporarily saved.
                       this.userToken_ = null;
                       FlowManager.getModelAdapter (UserToken.class).delete (this.userToken_);

                       settlement.reject (reason);
                     }));
    });
  }

  public Promise <Boolean> signOut ()
  {
    return this.signOut (true);
  }

  /**
   * Sign out the current user
   */
  public Promise <Boolean> signOut (boolean forceSignOut)
  {
    if (this.userToken_ == null)
      return Promise.resolve (true);

    return new Promise<> (settlement -> {
      this.logger_.info ("Signing out current user");

      this.userMethods_.logout ().enqueue (new Callback<Boolean> ()
      {
        @Override
        public void onResponse (Call<Boolean> call, retrofit2.Response<Boolean> response)
        {
          if (response.isSuccessful () || forceSignOut)
          {
            Boolean result = response.body ();
            boolean complete = (result != null && result) || forceSignOut;

            if (complete)
              completeSignOut ();

            settlement.resolve (complete);
          }
          else
          {
            settlement.reject (new IllegalStateException ("Failed to sign out user"));
          }
        }

        @Override
        public void onFailure (Call<Boolean> call, Throwable t)
        {
          if (forceSignOut)
          {
            completeSignOut ();
            settlement.resolve (true);
          }
          else
          {
            settlement.reject (t);
          }
        }
      });
    });
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
    return new Promise<> (settlement ->
      this.client_.getClientToken ()
                  .then (token -> {
                    this.clientToken_ = ClientToken.fromToken (this.client_.getClientId (), token);
                    FlowManager.getModelAdapter (ClientToken.class).save (this.clientToken_);

                    // Make a call to create the account.
                    JsonAccount account = new JsonAccount ();
                    account.username = username;
                    account.password = password;
                    account.email = email;

                    return this.getCreateAccountEndpoint ().create (account);
                  })
                  .then (resolved (r -> settlement.resolve (r.get ("account"))))
                  ._catch (rejected (settlement::reject))
    );
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
    return new Promise<> (settlement ->
      this.client_.getClientToken ()
                  .then (token -> {
                    // Use the client token to create a new account. We are going to login
                    // with the newly created account.
                    this.clientToken_ = ClientToken.fromToken (this.client_.getClientId (), token);
                    FlowManager.getModelAdapter (ClientToken.class).save (this.clientToken_);

                    // Make a call to create the account.
                    JsonAccount account = new JsonAccount ();
                    account.username = username;
                    account.password = password;
                    account.email = email;

                    HashMap <String, Object> options = new HashMap<> ();
                    options.put ("login", autoSignIn);

                    return this.getCreateAccountEndpoint ().create (account, options);
                  })
                  .then (resolved (r -> {
                    // Complete the sign in process.
                    JsonAccount account = r.get ("account");
                    JsonBearerToken userToken = r.get ("token");

                    this.completeSignIn (username, userToken)
                        .then (resolved (value -> settlement.resolve (account)))
                        ._catch (rejected (settlement::reject));
                  }))
                  ._catch (rejected (settlement::reject))
    );
  }

  private ResourceEndpoint<JsonAccount> getCreateAccountEndpoint ()
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

  // The messaging handler for this client. This handlers notifies interested
  // parties when a user is signed in and when a user is signed out.
  private static final int MSG_ON_LOGIN = 0;
  private static final int MSG_ON_LOGOUT = 1;
  private static final int MSG_ON_REAUTHENTICATE = 2;

  private final Handler uiHandler_ = new Handler (Looper.getMainLooper ()) {
    @Override
    public void handleMessage (Message msg)
    {
      switch (msg.what)
      {
        case MSG_ON_LOGIN:
          for (Listener listener: listeners_)
            listener.onSignedIn (GatekeeperSessionClient.this);
          break;

        case MSG_ON_LOGOUT:
          for (Listener listener: listeners_)
            listener.onSignedOut (GatekeeperSessionClient.this);
          break;

        case MSG_ON_REAUTHENTICATE:
          HttpError httpError = (HttpError)msg.obj;

          for (Listener listener: listeners_)
            listener.onReauthenticate (GatekeeperSessionClient.this, httpError);
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
      okhttp3.Request original = chain.request ();
      okhttp3.Request.Builder builder = original.newBuilder ();

      if (userToken_ != null)
        builder.header ("Authorization", "Bearer " + userToken_.accessToken);

      if (userAgent_ != null)
        builder.header ("User-Agent", userAgent_);

      builder.method (original.method (), original.body ());

      return chain.proceed (builder.build ());
    }
  };

  private boolean refreshTokenSync ()
  {
    try
    {
      retrofit2.Response<JsonBearerToken> response =
          this.client_.refreshTokenSync (this.userToken_.refreshToken)
                      .execute ();

      if (response.isSuccessful ())
      {
        JsonBearerToken token = response.body ();

        this.userToken_.accessToken = token.accessToken;
        this.userToken_.refreshToken = token.refreshToken;
        FlowManager.getModelAdapter (UserToken.class).save (this.userToken_);

        return true;
      }
      else
      {
        return false;
      }
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
      builder.header ("Authorization", "Bearer " + clientToken_.accessToken);

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
          Message msg = uiHandler_.obtainMessage (MSG_ON_REAUTHENTICATE, error);
          msg.sendToTarget ();
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
    REAUTHENTICATE_ERROR_CODES.add ("unknown_token");
    REAUTHENTICATE_ERROR_CODES.add ("invalid_token");
    REAUTHENTICATE_ERROR_CODES.add ("token_disabled");
    REAUTHENTICATE_ERROR_CODES.add ("unknown_client");
    REAUTHENTICATE_ERROR_CODES.add ("client_disabled");
    REAUTHENTICATE_ERROR_CODES.add ("unknown_account");
    REAUTHENTICATE_ERROR_CODES.add ("account_disabled");
  }
}
