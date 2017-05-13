package com.onehilltech.gatekeeper.android.http;

import com.google.gson.annotations.SerializedName;

public class JsonChangePassword
{
  @SerializedName ("current")
  public String currentPassword;

  @SerializedName ("new")
  public String newPassword;
}
