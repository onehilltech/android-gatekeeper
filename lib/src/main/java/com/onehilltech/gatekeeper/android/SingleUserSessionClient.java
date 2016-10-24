package com.onehilltech.gatekeeper.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.onehilltech.gatekeeper.android.http.JsonBearerToken;
import com.onehilltech.gatekeeper.android.http.jsonapi.Resource;
import com.onehilltech.gatekeeper.android.model.UserToken;
import com.onehilltech.gatekeeper.android.model.UserToken_Table;
import com.raizlabs.android.dbflow.runtime.FlowContentObserver;
import com.raizlabs.android.dbflow.sql.language.SQLCondition;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.structure.BaseModel;
import com.raizlabs.android.dbflow.structure.Model;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SingleUserSessionClient extends UserSessionClient
  implements FlowContentObserver.OnModelStateChangedListener
{
  public static final class Builder
  {
    private GatekeeperClient client_;
    private Context context_;

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

    public SingleUserSessionClient build ()
    {
      return new SingleUserSessionClient (this.context_, this.client_);
    }
  }

  public interface Listener
  {
    void onLogin (SingleUserSessionClient client);

    void onLogout (SingleUserSessionClient client);
  }

  private OkHttpClient httpClient_;

  private UserToken userToken_;

  private Retrofit retrofit_;

  private Listener listener_;

  private final FlowContentObserver userTokenObserver_ = new FlowContentObserver ();

  private final Context context_;

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
            listener_.onLogin ((SingleUserSessionClient)msg.obj);
          break;

        case MSG_ON_LOGOUT:
          if (listener_ != null)
            listener_.onLogout ((SingleUserSessionClient)msg.obj);
          break;
      }
    }
  };

  /**
   * Interceptor that adds the Authorization header to a request.
   */
  private final Interceptor authHeaderInterceptor_ = new Interceptor ()
  {
    @Override
    public Response intercept (Chain chain) throws IOException
    {
      String authorization = "Bearer " + userToken_.accessToken;

      okhttp3.Request original = chain.request ();
      okhttp3.Request request =
          original.newBuilder ()
                  .header ("User-Agent", "FundAll Android")
                  .header ("Authorization", authorization)
                  .method (original.method (), original.body ())
                  .build ();

      return chain.proceed (request);
    }
  };

  /**
   * Initializing constructor.
   *
   * @param client
   */
  SingleUserSessionClient (Context context, GatekeeperClient client)
  {
    super (client);

    this.context_ = context;

    // Build a new HttpClient for the user session. This client is responsible for
    // adding the authentication header to each request.
    OkHttpClient.Builder builder = client.getHttpClient ().newBuilder ();
    builder.addInterceptor (this.authHeaderInterceptor_);
    this.httpClient_ = builder.build ();

    // Build the Retrofit object for this client.
    this.retrofit_ = new Retrofit.Builder ()
        .baseUrl (client.getBaseUrlWithVersion ())
        .addConverterFactory (GsonConverterFactory.create (client.getGson ()))
        .client (this.httpClient_)
        .build ();

    this.service_ = this.retrofit_.create (UserSessionClient.Service.class);

    // Load the one and only user token from the database. We also want to
    // observe the user token table for changes. These changes could be logging
    // out or refreshing the user token.
    this.userToken_ = SQLite.select ().from (UserToken.class).querySingle ();

    this.userTokenObserver_.registerForContentChanges (context, UserToken.class);
    this.userTokenObserver_.addModelChangeListener (this);
  }

  /**
   * Ensure the user is logged in. If the user is not logged in, then the login activity
   * will start.
   *
   * @param activity        Parent activity
   * @param login           Login activity class
   * @return                True if logged in; otherwise false
   */
  public static boolean ensureLoggedIn (Activity activity, Class <? extends Activity> login)
  {
    return ensureLoggedIn (activity, new Intent (activity, login));
  }

  /**
   * Ensure the user is logged in. If the user is not logged in, then the login activity
   * will start.
   *
   * @param activity        Parent activity
   * @param loginIntent     Login intent
   * @return                True if logged in; otherwise false
   */
  public static boolean ensureLoggedIn (Activity activity, Intent loginIntent)
  {
    SingleUserSessionClient client = new SingleUserSessionClient.Builder (activity).build ();

    try
    {
      return client.checkLoggedIn (activity, loginIntent);
    }
    finally
    {
      // Make sure we destroy the client to prevent any resource leaks.
      client.onDestroy ();
    }
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
   * Get the listener object
   *
   * @return              A Listener object
   */
  public Listener getListener ()
  {
    return this.listener_;
  }

  /**
   * Check if the client is logged in. If not, then show the login activity so the
   * user can login.
   *
   * @param activity      Parent activity
   * @param login         Login activity class
   * @return              True if logged in; otherwise false
   */
  public boolean checkLoggedIn (Activity activity, Class <? extends Activity> login)
  {
    return this.checkLoggedIn (activity, new Intent (this.context_, login));
  }

  /**
   * Check if the client is logged in. If not, then show the login activity so the
   * user can login.
   *
   * @param activity          Parent activity
   * @param loginIntent       Login activity intent
   * @return
   */
  public boolean checkLoggedIn (Activity activity, Intent loginIntent)
  {
    if (this.isLoggedIn ())
      return true;

    loginIntent.putExtra (SingleUserLoginActivity.ARG_ON_LOGIN_COMPLETE_INTENT, activity.getIntent ());
    this.context_.startActivity (loginIntent);

    return false;
  }

  /**
   * Cleanup the object. This is to be called the onDestroy() method of the
   * parent that created this object.
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
   * Test is client has a user logged in.
   *
   * @return      True if logged; otherwise false
   */
  public boolean isLoggedIn ()
  {
    return this.userToken_ != null;
  }

  void refreshToken (JsonBearerToken token)
  {
    this.userToken_.accessToken = token.accessToken;
    this.userToken_.refreshToken = token.refreshToken;

    // Update the token in the database.
    this.userToken_.update ();
  }

  /**
   * Log out the current user.
   */
  public void logout (Callback<Boolean> callback)
  {
    if (this.userToken_ == null)
      throw new IllegalStateException ("User is already logged out");

    this.service_.logout ().enqueue (new CallbackProxy<Boolean> (callback)
    {
      @Override
      public void onResponse (Call<Boolean> call, retrofit2.Response<Boolean> response)
      {
        // Complete the logout process.
        if (response.isSuccessful () && response.body ())
          completeLogout ();

        super.onResponse (call, response);
      }
    });
  }

  /**
   * Get the user token.
   *
   * @return
   */
  UserToken getUserToken ()
  {
    return this.userToken_;
  }

  /**
   * Complete the logout process.
   */
  private void completeLogout ()
  {
    // Delete the token from the database. This will cause all session clients
    // listening for changes to be notified of the change.
    this.userToken_.delete ();
    this.userToken_ = null;
  }

  /**
   * Login the user.
   *
   * @param username
   * @param password
   * @param callback
   */
  public void login (final String username, String password, Callback<JsonBearerToken> callback)
  {
    this.gatekeeper_.getUserToken (username, password)
                    .enqueue (new CallbackProxy<JsonBearerToken> (callback)
                    {
                      @Override
                      public void onResponse (Call<JsonBearerToken> call, retrofit2.Response<JsonBearerToken> response)
                      {
                        if (response.isSuccessful ())
                          completeLogin (username, response.body ());

                        super.onResponse (call, response);
                      }
                    });
  }

  /**
   * Create a new account.
   *
   * @param username
   * @param password
   * @param email
   * @param callback
   */
  public void createAccount (String username, String password, String email, final Callback <Resource> callback)
  {
    this.gatekeeper_.createAccount (username, password, email, callback);
  }

  /**
   * Complete the login process by storing the information in the database, and
   * notifying all parties that the login is complete.
   *
   * @param username
   * @param jsonToken
   */
  private void completeLogin (String username, JsonBearerToken jsonToken)
  {
    // Save the token to the database. This will notify all session clients that
    // we have created a token, and the user is currently logged in.
    this.userToken_ = UserToken.fromToken (username, jsonToken);
    this.userToken_.insert ();
  }

  @Override
  public void onModelStateChanged (@Nullable Class<? extends Model> table,
                                   BaseModel.Action action,
                                   @NonNull SQLCondition[] primaryKeyValues)
  {
    if (action == BaseModel.Action.DELETE)
    {
      if (this.userToken_ != null)
        this.userToken_ = null;

      Message msg = this.uiHandler_.obtainMessage (MSG_ON_LOGOUT, this);
      this.uiHandler_.dispatchMessage (msg);
    }
    else
    {
      // Load the most recent token from the database.
      this.userToken_ =
          SQLite.select ()
                .from (UserToken.class)
                .where (
                    UserToken_Table.username.eq ((String)primaryKeyValues[0].value ())
                ).querySingle ();

      if (action == BaseModel.Action.INSERT)
      {
        // The token is inserted into the database when the user is logged in. The
        // token is updated in the database when the it is refreshed from the server.

        Message msg = this.uiHandler_.obtainMessage (MSG_ON_LOGIN, this);
        this.uiHandler_.dispatchMessage (msg);
      }
    }
  }
}
