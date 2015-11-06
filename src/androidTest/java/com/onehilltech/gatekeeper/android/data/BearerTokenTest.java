package com.onehilltech.gatekeeper.android.data;

import android.support.test.runner.AndroidJUnit4;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith (AndroidJUnit4.class)
public class BearerTokenTest
{
  @Test
  public void testConstructor ()
  {
    BearerToken token = new BearerToken ("access_token", "refresh_token", 1);

    Assert.assertEquals ("access_token", token.accessToken);
    Assert.assertEquals ("refresh_token", token.refreshToken);
  }

  @Test
  public void testCanRefresh ()
  {
    BearerToken token = new BearerToken ("access_token", "refresh_token", 1);
    Assert.assertTrue (token.getCanRefresh ());

    token = new BearerToken ("access_token", null, 1);
    Assert.assertFalse (token.getCanRefresh ());
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
    String tag = "id";
    BearerToken fakeToken = BearerToken.generateRandomToken ();
    fakeToken.tag = tag;

    String jsonString = fakeToken.toJsonString ();

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
    bearerToken.tag = tag;

    Assert.assertEquals (fakeToken, bearerToken);
  }
}
