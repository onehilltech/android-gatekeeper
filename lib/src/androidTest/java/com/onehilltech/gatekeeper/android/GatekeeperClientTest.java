package com.onehilltech.gatekeeper.android;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.onehilltech.backbone.http.Resource;
import com.onehilltech.gatekeeper.android.http.JsonAccount;
import com.onehilltech.gatekeeper.android.http.JsonBearerToken;
import com.raizlabs.android.dbflow.config.FlowManager;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


@RunWith (AndroidJUnit4.class)
public class GatekeeperClientTest extends TestWithDatabase
{
  private static final String STRING_USERNAME = "tester1";
  private static final String STRING_PASSWORD = "tester1";
  private static final String STRING_EMAIL = "tester@gatekeeper.com";

  private GatekeeperClient gatekeeper_;

  private MockWebServer server_;

  private OkHttpClient httpClient_;

  private boolean completed_;

  private final Object syncObject_ = new Object ();

  @Before
  public void setup ()
      throws Exception
  {
    // Pass control to the base class.
    super.setup ();

    // Get the target context for the test case.
    Context targetContext = InstrumentationRegistry.getTargetContext ();

    this.httpClient_ = new OkHttpClient.Builder ().build ();
    this.server_ = new MockWebServer ();

    HttpUrl serverUrl = this.server_.url ("/");

    GatekeeperClient.Configuration config =
        GatekeeperClient.Configuration
            .loadFromMetadata (targetContext);

    config.baseUri = serverUrl.uri ().toString ();

    // Prepare data needs to initialize the client.
    this.gatekeeper_ =
        new GatekeeperClient.Builder (targetContext)
            .setClient (this.httpClient_)
            .setConfiguration (config)
            .build ();

    this.completed_ = false;
  }

  @After
  public void teardown ()
      throws Exception
  {
    FlowManager.destroy ();
  }

  @Test
  public void testBuilder ()
      throws Exception
  {
    GatekeeperClient.Configuration config =
        GatekeeperClient.Configuration
            .loadFromMetadata (InstrumentationRegistry.getTargetContext ());

    Context targetContext = InstrumentationRegistry.getTargetContext ();

    GatekeeperClient client =
        new GatekeeperClient.Builder (targetContext)
            .build ();

    Assert.assertSame (this.httpClient_, this.gatekeeper_.getHttpClient ());
    Assert.assertEquals (config.baseUri, client.getBaseUrl ());
    Assert.assertEquals (config.clientId, client.getClientId ());
  }

  @Test
  public void testGetUserToken ()
      throws Exception
  {
    JsonBearerToken newToken = JsonBearerToken.generateRandomToken ();

    this.server_.enqueue (
        new MockResponse ()
            .setResponseCode (200)
            .setHeader ("Content-Type", "application/json")
            .setBody (newToken.toString ()));

    Response <JsonBearerToken> response =
        this.gatekeeper_.getUserToken (STRING_USERNAME, STRING_PASSWORD)
                        .execute ();

    Assert.assertEquals (200, response.code ());
    JsonBearerToken token = response.body ();

    Assert.assertEquals (newToken, token);
  }

  @Test
  public void testGetClientToken ()
      throws Exception
  {
    JsonBearerToken newToken = JsonBearerToken.generateRandomToken ();

    this.server_.enqueue (
        new MockResponse ()
            .setResponseCode (200)
            .setHeader ("Content-Type", "application/json")
            .setBody (newToken.toString ()));

    Response <JsonBearerToken> response =
        this.gatekeeper_.getClientToken ()
                        .execute ();

    Assert.assertEquals (200, response.code ());
    JsonBearerToken token = response.body ();

    Assert.assertEquals (newToken, token);
  }

  @Test
  public void testRefreshToken () throws Exception
  {
    JsonBearerToken newToken = JsonBearerToken.generateRandomToken ();

    this.server_.enqueue (
        new MockResponse ()
            .setResponseCode (200)
            .setHeader ("Content-Type", "application/json")
            .setBody (newToken.toString ()));

    JsonBearerToken currToken = JsonBearerToken.generateRandomToken ();
    Response <JsonBearerToken> response =
        this.gatekeeper_.refreshToken (currToken.refreshToken)
                        .execute ();

    Assert.assertEquals (newToken, response.body ());
  }

  @Test
  public void testCreateAccount ()
      throws Exception
  {
    final JsonAccount mockAccount = new JsonAccount ();
    mockAccount._id = "my_id";
    mockAccount.username = STRING_USERNAME;
    mockAccount.password = STRING_PASSWORD;
    mockAccount.email = STRING_EMAIL;

    Resource mockResource = new Resource ("account", mockAccount);

    this.server_.enqueue (
        new MockResponse ()
            .setResponseCode (200)
            .setHeader ("Content-Type", "application/json")
            .setBody (this.gatekeeper_.getGson ().toJson (mockResource)));

    synchronized (this.syncObject_)
    {
      this.gatekeeper_.createAccount (STRING_USERNAME, STRING_PASSWORD, STRING_EMAIL, new Callback<Resource> ()
      {
        @Override
        public void onResponse (Call<Resource> call, Response<Resource> response)
        {
          Resource rc = response.body ();
          JsonAccount account = rc.get ("account");

          Assert.assertEquals (mockAccount._id, account._id);
          Assert.assertEquals (mockAccount.username, account.username);
          Assert.assertEquals (mockAccount.password, account.password);
          Assert.assertEquals (mockAccount.email, account.email);

          synchronized (syncObject_)
          {
            // Mark the test as completed.
            completed_ = true;

            // Notify the waiting thread.
            syncObject_.notify ();
          }
        }

        @Override
        public void onFailure (Call<Resource> call, Throwable t)
        {
          Assert.fail (t.getLocalizedMessage ());

          synchronized (syncObject_)
          {
            syncObject_.notify ();
          }
        }
      });

      this.syncObject_.wait (5000);
    }

    Assert.assertTrue (this.completed_);
  }
}
