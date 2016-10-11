package com.onehilltech.gatekeeper.android.http;

import com.google.gson.annotations.SerializedName;

public class JsonClientCredentials extends JsonGrant
{
  @SerializedName ("client_secret")
  public String clientSecret;
}
