package com.onehilltech.gatekeeper.android;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Grant kind for refreshing an existing access token.
 */
public class RefreshToken extends Grant
{
  @JsonProperty("refresh_token")
  public String refreshToken;
}
