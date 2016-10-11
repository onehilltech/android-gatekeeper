package com.onehilltech.gatekeeper.android;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.google.gson.reflect.TypeToken;
import com.onehilltech.gatekeeper.android.http.JsonAccount;
import com.onehilltech.gatekeeper.android.http.JsonBearerToken;
import com.onehilltech.gatekeeper.android.http.jsonapi.Resource;
import com.onehilltech.gatekeeper.android.model.ClientToken;
import com.onehilltech.gatekeeper.android.model.ClientToken_Table;
import com.raizlabs.android.dbflow.config.FlowConfig;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import retrofit2.Response;


@RunWith (AndroidJUnit4.class)
public class GatekeeperClientTest
  implements GatekeeperClient.OnInitializedListener
{
  private Configuration clientConfig_;
  private GatekeeperClient gatekeeperClient_;

  private static final String STRING_USERNAME = "tester1";
  private static final String STRING_PASSWORD = "tester1";
  private static final String STRING_EMAIL = "tester@gatekeeper.com";

  private MockWebServer server_;

  private HttpUrl serverUrl_;

  private OkHttpClient httpClient_;

  @Before
  public void setup () throws Exception
  {
    // Get the target context for the test case.
    Context targetContext = InstrumentationRegistry.getTargetContext ();

    // Delete the database.
    targetContext.deleteDatabase ("gatekeeper.db");

    // Initialize the DBFlow framework.
    FlowManager.init (
        new FlowConfig.Builder (targetContext)
            .openDatabasesOnInit (true)
            .build ());

    this.httpClient_ = new OkHttpClient.Builder ().build ();

    this.server_ = new MockWebServer ();
    this.serverUrl_ = this.server_.url ("/");

    // Prepare data needs to initialize the client.
    this.clientConfig_ = Configuration.loadFromMetadata (targetContext);
    this.clientConfig_.baseUri = this.serverUrl_.uri ().toString ();

    Resource.registerType ("account", new TypeToken<JsonAccount> () {}.getType ());
    Resource.registerType ("accounts", new TypeToken<List<JsonAccount>> () {}.getType ());
  }

  @After
  public void teardown () throws Exception
  {
    FlowManager.destroy ();
  }

  @Test
  public void testInitialize () throws Exception
  {
    this.initializeClient ();
    Assert.assertNotNull (this.gatekeeperClient_);

    // Make sure data is in the database.
    ClientToken clientToken =
        SQLite.select ()
              .from (ClientToken.class)
              .where (ClientToken_Table.client_id.eq (clientConfig_.clientId))
              .querySingle ();

    Assert.assertEquals (this.gatekeeperClient_.getClientToken (), clientToken);

    // Test initialization again. There should be no network transmission, and the
    // client should initialize itself again.
    this.gatekeeperClient_ = null;
    this.initializeClient ();

    Assert.assertNotNull (this.gatekeeperClient_);
  }

  @Test
  public void testGetUserToken () throws Exception
  {
    // Initialize the Gatekeeper client, the get a user token. This approach should
    // get a user token from the service.
    this.initializeClient ();
    this.getUserToken (STRING_USERNAME, STRING_PASSWORD);
  }

  @Test
  public void testRefreshToken () throws Exception
  {
    this.initializeClient ();

    JsonBearerToken newToken = JsonBearerToken.generateRandomToken ();
    this.server_.enqueue (
        new MockResponse ()
            .setResponseCode (200)
            .setHeader ("Content-Type", "application/json")
            .setBody (newToken.toString ()));

    JsonBearerToken fakeToken = JsonBearerToken.generateRandomToken ();

    Response <JsonBearerToken> response =
        this.gatekeeperClient_.refreshToken (fakeToken.refreshToken)
                              .execute ();

    JsonBearerToken refreshToken = response.body ();

    Assert.assertEquals (newToken.accessToken, refreshToken.accessToken);
    Assert.assertEquals (newToken.refreshToken, refreshToken.refreshToken);
  }

  @Test
  public void testCreateAccount () throws Exception
  {
    this.initializeClient ();

    JsonAccount mockAccount = new JsonAccount ();
    mockAccount._id = "my_id";
    mockAccount.username = STRING_USERNAME;
    mockAccount.password = STRING_PASSWORD;
    mockAccount.email = STRING_EMAIL;

    Resource mockResource = new Resource ("account", mockAccount);

    this.server_.enqueue (
        new MockResponse ()
            .setResponseCode (200)
            .setHeader ("Content-Type", "application/json")
            .setBody (this.gatekeeperClient_.getGson ().toJson (mockResource))
    );

    Response <Resource> response =
        this.gatekeeperClient_.createAccount (STRING_USERNAME, STRING_PASSWORD, STRING_EMAIL)
                              .execute ();

    Resource rc = response.body ();
    JsonAccount account = rc.getAs ("account");

    Assert.assertEquals (mockAccount._id, account._id);
    Assert.assertEquals (mockAccount.username, account.username);
    Assert.assertEquals (mockAccount.password, account.password);
    Assert.assertEquals (mockAccount.email, account.email);
  }

  /**
   * Helper method to get the user token for testing purposes.
   *
   * @param username
   * @param password
   * @return
   */
  private JsonBearerToken getUserToken (String username, String password)
    throws Exception
  {
    // Setup the mocked routes.
    JsonBearerToken fakeToken = JsonBearerToken.generateRandomToken ();

    this.server_.enqueue (
        new MockResponse ()
            .setResponseCode (200)
            .setHeader ("Content-Type", "application/json")
            .setBody (fakeToken.toString ()));

    Response <JsonBearerToken> response =
        this.gatekeeperClient_.getUserToken (username, password)
                              .execute ();

    JsonBearerToken body = response.body ();
    Assert.assertEquals (fakeToken.accessToken, body.accessToken);
    Assert.assertEquals (fakeToken.refreshToken, body.refreshToken);

    return body;
  }

  /**
   * Initialize the GatekeeperClient variable.
   *
   * @throws Exception
   */
  private void initializeClient ()
      throws Exception
  {
    // Enqueue a fake token.
    final JsonBearerToken fakeToken = JsonBearerToken.generateRandomToken ();

    this.server_.enqueue (
        new MockResponse ()
            .setResponseCode (200)
            .setHeader ("Content-Type", "application/json")
            .setBody (fakeToken.toString ())
    );

    synchronized (this)
    {
      GatekeeperClient.initialize (this.clientConfig_, this.httpClient_, this);
      this.wait ();
    }
  }

  @Override
  public void onInitialized (GatekeeperClient client)
  {
    Assert.assertEquals (this.clientConfig_.clientId, client.getClientId ());
    Assert.assertEquals (this.clientConfig_.baseUri, client.getBaseUrl ());

    synchronized (this)
    {
      this.gatekeeperClient_ = client;
      this.notify ();
    }
  }

  @Override
  public void onInitializeFailed (Throwable e)
  {
    synchronized (this)
    {
      Assert.fail (e.getMessage ());
      this.notify ();
    }
  }
}
