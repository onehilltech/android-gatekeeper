package com.onehilltech.gatekeeper.android;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import okhttp3.mockwebserver.MockResponse;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.GET;

public class RefreshTokenCallbackTest extends SingleUserSessionTest
{
  interface MockService
  {
    @GET("mock-message")
    Call<String> getMessage ();
  }

  private MockService mockService_;

  @Before
  public void setup () throws Exception
  {
    super.setup ();

    this.mockService_ = this.retrofit_.create (MockService.class);
    this.initializeGatekeeperClient ();
  }

  @Test
  public void smokeTest () throws Exception
  {
    final String body = "{\"message\": \"Hello, World\"}";

    this.server_.enqueue (
        new MockResponse ()
            .setResponseCode (401)
            .setHeader ("Content-Type", "application/json")
            .setBody (body));

    /*
    this.server_.enqueue (
        new MockResponse ()
            .setResponseCode (200)
            .setHeader ("Content-Type", "application/json")
            .setBody ("DONE!!")
    );
    */

    synchronized (this)
    {
      this.mockService_.getMessage ().enqueue (new RefreshTokenCallback<String> (this.sessionClient_)
      {
        @Override
        public void onHandleResponse (Call<String> call, Response<String> response)
        {
          synchronized (RefreshTokenCallbackTest.this)
          {
            Assert.assertEquals (body, response.body ());
            this.notify ();
          }
        }

        @Override
        public void onFailure (Call<String> call, Throwable t)
        {
          synchronized (RefreshTokenCallbackTest.this)
          {
            Assert.fail (t.getLocalizedMessage ());
            this.notify ();
          }
        }
      });

      this.wait ();
    }
  }
}
