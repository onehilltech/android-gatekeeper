package com.onehilltech.gatekeeper.android.model;

import com.onehilltech.backbone.data.serializers.ObjectIdSerializer;
import com.onehilltech.backbone.objectid.ObjectId;
import com.onehilltech.gatekeeper.android.http.JsonBearerToken;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;

@Table(database=GatekeeperDatabase.class, name="client_tokens")
public class ClientToken extends AccessToken
{
  /// Client id.
  @PrimaryKey
  @Column(name="client_id", typeConverter = ObjectIdSerializer.class)
  public ObjectId clientId;

  public static ClientToken fromToken (String clientId, JsonBearerToken token)
  {
    return new ClientToken (new ObjectId (clientId), token.accessToken);
  }

  ClientToken ()
  {

  }

  private ClientToken (ObjectId clientId, String accessToken)
  {
    this.clientId = clientId;
    this.accessToken = accessToken;
  }

  @Override
  public int hashCode ()
  {
    return this.clientId.hashCode ();
  }

  @Override
  public boolean equals (Object obj)
  {
    if (!super.equals (obj))
      return false;

    if (!(obj instanceof ClientToken))
      return false;

    ClientToken clientToken = (ClientToken)obj;
    return clientToken.clientId.equals (this.clientId);
  }
}
