package com.onehilltech.gatekeeper.android.http;

import com.google.gson.annotations.SerializedName;

public abstract class JsonGrant
{
  @SerializedName ("client_id")
  public String clientId;

  @SerializedName ("client_secret")
  public String clientSecret;

  @SerializedName ("package")
  public String packageName;
}
