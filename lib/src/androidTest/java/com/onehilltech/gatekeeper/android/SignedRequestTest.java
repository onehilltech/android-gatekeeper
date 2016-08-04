package com.onehilltech.gatekeeper.android;

import android.support.test.runner.AndroidJUnit4;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.fasterxml.jackson.core.type.TypeReference;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.HttpURLConnection;
import java.util.HashMap;

@RunWith (AndroidJUnit4.class)
public class SignedRequestTest
{
  public static class Message
  {
    public int id;
    public String message;
  }

  @Test
  public void testParseNetworkResponse ()
  {
    // Test with a basic boolean type.
    String trueString = "true";
    HashMap <String, String> headers = new HashMap<> ();
    headers.put ("Content-Type", "UTF-8");

    NetworkResponse trueNetworkResponse =
        new NetworkResponse (
            HttpURLConnection.HTTP_OK,
            trueString.getBytes (),
            headers,
            false);

    SignedRequest<Boolean> req1 =
        new SignedRequest<> (
            Request.Method.GET,
            "/does-not-matter",
            null,
            new TypeReference<Boolean> () { },
            new ResponseListener<Boolean> ()
            {
              @Override
              public void onResponse (Boolean response)
              {

              }

              @Override
              public void onErrorResponse (VolleyError error)
              {

              }
            });

    Response <Boolean> res1 = req1.parseNetworkResponse (trueNetworkResponse);
    Assert.assertTrue (res1.result);

    // Test with a complex type.
    String content = "{\"id\": 123, \"message\": \"Hello, World!\"}";

    NetworkResponse jsonObjNetworkResponse =
        new NetworkResponse (
            HttpURLConnection.HTTP_OK,
            content.getBytes (),
            headers,
            false);

    SignedRequest<Message> req2 =
        new SignedRequest<> (
            Request.Method.GET,
            "/does-not-matter",
            null,
            new TypeReference<Message> () { },
            new ResponseListener<Message> ()
            {
              @Override
              public void onResponse (Message response)
              {

              }

              @Override
              public void onErrorResponse (VolleyError error)
              {

              }
            });

    Response <Message> res2 = req2.parseNetworkResponse (jsonObjNetworkResponse);

    Assert.assertEquals (123, res2.result.id);
    Assert.assertEquals ("Hello, World!", res2.result.message);
  }
}