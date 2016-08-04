package com.onehilltech.gatekeeper.android;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JsonGrant kind for refreshing an existing access token.
 */
class JsonRefreshToken extends JsonGrant
{
  @JsonProperty("refresh_token")
  public String refreshToken;
}
