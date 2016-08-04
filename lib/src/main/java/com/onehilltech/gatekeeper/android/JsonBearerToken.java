package com.onehilltech.gatekeeper.android;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.apache.commons.lang3.RandomStringUtils;

import java.util.Date;

public class JsonBearerToken extends JsonToken
{
  @JsonIgnore
  int id;

  @JsonIgnore
  public String tag;

  @JsonProperty("access_token")
  public String accessToken;

  @JsonProperty("refresh_token")
  public String refreshToken;

  static char [] VALID_CHARS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', 'A', 'B', 'C', 'D', 'E', 'F'};

  public static final int DEFAULT_EXPIRES_IN = 3600;

  public static JsonBearerToken generateRandomToken ()
  {
    JsonBearerToken token =
        new JsonBearerToken (RandomStringUtils.random (48, VALID_CHARS),
                             RandomStringUtils.random (48, VALID_CHARS),
                             DEFAULT_EXPIRES_IN);

    return token;
  }

  /**
   * Initializing constructor.
   *
   * @param accessToken
   * @param refreshToken
   * @param expiresIn
   */
  @JsonCreator
  public JsonBearerToken (@JsonProperty("access_token") String accessToken,
                          @JsonProperty("refresh_token") String refreshToken,
                          @JsonProperty("expires_in") int expiresIn)
  {
    super (new Date (System.currentTimeMillis () + (expiresIn * 1000)));

    this.accessToken = accessToken;
    this.refreshToken = refreshToken;
  }

  @Override
  public void accept (JsonTokenVisitor v)
  {
    v.visitBearerToken (this);
  }

  @JsonIgnore
  public boolean getCanRefresh ()
  {
    return this.refreshToken != null;
  }

  @Override
  public int hashCode ()
  {
    return this.id;
  }

  @Override
  public boolean equals (Object obj)
  {
    if (!(obj instanceof JsonBearerToken))
      return false;

    JsonBearerToken token = (JsonBearerToken)obj;

    if (!this.tag.equals (token.tag) ||
        !this.accessToken.equals (token.accessToken))
    {
      return false;
    }

    if (this.refreshToken != null)
      return this.refreshToken.equals (token.refreshToken);

    return true;
  }
}
