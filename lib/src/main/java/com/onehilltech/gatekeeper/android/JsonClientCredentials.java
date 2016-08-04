package com.onehilltech.gatekeeper.android;

import com.fasterxml.jackson.annotation.JsonProperty;

class JsonClientCredentials extends JsonGrant
{
  @JsonProperty("client_secret")
  public String clientSecret;
}
