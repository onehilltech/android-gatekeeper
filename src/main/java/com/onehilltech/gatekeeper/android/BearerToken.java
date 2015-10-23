package com.onehilltech.gatekeeper.android;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.apache.commons.lang3.RandomStringUtils;

import java.util.Date;

/**
 * Implementation of a Token object that implements the Bearer strategy used in
 * Oauth 2.0.
 */
public class BearerToken extends Token
{
  /// The access token.
  private String accessToken_;

  /// The refresh token.
  private String refreshToken_;

  /// How long before the token expires in seconds.
  private long expiresIn_;

  /// The expiration data based on \a expiresIn_.
  private transient Date expirationDate_;

  static char [] VALID_CHARS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', 'A', 'B', 'C', 'D', 'E', 'F'};

  public static final int DEFAULT_EXPIRES_IN = 3600;

  static BearerToken generateRandomToken ()
  {
    return new BearerToken (
        RandomStringUtils.random (48, VALID_CHARS),
        RandomStringUtils.random (48, VALID_CHARS),
        DEFAULT_EXPIRES_IN
    );
  }

  /**
   * Default constructor.
   */
  BearerToken ()
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
    this.accessToken_ = accessToken;
    this.refreshToken_ = refreshToken;
    this.expiresIn_ = expiresIn;

    this.expirationDate_ = new Date (System.currentTimeMillis () + (expiresIn * 1000));
  }

  /**
   * Get the access token.
   *
   * @return
   */
  @JsonProperty ("access_token")
  public String getAccessToken ()
  {
    return this.accessToken_;
  }

  /**
   * Get the refresh token.
   *
   * @return
   */
  @JsonProperty ("refresh_token")
  public String getRefreshToken ()
  {
    return this.refreshToken_;
  }

  /**
   * Get how long in seconds before the token expires. This value is from the time
   * the token was created. If you want to know the expiration date, then use the
   * getExpirationDate () method.
   *
   * @return
   */
  @JsonProperty ("expires_in")
  public long getExpiresIn ()
  {
    return this.expiresIn_;
  }

  /**
   * Test if the access token can be refreshed.
   *
   * @return
   */
  public boolean canRefresh ()
  {
    return this.refreshToken_ != null;
  }

  @Override
  public boolean hasExpired ()
  {
    return System.currentTimeMillis () > this.expirationDate_.getTime ();
  }

  @Override
  public Date getExpirationDate ()
  {
    return this.expirationDate_;
  }

  @Override
  public boolean equals (Object obj)
  {
    if (!(obj instanceof BearerToken))
      return false;

    BearerToken token = (BearerToken)obj;

    return token == this ||
        this.accessToken_.equals (token.accessToken_) &&
            this.refreshToken_.equals (token.refreshToken_) &&
            this.expiresIn_ == token.expiresIn_;
  }

  @Override
  public void accept (TokenVisitor v)
  {
    v.visitBearerToken (this);
  }
}
