package com.onehilltech.gatekeeper.android;

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
}
