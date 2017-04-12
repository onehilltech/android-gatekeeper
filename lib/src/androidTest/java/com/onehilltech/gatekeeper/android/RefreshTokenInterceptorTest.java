package com.onehilltech.gatekeeper.android;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import com.onehilltech.backbone.http.BackboneHttpDatabase;
import com.onehilltech.gatekeeper.android.http.JsonBearerToken;
import com.onehilltech.gatekeeper.android.model.GatekeeperDatabase;
import com.onehilltech.gatekeeper.android.model.UserToken;
import com.raizlabs.android.dbflow.config.FlowConfig;
import com.raizlabs.android.dbflow.config.FlowManager;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.GET;

public class RefreshTokenInterceptorTest
{
  private MockWebServer mockWebServer_;
  private FakeService fakeService_;

  private static final String TEST_URL_PATH = "/helloworld";

  private JsonBearerToken fakeToken_;

  private GatekeeperSessionClient session_;

  @BeforeClass
  public static void init ()
  {
    Context targetContext = InstrumentationRegistry.getTargetContext ();

    FlowManager.init (new FlowConfig.Builder (targetContext).build ());
    Gatekeeper.initialize (targetContext);

    // Do a hard reset of the database.
    FlowManager.getDatabase (GatekeeperDatabase.class).reset (targetContext);
    FlowManager.getDatabase (BackboneHttpDatabase.class).reset (targetContext);
  }

  @Before
  public void setup () throws Exception
  {
    Context targetContext = InstrumentationRegistry.getTargetContext ();

    this.mockWebServer_ = new MockWebServer ();
    this.mockWebServer_.start ();

    RefreshTokenInterceptor refreshToken = new RefreshTokenInterceptor ();

    OkHttpClient client =
        new OkHttpClient.Builder ()
            .addInterceptor (refreshToken)
            .build ();

    HttpUrl baseUrl = this.mockWebServer_.url ("/");

    Retrofit retrofit =
        new Retrofit.Builder ()
            .baseUrl (baseUrl.toString ())
            .client (client)
            .build ();

    this.fakeService_ = retrofit.create (FakeService.class);

    // Add a single user token to the database.
    this.fakeToken_ = JsonBearerToken.generateRandomToken ();
    UserToken userToken = UserToken.fromToken ("me", this.fakeToken_);
    userToken.save ();

    GatekeeperClient.Configuration config =
        GatekeeperClient.Configuration
            .loadFromMetadata (InstrumentationRegistry.getTargetContext ());

    config.baseUri = baseUrl.toString ();

    GatekeeperClient gatekeeper =
        new GatekeeperClient.Builder (targetContext)
            .setConfiguration (config)
            .setClient (client)
            .build ();

    this.session_ =
        new GatekeeperSessionClient.Builder (targetContext)
            .setGatekeeperClient (gatekeeper)
            .build ();

    refreshToken.setSession (this.session_);
  }

  @After
  public void teardown () throws Exception
  {
    if (this.mockWebServer_ != null)
      this.mockWebServer_.shutdown ();
  }

  @Test
  public void testIntercept ()
      throws Exception
  {
    JsonBearerToken initToken = JsonBearerToken.generateRandomToken ();
    JsonBearerToken newToken = JsonBearerToken.generateRandomToken ();

    this.mockWebServer_.enqueue (new MockResponse ().setBody (initToken.toString ()));
    this.mockWebServer_.enqueue (new MockResponse ().setResponseCode (401));
    this.mockWebServer_.enqueue (new MockResponse ().setBody (newToken.toString ()));
    this.mockWebServer_.enqueue (new MockResponse ().setBody ("success"));

    Response<ResponseBody> res = this.fakeService_.getHelloWorld ().execute ();
    Assert.assertEquals (3, this.mockWebServer_.getRequestCount ());
    Assert.assertEquals ("success", res.body ().string ());

    RecordedRequest req1 = this.mockWebServer_.takeRequest ();
    Assert.assertEquals ("GET", req1.getMethod ());
    Assert.assertEquals ("/helloworld", req1.getPath ());

    RecordedRequest req2 = this.mockWebServer_.takeRequest ();
    Assert.assertEquals ("POST", req2.getMethod ());
    Assert.assertEquals ("/v1/oauth2/token", req2.getPath ());

    RecordedRequest req3 = this.mockWebServer_.takeRequest ();
    Assert.assertEquals ("GET", req3.getMethod ());
    Assert.assertEquals ("/helloworld", req3.getPath ());
  }

  @Test
  public void testInterceptRefreshFail () throws Exception
  {
    this.mockWebServer_.enqueue (new MockResponse ().setResponseCode (401));
    this.mockWebServer_.enqueue (new MockResponse ().setResponseCode (500));

    Response<ResponseBody> res = this.fakeService_.getHelloWorld ().execute ();
    Assert.assertEquals (2, this.mockWebServer_.getRequestCount ());
    Assert.assertEquals (401, res.code ());

    RecordedRequest req1 = this.mockWebServer_.takeRequest ();
    Assert.assertEquals ("GET", req1.getMethod ());
    Assert.assertEquals ("/helloworld", req1.getPath ());

    RecordedRequest req2 = this.mockWebServer_.takeRequest ();
    Assert.assertEquals ("POST", req2.getMethod ());
    Assert.assertEquals ("/v1/oauth2/token", req2.getPath ());
  }

  interface FakeService
  {
    @GET("helloworld")
    Call <ResponseBody> getHelloWorld ();
  }
}
