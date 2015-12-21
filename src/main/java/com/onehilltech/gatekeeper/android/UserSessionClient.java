package com.onehilltech.gatekeeper.android;

import com.onehilltech.gatekeeper.android.model.UserToken;

/**
 * Created by hilljh on 12/1/15.
 */
public abstract class UserSessionClient
{
  protected final GatekeeperClient client_;

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
  public JsonRequest loginUser (String username, String password, final ResponseListener <UserToken> listener)
  {
    return this.client_.getUserToken (username, password, listener);
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
