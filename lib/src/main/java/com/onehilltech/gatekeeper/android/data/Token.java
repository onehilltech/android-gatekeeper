package com.onehilltech.gatekeeper.android.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Date;

/**
 * Base class for all token types.
 */
@JsonTypeInfo(
    use=JsonTypeInfo.Id.NAME,
    include=JsonTypeInfo.As.PROPERTY,
    property="token_type")
@JsonSubTypes({
    @Type(value=BearerToken.class, name="Bearer")})
@JsonAutoDetect(getterVisibility= JsonAutoDetect.Visibility.NONE)
public abstract class Token
{
  /// FastXML object mapper reading/writing Json.
  private static final ObjectMapper objMapper = new ObjectMapper();

  @JsonIgnore
  private Date expiration_;

  public Token ()
  {

  }

  public Token (Date expiration)
  {
    this.expiration_ = expiration;
  }

  /**
   * Get how long before the token expires in seconds.
   *
   * @return
   */
  @JsonProperty("expires_in")
  public long getExpiresIn ()
  {
    return (this.expiration_.getTime () - System.currentTimeMillis ()) / 1000;
  }

  /**
   * Test if the token can expire. If the token does not have an expiration date, then
   * the token does not expire.
   *
   * @return
   */
  public boolean canExpire ()
  {
    return this.expiration_ != null;
  }

  /**
   * Convert a JSON string into a BaseToken.
   *
   * @param json
   * @return
   */
  public static Token fromJSON (String json)
      throws IOException
  {
    return objMapper.readValue (json, Token.class);
  }

  /**
   * Test if the token has expired.
   *
   * @return True if token has expired; otherwise false
   */
  public boolean hasExpired ()
  {
    return System.currentTimeMillis () > this.expiration_.getTime ();
  }

  /**
   * Get the expiration date for the token.
   *
   * @return
   */
  public Date getExpiration ()
  {
    return this.expiration_;
  }

  /**
   * Accept a TokenVisitor object.
   *
   * @param v   The visitor object
   */
  public abstract void accept (TokenVisitor v);

  @Override
  public boolean equals (Object obj)
  {
    if (!(obj instanceof Token))
      return false;

    Token token = (Token)obj;
    return this.expiration_.equals (token.expiration_);
  }

  /**
   * Convert the token to a JSON string.
   *
   * @return
   */
  public final String toJsonString ()
      throws JsonProcessingException
  {
    return objMapper.writeValueAsString (this);
  }

  /**
   * Get the token as Json bytes. This is useful when sending the token as part of
   * a Json request.
   *
   * @return
   * @throws JsonProcessingException
   */
  public final byte [] toJsonBytes ()
      throws JsonProcessingException
  {
    return objMapper.writeValueAsBytes (this);
  }
}
