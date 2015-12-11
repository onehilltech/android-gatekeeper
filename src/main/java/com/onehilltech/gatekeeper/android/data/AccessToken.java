package com.onehilltech.gatekeeper.android.data;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.structure.BaseModel;

import java.util.Date;

public class AccessToken extends BaseModel
{
  /// Access token for the client.
  @Column(name="access_token")
  String accessToken_;

  /// Expiration for the client type.
  @Column(name="expiration")
  Date expiration_;

  public String getAccessToken ()
  {
    return this.accessToken_;
  }

  public Date getExpiration ()
  {
    return this.expiration_;
  }

  @Override
  public boolean equals (Object obj)
  {
    if (!(obj instanceof AccessToken))
      return false;

    AccessToken token = (AccessToken)obj;
    return token.accessToken_.equals (this.accessToken_);
  }
}
