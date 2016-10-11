package com.onehilltech.gatekeeper.android.model;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.structure.BaseModel;

public class AccessToken extends BaseModel
{
  /// Access token for the client.
  @Column(name="access_token")
  public String accessToken;

  public String getAccessToken ()
  {
    return this.accessToken;
  }

  @Override
  public boolean equals (Object obj)
  {
    if (!(obj instanceof AccessToken))
      return false;

    AccessToken token = (AccessToken)obj;
    return token.accessToken.equals (this.accessToken);
  }
}
