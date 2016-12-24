package com.onehilltech.gatekeeper.android.model;

import com.onehilltech.gatekeeper.android.http.JsonBearerToken;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;

@Table(database=GatekeeperDatabase.class, name="client_token")
public class ClientToken extends AccessToken
{
  /// Client id.
  @Column(name="client_id")
  @PrimaryKey
  public String clientId_;

  public static ClientToken fromToken (String clientId, JsonBearerToken token)
  {
    return new ClientToken (clientId, token.accessToken);
  }

  ClientToken ()
  {

  }

  private ClientToken (String clientId, String accessToken)
  {
    this.clientId_ = clientId;
    this.accessToken = accessToken;
  }

  public String getClientId ()
  {
    return this.clientId_;
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
