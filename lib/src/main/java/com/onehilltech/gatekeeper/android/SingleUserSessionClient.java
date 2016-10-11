package com.onehilltech.gatekeeper.android;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.onehilltech.gatekeeper.android.http.JsonBearerToken;
import com.onehilltech.gatekeeper.android.model.AccessToken;
import com.onehilltech.gatekeeper.android.model.UserToken;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.structure.database.transaction.QueryTransaction;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SingleUserSessionClient extends UserSessionClient
{
  private UserToken userToken_;

  private OkHttpClient httpClient_;

  private Retrofit retrofit_;

  /**
   * Interceptor that adds the Authorization header to a request.
   */
  private final Interceptor authHeaderInterceptor_ = new Interceptor ()
  {
    @Override
    public Response intercept (Chain chain) throws IOException
    {
      String authorization = "Bearer " + getToken ().getAccessToken ();

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

  public interface OnInitializedListener
  {
    void onInitialized (SingleUserSessionClient sessionClient);

    void onInitializeFailed (Throwable t);
  }

  private static class OnInitialized implements GatekeeperClient.OnInitializedListener
  {
    private OnInitializedListener onInitializedListener_;

    public OnInitialized (OnInitializedListener onInitializedListener)
    {
      this.onInitializedListener_ = onInitializedListener;
    }

    @Override
    public void onInitialized (final GatekeeperClient client)
    {
      SQLite.select ()
            .from (UserToken.class)
            .async ()
            .querySingleResultCallback (new QueryTransaction.QueryResultSingleCallback<UserToken> ()
            {
              @Override
              public void onSingleQueryResult (QueryTransaction transaction, @Nullable UserToken userToken)
              {
                SingleUserSessionClient sessionClient = new SingleUserSessionClient (client, userToken);
                onInitializedListener_.onInitialized (sessionClient);
              }
            }).execute ();
    }

    @Override
    public void onInitializeFailed (Throwable e)
    {
      onInitializedListener_.onInitializeFailed (e);
    }
  }

  public static void initialize (Configuration config,
                                 OkHttpClient httpClient,
                                 OnInitializedListener onInitializedListener)
  {
    GatekeeperClient.initialize (config, httpClient, new OnInitialized (onInitializedListener));
  }

  /**
   * Initialize the single user session client.
   *
   * @param context
   * @param httpClient
   * @param onInitializedListener
   */
  public static void initialize (Context context,
                                 OkHttpClient httpClient,
                                 @NonNull final OnInitializedListener onInitializedListener)
  {
    GatekeeperClient.initialize (context, httpClient, new OnInitialized (onInitializedListener));
  }

  /**
   * Initializing constructor.
   *
   * @param client
   */
  private SingleUserSessionClient (GatekeeperClient client, UserToken userToken)
  {
    super (client);

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

    this.userToken_ = userToken;
    this.service_ = this.retrofit_.create (UserSessionClient.Service.class);
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
   * Log out the current user.
   */
  public void logout (final Callback<Boolean> callback)
  {
    if (this.userToken_ == null)
      throw new IllegalStateException ("User is already logged out");

    this.service_.logout ().enqueue (new Callback<Boolean> ()
    {
      @Override
      public void onResponse (Call<Boolean> call, retrofit2.Response<Boolean> response)
      {
        // Complete the logout process.
        if (response.body ())
          completeLogout ();

        // Pass control to the caller.
        callback.onResponse (call, response);
      }

      @Override
      public void onFailure (Call<Boolean> call, Throwable t)
      {
        callback.onFailure (call, t);
      }
    });
  }

  /**
   * Complete the logout process.
   */
  private void completeLogout ()
  {
    this.userToken_.delete ();
    this.userToken_ = null;
  }

  public UserToken getUserToken ()
  {
    return this.userToken_;
  }

  public void updateUserToken (JsonBearerToken token)
  {
    this.userToken_.accessToken = token.accessToken;
    this.userToken_.refreshToken = token.refreshToken;

    this.userToken_.save ();
  }

  private AccessToken getToken ()
  {
    return this.userToken_ != null ? this.userToken_ : this.client_.getClientToken ();
  }

  /**
   * Test if the client is logged in.
   *
   * @return
   */
  public boolean isLoggedIn ()
  {
    return this.userToken_ != null;
  }
}
