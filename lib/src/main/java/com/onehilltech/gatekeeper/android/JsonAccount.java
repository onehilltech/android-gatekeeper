package com.onehilltech.gatekeeper.android;

import com.fasterxml.jackson.annotation.JsonProperty;

class JsonAccount
{
  public static class Account
  {
    @JsonProperty
    public String _id;
  }

  @JsonProperty
  public Account account;
}
