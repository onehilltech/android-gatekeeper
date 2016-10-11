package com.onehilltech.gatekeeper.android.http;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.RandomStringUtils;

public class JsonBearerToken
{
  @SerializedName ("token_type")
  private final String tokenType = "Bearer";

  @SerializedName ("access_token")
  public String accessToken;

  @SerializedName ("refresh_token")
  public String refreshToken;

  static char [] VALID_CHARS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', 'A', 'B', 'C', 'D', 'E', 'F'};

  public static JsonBearerToken generateRandomToken ()
  {
    String accessToken = RandomStringUtils.random (48, VALID_CHARS);
    String refreshToken = RandomStringUtils.random (48, VALID_CHARS);

    return new JsonBearerToken (accessToken, refreshToken);
  }

  JsonBearerToken (String accessToken, String refreshToken)
  {
    this.accessToken = accessToken;
    this.refreshToken = refreshToken;
  }

  public boolean getCanRefresh ()
  {
    return this.refreshToken != null;
  }

  @Override
  public int hashCode ()
  {
    return this.accessToken.hashCode ();
  }

  @Override
  public boolean equals (Object obj)
  {
    if (!(obj instanceof JsonBearerToken))
      return false;

    JsonBearerToken token = (JsonBearerToken)obj;

    if (!this.accessToken.equals (token.accessToken))
      return false;

    if (this.refreshToken != null)
      return this.refreshToken.equals (token.refreshToken);

    return true;
  }

  @Override
  public String toString ()
  {
    return new Gson ().toJson (this);
  }
}
