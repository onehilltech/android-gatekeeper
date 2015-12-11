package com.onehilltech.gatekeeper.android.data;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;

import java.util.Date;

@Table(database=GatekeeperDatabase.class, name="client_tokens")
public class ClientToken extends AccessToken
{
  /// Client id.
  @Column(name="client_id")
  @PrimaryKey
  String clientId_;

  public static ClientToken fromToken (String clientId, BearerToken token)
  {
    return new ClientToken (clientId, token.accessToken, token.getExpiration ());
  }

  ClientToken ()
  {

  }

  private ClientToken (String clientId, String accessToken, Date expiration)
  {
    this.clientId_ = clientId;
    this.accessToken_ = accessToken;
    this.expiration_ = expiration;
  }

  public String getClientId ()
  {
    return this.clientId_;
  }

  public String getAccessToken ()
  {
    return this.accessToken_;
  }

  public Date getExpiration ()
  {
    return this.expiration_;
  }

  @Override
  public int hashCode ()
  {
    return this.clientId_.hashCode ();
  }

  @Override
  public boolean equals (Object obj)
  {
    if (!super.equals (obj))
      return false;

    if (!(obj instanceof ClientToken))
      return false;

    ClientToken clientToken = (ClientToken)obj;
    return clientToken.clientId_.equals (this.clientId_);
  }
}
