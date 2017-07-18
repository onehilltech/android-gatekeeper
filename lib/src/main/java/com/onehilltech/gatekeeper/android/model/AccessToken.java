package com.onehilltech.gatekeeper.android.model;

import com.raizlabs.android.dbflow.annotation.Column;

public class AccessToken
{
  /// Access token for the client.
  @Column(name="access_token")
  public String accessToken;

  @Override
  public boolean equals (Object obj)
  {
    if (!(obj instanceof AccessToken))
      return false;

    AccessToken token = (AccessToken)obj;
    return token.accessToken.equals (this.accessToken);
  }
}
