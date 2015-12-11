package com.onehilltech.gatekeeper.android;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.android.volley.MockNetwork;
import com.android.volley.MockRequestQueue;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.onehilltech.gatekeeper.android.data.BearerToken;
import com.onehilltech.gatekeeper.android.data.ClientToken;
import com.onehilltech.gatekeeper.android.data.ClientToken_Table;
import com.onehilltech.gatekeeper.android.data.UserToken;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.language.Select;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith (AndroidJUnit4.class)
public class GatekeeperClientTest
{
  private MockRequestQueue requestQueue_;
  private MockNetwork mockNetwork_;

  private GatekeeperClient client_;

  private static final String STRING_BASEURL = "http://localhost:5000";
  private static final String STRING_CLIENT_ID = "my_client_id";
  private static final String STRING_CLIENT_SECRET = "my_client_secret";

  private static final String STRING_USERNAME = "tester1";
  private static final String STRING_PASSWORD = "tester1";
  private static final String STRING_EMAIL = "tester@gatekeeper.com";

  @Before
  public void setup ()
  {
    // Delete the Gatekeeper database.
    InstrumentationRegistry.getContext ().deleteDatabase ("gatekeeper.db");

    this.requestQueue_ = MockRequestQueue.newInstance ();
    this.mockNetwork_ = this.requestQueue_.getMockNetwork ();

    // Make sure we start the request queue!
    this.requestQueue_.start ();

    // Initialize the DBFlow framework.
    FlowManager.init (InstrumentationRegistry.getTargetContext ());
  }

  @After
  public void teardown ()
  {
    // Stop the request queue.
    this.requestQueue_.stop ();

    // Destroy the FlowManager resources.
    FlowManager.destroy ();
  }

  @Test
  public void testInitialize () throws Exception
  {
    Assert.assertNotNull (this.initializeClient (STRING_CLIENT_ID, STRING_CLIENT_SECRET));
    Assert.assertNotNull (this.client_);

    // Make sure data is in the database.
    ClientToken clientToken =
        new Select ().from (ClientToken.class)
                     .where (ClientToken_Table.client_id.eq (STRING_CLIENT_ID))
                     .querySingle ();

    Assert.assertEquals (this.client_.getClientToken (), clientToken);

    // Test initialization again. There should be no network transmission, and the
    // client should initialize itself again.
    this.client_ = null;
    Assert.assertNull (this.initializeClient (STRING_CLIENT_ID, STRING_CLIENT_SECRET));
    Assert.assertNotNull (this.client_);
  }

  @Test
  public void testGetUserToken () throws Exception
  {
    // Initialize the Gatekeeper client, the get a user token. This approach should
    // get a user token from the service.
    Assert.assertNotNull (this.initializeClient (STRING_CLIENT_ID, STRING_CLIENT_SECRET));
    Assert.assertNotNull (this.getUserToken (STRING_USERNAME, STRING_PASSWORD));

    // Test initialization again. Both the client and the user should be initialized
    // without any network communication.
    this.client_ = null;
    Assert.assertNull (this.initializeClient (STRING_CLIENT_ID, STRING_CLIENT_SECRET));
  }

  /**
   * Helper method to get the user token for testing purposes.
   *
   * @param username
   * @param password
   * @return
   */
  private Request getUserToken (final String username, final String password) throws Exception
  {
    // Setup the mocked routes.
    final BearerToken fakeToken = BearerToken.generateRandomToken ();

    final MockNetwork.RequestMatcher requestMatcher = new MockNetwork.RequestMatcher ()
    {
      @Override
      public boolean matches (Request<?> request)
      {
        if (!request.getOriginUrl ().equals (STRING_BASEURL + "/oauth2/token"))
          return false;

        JsonRequest jsonRequest = (JsonRequest) request;
        UserCredentials userCredentials = (UserCredentials) jsonRequest.getData ();

        return
            userCredentials.username.equals (username) &&
                userCredentials.password.equals (password) &&
                userCredentials.clientId.equals (STRING_CLIENT_ID);
      }

      @Override
      public NetworkResponse getNetworkResponse (Request<?> request) throws VolleyError
      {
        try
        {
          return new NetworkResponse (fakeToken.toJsonBytes ());
        }
        catch (JsonProcessingException e)
        {
          throw new VolleyError (e);
        }
      }
    };

    this.mockNetwork_.addMatcher (requestMatcher);

    synchronized (this)
    {
      // Request a user token.
      Request<?> request = this.client_.getUserToken (
          username,
          password,
          new ResponseListener<UserToken> ()
          {
            @Override
            public void onErrorResponse (VolleyError error)
            {
              synchronized (GatekeeperClientTest.this)
              {
                Assert.fail (error.getMessage ());
              }
            }

            @Override
            public void onResponse (UserToken response)
            {
              synchronized (GatekeeperClientTest.this)
              {
                Assert.assertEquals (username, response.getUsername ());
                Assert.assertEquals (fakeToken.accessToken, response.getAccessToken ());
                Assert.assertEquals (fakeToken.refreshToken, response.getRefreshToken ());
                Assert.assertNotNull (response.getExpiration ());

                GatekeeperClientTest.this.notify ();
              }
            }
          });

      if (request != null)
        this.wait ();

      this.mockNetwork_.removeMatcher (requestMatcher);
      return request;
    }
  }

  /**
   * Initialize the GatekeeperClient variable.
   *
   * @throws Exception
   */
  private Request <?> initializeClient (final String clientId, final String clientSecret)
      throws Exception
  {
    final BearerToken fakeToken = BearerToken.generateRandomToken ();

    MockNetwork.RequestMatcher requestMatcher = new MockNetwork.RequestMatcher ()
    {
      @Override
      public boolean matches (Request<?> request)
      {
        if (!request.getOriginUrl ().equals (STRING_BASEURL + "/oauth2/token"))
          return false;

        JsonRequest jsonRequest = (JsonRequest) request;
        ClientCredentials clientCredentials = (ClientCredentials) jsonRequest.getData ();

        return
            clientCredentials.clientId.equals (clientId) &&
                clientCredentials.clientSecret.equals (clientSecret);
      }

      @Override
      public NetworkResponse getNetworkResponse (Request<?> request) throws VolleyError
      {
        try
        {
          return new NetworkResponse (fakeToken.toJsonBytes ());
        }
        catch (JsonProcessingException e)
        {
          throw new VolleyError (e);
        }
      }
    };

    this.mockNetwork_.addMatcher (requestMatcher);

    // Initialize the Gatekeeper client.
    final GatekeeperClient.Options options = new GatekeeperClient.Options ();
    options.clientId = clientId;
    options.clientSecret = clientSecret;
    options.baseUri = options.baseUriEmulator = STRING_BASEURL;

    synchronized (this)
    {
      Request <?> request =
          GatekeeperClient.initialize (
              options,
              this.requestQueue_,
              new GatekeeperClient.OnInitialized ()
              {
                @Override
                public void onInitialized (GatekeeperClient client)
                {
                  Assert.assertEquals (options.clientId, client.getClientId ());
                  Assert.assertEquals (options.baseUri, client.getBaseUri ());

                  // Save the client for the later test.
                  client_ = client;

                  synchronized (GatekeeperClientTest.this)
                  {
                    GatekeeperClientTest.this.notify ();
                  }
                }

                @Override
                public void onError (VolleyError error)
                {
                  synchronized (GatekeeperClientTest.this)
                  {
                    Assert.fail (error.getMessage ());
                  }
                }
              });

      if (request != null)
        this.wait ();

      this.mockNetwork_.removeMatcher (requestMatcher);
      return request;
    }
  }
}
