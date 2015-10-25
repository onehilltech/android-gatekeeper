package com.onehilltech.gatekeeper.android;

import com.android.volley.VolleyError;
import com.onehilltech.gatekeeper.android.BearerToken;
import com.onehilltech.gatekeeper.android.ResponseListener;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.httpclient.FakeHttp;
import org.robolectric.shadows.httpclient.FakeHttpLayer;
import org.robolectric.shadows.httpclient.TestHttpResponse;

import java.io.IOException;
import java.lang.Boolean;
import java.lang.Override;
import java.util.Arrays;
import java.util.Map;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants=BuildConfig.class)
public class GatekeeperClientTest
{
  private GatekeeperClient client_;

  private static final String STRING_HOSTNAME = "gatekeeper.com";
  private static final String STRING_BASEURI = "http://" + STRING_HOSTNAME;
  private static final String STRING_CLIENT_ID = "my_client_id";
  private static final String STRING_CLIENT_SECRET = "my_client_secret";

  private static final String STRING_USERNAME = "tester1";
  private static final String STRING_PASSWORD = "tester1";
  private static final String STRING_EMAIL = "tester@gatekeeper.com";

  @Before
  public void setup ()
  {
    FakeHttp.reset ();

    this.client_ =
        new GatekeeperClient (
            RuntimeEnvironment.application,
            STRING_BASEURI,
            STRING_CLIENT_ID,
            STRING_CLIENT_SECRET);
  }

  @Test
  public void testConstructor ()
  {
    Assert.assertEquals (STRING_BASEURI, this.client_.getBaseUri ());
    Assert.assertEquals (STRING_CLIENT_ID, this.client_.getClientId ());
    Assert.assertEquals (STRING_CLIENT_SECRET, this.client_.getClientSecret ());
    Assert.assertFalse (this.client_.hasToken ());
  }

  @Test
  public void testGetUserToken () throws Exception
  {
    // Setup the fake HTTP responses.

    final BearerToken randomToken = BearerToken.generateRandomToken ();
    FakeHttp.addHttpResponseRule (
        new FakeHttpLayer.RequestMatcherBuilder ()
            .host (STRING_HOSTNAME).path ("oauth2/token")
            .method ("POST")
            .postBody (new EncodedPostBodyMatcher () {
              @Override
              public boolean matches (Map<String, String> postData) throws IOException
              {
                boolean matches =
                    postData.get ("grant_type").equals ("password") &&
                        postData.get ("username").equals (STRING_USERNAME) &&
                        postData.get ("password").equals (STRING_PASSWORD) &&
                        postData.get ("client_id").equals (client_.getClientId ());

                return matches;
              }
            }),
        new TestHttpResponse (200, randomToken.toJSONString ()));

    // Execute the test.
    this.client_.getUserToken (STRING_USERNAME, STRING_PASSWORD, new ResponseListener<BearerToken> () {
        @Override
        public void onErrorResponse (VolleyError error)
        {
          Assert.fail ();
        }

        @Override
        public void onResponse (BearerToken response)
        {
          Assert.assertEquals (randomToken, response);
        }
    });
  }

  @Test
  public void testCreateAccount () throws Exception
  {
    // Setup the fake HTTP responses.

    BearerToken fakeToken = BearerToken.generateRandomToken ();
    FakeHttp.addHttpResponseRule (
        new FakeHttpLayer.RequestMatcherBuilder ()
            .host (STRING_HOSTNAME).path ("oauth2/token")
            .method ("POST")
            .postBody (new EncodedPostBodyMatcher ()
            {
              @Override
              public boolean matches (Map<String, String> postData) throws IOException
              {
                boolean matches =
                    postData.get ("client_id").equals (client_.getClientId ()) &&
                        postData.get ("client_secret").equals (client_.getClientSecret ()) &&
                        postData.get ("grant_type").equals ("client_credentials");

                return matches;
              }
            }),
        new TestHttpResponse (200, fakeToken.toJSONString ()));

    FakeHttp.addHttpResponseRule (
        new FakeHttpLayer.RequestMatcherBuilder ()
            .host (STRING_HOSTNAME).path ("accounts")
            .method ("POST")
            .postBody (new EncodedPostBodyMatcher ()
            {
              @Override
              public boolean matches (Map<String, String> postData) throws IOException
              {
                boolean matches =
                    postData.get ("client_id").equals (client_.getClientId ()) &&
                        postData.get ("username").equals (STRING_USERNAME) &&
                        postData.get ("password").equals (STRING_PASSWORD) &&
                        postData.get ("email").equals (STRING_EMAIL);

                return matches;
              }
            }),
        Arrays.asList (
            new TestHttpResponse (200, "true"),
            new TestHttpResponse (200, "false"))
    );

    // Execute the test.

    this.client_.createAccount (STRING_USERNAME, STRING_PASSWORD, STRING_EMAIL, new ResponseListener<Boolean> () {
      @Override
      public void onErrorResponse (VolleyError error)
      {
        Assert.fail ();
      }

      @Override
      public void onResponse (Boolean response)
      {
        Assert.assertTrue (response);
      }
    });

    this.client_.createAccount (STRING_USERNAME, STRING_PASSWORD, STRING_EMAIL, new ResponseListener<Boolean> () {
      @Override
      public void onErrorResponse (VolleyError error)
      {
        Assert.fail ();
      }

      @Override
      public void onResponse (Boolean response)
      {
        Assert.assertFalse (response);
      }
    });
  }
}
