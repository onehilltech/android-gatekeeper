package com.onehilltech.gatekeeper.android;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import com.onehilltech.gatekeeper.android.http.JsonBearerToken;
import com.raizlabs.android.dbflow.config.FlowConfig;
import com.raizlabs.android.dbflow.config.FlowManager;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SingleUserSessionTest
{
  protected GatekeeperClient.Configuration clientConfig_;
  protected SingleUserSessionClient sessionClient_;
  protected MockWebServer server_;
  protected HttpUrl serverUrl_;
  protected OkHttpClient httpClient_;
  protected Retrofit retrofit_;

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

    this.retrofit_ =
        new Retrofit.Builder ()
            .baseUrl (this.serverUrl_)
            .client (this.httpClient_)
            .addConverterFactory (GsonConverterFactory.create())
            .build ();

    // Prepare data needs to initialize the client.
    this.clientConfig_ = GatekeeperClient.Configuration.loadFromMetadata (targetContext);
    this.clientConfig_.baseUri = this.serverUrl_.uri ().toString ();

    this.initializeGatekeeperClient ();
  }

  protected void initializeGatekeeperClient () throws Exception
  {
    synchronized (this)
    {
      JsonBearerToken token = JsonBearerToken.generateRandomToken ();

      this.server_.enqueue (
          new MockResponse ()
              .setResponseCode (200)
              .setBody (token.toString ()));


      this.wait ();
    }
  }
}
