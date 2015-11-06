package com.onehilltech.gatekeeper.android.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.annotation.Unique;

import org.apache.commons.lang3.RandomStringUtils;

import java.util.Date;

@Table (databaseName=GatekeeperDatabase.NAME, tableName=BearerToken.NAME)
public class BearerToken extends Token
{
  static final String NAME = "bearer_tokens";

  public enum Kind
  {
    ClientToken,
    UserToken
  }

  @Column
  @PrimaryKey
  @JsonIgnore
  int id;

  @JsonIgnore
  @Column
  @Unique
  public String tag;

  @JsonProperty("access_token")
  @Column(name="access_token")
  public String accessToken;

  @JsonProperty("refresh_token")
  @Column(name="refresh_token")
  public String refreshToken;

  @JsonIgnore
  @Column
  public Kind kind = Kind.UserToken;

  static char [] VALID_CHARS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', 'A', 'B', 'C', 'D', 'E', 'F'};

  public static final int DEFAULT_EXPIRES_IN = 3600;

  public static BearerToken generateRandomToken ()
  {
    return new BearerToken (
        RandomStringUtils.random (48, VALID_CHARS),
        RandomStringUtils.random (48, VALID_CHARS),
        DEFAULT_EXPIRES_IN
    );
  }

  public BearerToken ()
  {

  }

  /**
   * Initializing constructor.
   *
   * @param accessToken
   * @param refreshToken
   * @param expiresIn
   */
  @JsonCreator
  public BearerToken (@JsonProperty("access_token") String accessToken,
                      @JsonProperty("refresh_token") String refreshToken,
                      @JsonProperty("expires_in") int expiresIn)
  {
    super (new Date (System.currentTimeMillis () + (expiresIn * 1000)));

    this.accessToken = accessToken;
    this.refreshToken = refreshToken;
  }

  @Override
  public void accept (TokenVisitor v)
  {
    v.visitBearerToken (this);
  }

  @JsonIgnore
  public boolean getCanRefresh ()
  {
    return this.refreshToken != null;
  }

  @JsonProperty("expires_in")
  public int getExpiresIn ()
  {
    return (int)(this.expiration.getTime () - System.currentTimeMillis ()) / 1000;
  }

  @Override
  public int hashCode ()
  {
    return this.id;
  }

  @Override
  public boolean equals (Object obj)
  {
    if (!(obj instanceof BearerToken))
      return false;

    BearerToken token = (BearerToken)obj;

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
