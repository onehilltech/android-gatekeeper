package com.onehilltech.gatekeeper.android;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Grant for requesting an access token using client credentials.
 */
public class ClientCredentials extends Grant
{
  @JsonProperty("client_secret")
  public String clientSecret;
}
