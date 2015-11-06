package com.onehilltech.gatekeeper.android;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ClientCredentials extends Grant
{
  @JsonProperty("client_secret")
  public String clientSecret;
}
