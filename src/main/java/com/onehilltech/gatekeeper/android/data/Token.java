package com.onehilltech.gatekeeper.android.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.structure.BaseModel;

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
public abstract class Token extends BaseModel
{
  /// FastXML object mapper reading/writing Json.
  private static final ObjectMapper objMapper = new ObjectMapper();

  @Column
  @JsonIgnore
  public Date expiration;

  public Token ()
  {

  }

  public Token (Date expiration)
  {
    this.expiration = expiration;
  }

  /**
   * Test if the token can expire.
   *
   * @return
   */
  public boolean canExpire ()
  {
    return this.expiration != null;
  }

  /**
   * Convert a JSON string into a Token.
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
    return System.currentTimeMillis () > this.expiration.getTime ();
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
    return this.expiration.equals (token.expiration);
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
