package com.onehilltech.gatekeeper.android.http;

import com.google.gson.annotations.SerializedName;

public class JsonRefreshToken extends JsonGrant
{
  @SerializedName ("refresh_token")
  public String refreshToken;
}
