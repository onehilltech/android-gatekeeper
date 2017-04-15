package com.onehilltech.gatekeeper.android.http;

import com.google.gson.annotations.SerializedName;

public abstract class JsonGrant
{
  @SerializedName ("client_id")
  public String clientId;

  @SerializedName ("package")
  public String packageName;
}
