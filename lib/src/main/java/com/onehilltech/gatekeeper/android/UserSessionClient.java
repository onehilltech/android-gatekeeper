package com.onehilltech.gatekeeper.android;

import retrofit2.Call;
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
   * Get the underlying client for making the requests.
   *
   * @return
   */
  public GatekeeperClient getGatekeeper ()
  {
    return this.client_;
  }
}
