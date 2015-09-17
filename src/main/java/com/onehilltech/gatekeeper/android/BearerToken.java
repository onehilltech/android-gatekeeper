package com.onehilltech.gatekeeper.android;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

/**
 * Created by hillj on 9/7/15.
 */
public class BearerToken implements Token
{
  private final String accessToken_;

  private final String refreshToken_;

  private final Date expirationDate_;

  /**
   * Create a BearerToken from a JSON object.
   *
   * @param obj
   * @return
   * @throws JSONException
   */
  public static BearerToken fromJSONObject (JSONObject obj)
      throws JSONException
  {
    String accessToken = obj.getString ("access_token");
    String refreshToken = null;

    if (obj.has ("refresh_token"))
      refreshToken = obj.getString ("refresh_token");

    int expiresIn = obj.getInt ("expires_in");

    return new BearerToken (accessToken, refreshToken, expiresIn);
  }

  /**
   * Initializing constructor.
   *
   * @param accessToken
   * @param refreshToken
   * @param expiresIn
   */
  public BearerToken (String accessToken, String refreshToken, int expiresIn)
  {
    this.accessToken_ = accessToken;
    this.refreshToken_ = refreshToken;

    this.expirationDate_ = new Date (System.currentTimeMillis () + (expiresIn * 1000));
  }

  /**
   * Get the access token.
   *
   * @return
   */
  public String getAccessToken ()
  {
    return this.accessToken_;
  }

  /**
   * Get the refresh token.
   *
   * @return
   */
  public String getRefreshToken ()
  {
    return this.refreshToken_;
  }

  /**
   * Test if the access token can be refreshed.
   *
   * @return
   */
  public boolean canRefresh ()
  {
    return this.refreshToken_ != null;
  }

  @Override
  public boolean hasExpired ()
  {
    return System.currentTimeMillis () > this.expirationDate_.getTime ();
  }

  @Override
  public Date getExpirationDate ()
  {
    return this.expirationDate_;
  }
}
