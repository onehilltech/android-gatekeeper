package com.onehilltech.gatekeeper.android.model;

import com.onehilltech.gatekeeper.android.http.JsonBearerToken;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;

import java.util.Arrays;

@Table(database=GatekeeperDatabase.class, name="user_tokens")
public class UserToken extends AccessToken
{
  @PrimaryKey
  public String username;

  @Column(name="refresh_token")
  public String refreshToken;

  /**
   * Create a User token from a token.
   *
   * @param username
   * @param token
   * @return
   */
  public static UserToken fromToken (String username, JsonBearerToken token)
  {
    return new UserToken (username, token.accessToken, token.refreshToken);
  }

  UserToken ()
  {

  }

  private UserToken (String username, String accessToken, String refreshToken)
  {
    this.username = username;
    this.accessToken = accessToken;
    this.refreshToken = refreshToken;
  }

  public boolean canRefresh ()
  {
    return this.refreshToken != null;
  }

  @Override
  public int hashCode ()
  {
    Object [] objs = new Object[] { this.username, this.accessToken, this.refreshToken};
    return Arrays.hashCode (objs);
  }

  @Override
  public boolean equals (Object obj)
  {
    if (!super.equals (obj))
      return false;

    if (!(obj instanceof UserToken))
      return false;

    UserToken userToken = (UserToken)obj;

    if (!userToken.username.equals (this.username))
      return false;

    if (userToken.refreshToken == null && this.refreshToken != null ||
        userToken.refreshToken != null && this.refreshToken == null)
    {
      return false;
    }

    return userToken.refreshToken.equals (this.refreshToken);
  }
}
