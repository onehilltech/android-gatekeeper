package com.onehilltech.gatekeeper.android;

import android.support.test.runner.AndroidJUnit4;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.apache.http.HttpStatus;
import org.apache.http.protocol.HTTP;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;

@RunWith (AndroidJUnit4.class)
public class JsonRequestTest
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
    headers.put (HTTP.CONTENT_TYPE, "UTF-8");

    NetworkResponse trueNetworkResponse =
        new NetworkResponse (
            HttpStatus.SC_OK,
            trueString.getBytes (),
            headers,
            false);

    JsonRequest<Boolean> req1 =
        new JsonRequest<> (
            Request.Method.GET,
            "/does-not-matter",
            null,
            Boolean.class,
            new ResponseListener<Boolean> () {
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
            HttpStatus.SC_OK,
            content.getBytes (),
            headers,
            false);

    JsonRequest<Message> req2 =
        new JsonRequest<> (
            Request.Method.GET,
            "/does-not-matter",
            null,
            Message.class,
            new ResponseListener<Message> () {
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