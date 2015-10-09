package com.onehilltech.gatekeeper.android;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Assert;
import org.junit.Test;

public class BearerTokenTest
{
  @Test
  public void testConstructor ()
  {
    BearerToken token = new BearerToken ("access_token", "refresh_token", 1);
    long currentTimeMillis = System.currentTimeMillis () + 1000;

    Assert.assertEquals ("access_token", token.getAccessToken ());
    Assert.assertEquals ("refresh_token", token.getRefreshToken ());
    Assert.assertEquals (currentTimeMillis, token.getExpirationDate ().getTime ());
  }

  @Test
  public void testCanRefresh ()
  {
    BearerToken token = new BearerToken ("access_token", "refresh_token", 1);
    Assert.assertTrue (token.canRefresh ());

    token = new BearerToken ("access_token", null, 1);
    Assert.assertFalse (token.canRefresh ());
  }

  @Test
  public void testHasExpired () throws Exception
  {
    BearerToken token = new BearerToken ("access_token", "refresh_token", 1);
    Assert.assertFalse (token.hasExpired ());

    Thread.sleep (2000);

    Assert.assertTrue (token.hasExpired ());
  }

  @Test
  public void testJSON () throws Exception
  {
    BearerToken fakeToken = BearerToken.generateRandomToken ();
    String jsonString = fakeToken.toJSONString ();

    // Test the keys in the json string.
    ObjectMapper objectMapper = new ObjectMapper ();
    JsonNode jsonNode = objectMapper.readTree (jsonString);
    Assert.assertEquals (4, jsonNode.size ());
    Assert.assertTrue (jsonNode.has ("token_type"));
    Assert.assertTrue (jsonNode.has ("access_token"));
    Assert.assertTrue (jsonNode.has ("refresh_token"));
    Assert.assertTrue (jsonNode.has ("expires_in"));

    // Test creating a Token from the json string.
    Token token = Token.fromJSON (jsonString);
    Assert.assertTrue ((token instanceof BearerToken));

    // Make sure the original token was constructed.
    BearerToken bearerToken = (BearerToken)token;
    Assert.assertEquals (fakeToken, bearerToken);
  }
}
