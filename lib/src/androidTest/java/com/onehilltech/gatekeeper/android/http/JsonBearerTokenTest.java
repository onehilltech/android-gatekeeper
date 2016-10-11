package com.onehilltech.gatekeeper.android.http;

import android.support.test.runner.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith (AndroidJUnit4.class)
public class JsonBearerTokenTest
{
  @Test
  public void testConstructor ()
  {
    JsonBearerToken token = new JsonBearerToken ("access_token", "refresh_token");

    Assert.assertEquals ("access_token", token.accessToken);
    Assert.assertEquals ("refresh_token", token.refreshToken);
  }

  @Test
  public void testCanRefresh ()
  {
    JsonBearerToken token = new JsonBearerToken ("access_token", "refresh_token");
    Assert.assertTrue (token.getCanRefresh ());

    token = new JsonBearerToken ("access_token", null);
    Assert.assertFalse (token.getCanRefresh ());
  }
}
