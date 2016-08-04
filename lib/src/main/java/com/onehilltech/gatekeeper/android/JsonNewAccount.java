package com.onehilltech.gatekeeper.android;

import com.fasterxml.jackson.annotation.JsonProperty;

class JsonNewAccount
{
  @JsonProperty("client_id")
  public String clientId;

  public String username;

  public String password;

  public String email;
}
