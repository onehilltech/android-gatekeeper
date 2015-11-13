package com.onehilltech.gatekeeper.android.db;

import com.onehilltech.gatekeeper.android.data.BearerToken;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;

import java.util.Date;

@Table(databaseName= GatekeeperDatabase.NAME, tableName= UserToken.TABLE_NAME)
public class UserToken extends AccessToken
{
  public static final String TABLE_NAME = "user_tokens";

  /// Client id.
  @Column(name="username")
  @PrimaryKey
  String username_;

  /// Access token for the client.
  @Column(name="refresh_token")
  String refreshToken_;

  public static UserToken fromToken (String username, BearerToken token)
  {
    return new UserToken (username, token.accessToken, token.refreshToken, token.getExpiration ());
  }

  UserToken ()
  {

  }

  private UserToken (String username, String accessToken, String refreshToken, Date expiration)
  {
    this.username_ = username;
    this.accessToken_ = accessToken;
    this.refreshToken_ = refreshToken;
    this.expiration_ = expiration;
  }

  public String getUsername ()
  {
    return this.username_;
  }

  public String getRefreshToken ()
  {
    return this.refreshToken_;
  }

  public boolean canRefresh ()
  {
    return this.refreshToken_ != null;
  }

  @Override
  public int hashCode ()
  {
    return this.username_.hashCode ();
  }

  @Override
  public boolean equals (Object obj)
  {
    if (!super.equals (obj))
      return false;

    if (!(obj instanceof UserToken))
      return false;

    UserToken userToken = (UserToken)obj;

    if (!userToken.username_.equals (this.username_))
      return false;

    if (userToken.refreshToken_ == null && this.refreshToken_ != null ||
        userToken.refreshToken_ != null && this.refreshToken_ == null)
    {
      return false;
    }

    return userToken.refreshToken_.equals (this.refreshToken_);
  }
}
