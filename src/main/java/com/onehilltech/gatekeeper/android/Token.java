package com.onehilltech.gatekeeper.android;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
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
  private static final ObjectMapper objMapper = new ObjectMapper();

  /**
   * Test if the token has expired.
   *
   * @return
   */
  public abstract boolean hasExpired ();

  /**
   * Get the expiration date for the token.
   *
   * @return
   */
  public abstract Date getExpirationDate ();

  public abstract void accept (TokenVisitor v);

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
   * Convert the token to a JSON string.
   *
   * @return
   */
  public String toJSONString ()
      throws JsonProcessingException
  {
    return objMapper.writeValueAsString (this);
  }
}
