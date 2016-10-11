package com.onehilltech.gatekeeper.android;

import com.onehilltech.gatekeeper.android.http.JsonBearerToken;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.http.POST;

/**
 * Base class for all session client objects.
 */
public abstract class UserSessionClient
{
  protected interface Service
  {
    @POST("oauth2/logout")
    Call <Boolean> logout ();
  }

  protected final GatekeeperClient client_;

  protected Service service_;

  /**
   * Initializing constructor.
   *
   * @param client
   */
  protected UserSessionClient (GatekeeperClient client)
  {
    this.client_ = client;
  }

  /**
   * Login a user by username/password
   *
   * @param username
   * @param password
   * @param callback
   */
  public void login (String username, String password, Callback <JsonBearerToken> callback)
  {
    this.client_.getUserToken (username, password).enqueue (callback);
  }

  /**
   * Get the underlying client for making the requests.
   *
   * @return
   */
  public GatekeeperClient getClient ()
  {
    return this.client_;
  }
}
