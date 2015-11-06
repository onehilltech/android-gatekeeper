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
import com.onehilltech.gatekeeper.android.data.BearerToken$Table;
import com.onehilltech.gatekeeper.android.data.GatekeeperDatabase;
import com.raizlabs.android.dbflow.sql.builder.Condition;
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
    InstrumentationRegistry.getContext ().deleteDatabase (GatekeeperDatabase.NAME + ".db");

    this.requestQueue_ = MockRequestQueue.newInstance ();
    this.mockNetwork_ = this.requestQueue_.getMockNetwork ();

    // Make sure we start the request queue!
    this.requestQueue_.start ();
  }

  @After
  public void teardown ()
  {
    // Stop the request queue.
    this.requestQueue_.stop ();
  }

  @Test
  public void testInitialize () throws Exception
  {
    final BearerToken fakeToken = BearerToken.generateRandomToken ();
    fakeToken.tag = STRING_CLIENT_ID;

    // Define the mock request handler for the access token.

    this.mockNetwork_.addMatcher (new MockNetwork.RequestMatcher ()
    {
      @Override
      public boolean matches (Request <?> request)
      {
        return request.getOriginUrl ().equals (STRING_BASEURL + "/oauth2/token");
      }

      @Override
      public NetworkResponse getNetworkResponse (Request <?> request) throws VolleyError
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
    });

    // Initialize the Gatekeeper client.

    final GatekeeperClient.Options options = new GatekeeperClient.Options ();
    options.clientId = STRING_CLIENT_ID;
    options.clientSecret = STRING_CLIENT_SECRET;
    options.baseUri = options.baseUriEmulator = STRING_BASEURL;

    this.initializeClient (options);
    Assert.assertNotNull (this.client_);

    // Make sure data is in the database.
    BearerToken bearerToken =
        new Select ().from (BearerToken.class)
                     .where (Condition.column (BearerToken$Table.TAG).eq (STRING_CLIENT_ID),
                             Condition.column (BearerToken$Table.KIND).eq (BearerToken.Kind.ClientToken))
                     .querySingle ();

    Assert.assertEquals (fakeToken, bearerToken);

    // Reset the state of the mock network.
    this.mockNetwork_.reset ();
    this.client_ = null;

    // Test initialization again. There should be no network transmission, and the
    // client should initialize itself again.
    this.initializeClient (options);
    Assert.assertNotNull (this.client_);
  }

  /**
   * Initialize the GatekeeperClient variable.
   *
   * @param options
   * @throws Exception
   */
  private void initializeClient (final GatekeeperClient.Options options)
      throws Exception
  {
    synchronized (this)
    {
      Request <?> request =
          GatekeeperClient.initialize (
              InstrumentationRegistry.getContext (),
            options,
            this.requestQueue_,
            new GatekeeperClient.OnInitialized ()
            {
              @Override
              public void onInitialized (GatekeeperClient client)
              {
                Assert.assertEquals (options.clientId, client.getClientId ());
                Assert.assertEquals (options.baseUri, client.getBaseUri ());

                Assert.assertFalse (client.isLoggedIn ());

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
                  GatekeeperClientTest.this.notify ();
                  Assert.fail (error.getMessage ());
                }
              }
            });

      if (request != null)
        this.wait ();
    }
  }
}
