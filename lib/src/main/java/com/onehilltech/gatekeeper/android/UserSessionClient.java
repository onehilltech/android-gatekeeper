package com.onehilltech.gatekeeper.android;

import com.onehilltech.gatekeeper.android.model.UserToken;

/**
 * Base class for all session client objects.
 */
public abstract class UserSessionClient
{
  protected final GatekeeperClient client_;

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
   * Login the user.
   *
   * @param username
   * @param password
   * @param listener
   */
  public void loginUser (String username, String password, final ResponseListener <UserToken> listener)
  {
    this.client_.getUserToken (username, password, listener);
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
