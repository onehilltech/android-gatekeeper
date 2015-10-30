package com.onehilltech.gatekeeper.android;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class JsonRequest <T> extends Request <T>
{
  private final ObjectMapper objMapper_ = new ObjectMapper ();

  private final ResponseListener <T> listener_;

  private final BearerToken token_;

  private final HashMap<String, String> params_ = new HashMap <> ();

  /**
   * Initializing constructor.
   *
   * @param method
   * @param url
   * @param token
   * @param listener
   */
  public JsonRequest (int method,
                      String url,
                      BearerToken token,
                      ResponseListener<T> listener)
  {
    super (method, url, listener);

    this.token_ = token;
    this.listener_ = listener;
  }

  @Override
  protected void deliverResponse (T response)
  {
    this.listener_.onResponse (response);
  }

  public JsonRequest<T> addParams (Map <String, String> params)
  {
    this.params_.putAll (params);
    return this;
  }

  public JsonRequest<T> addParam (String name, String value)
  {
    this.params_.put (name, value);
    return this;
  }

  @Override
  protected Map<String, String> getParams () throws AuthFailureError
  {
    return this.params_;
  }

  @Override
  public String getBodyContentType ()
  {
    return "application/json; charset=utf-8";
  }

  @Override
  public Map<String, String> getHeaders () throws AuthFailureError
  {
    HashMap<String, String> headers = new HashMap <> (super.getHeaders ());

    if (this.token_ != null)
      headers.put ("Authorization", "Bearer " + this.token_.getAccessToken ());

    return headers;
  }

  @Override
  protected Response <T> parseNetworkResponse (NetworkResponse response)
  {
    try
    {
      String json = new String(response.data, HttpHeaderParser.parseCharset (response.headers));
      T value = this.objMapper_.readValue (json, this.listener_.getResponseType ());

      return Response.success (value, HttpHeaderParser.parseCacheHeaders (response));
    }
    catch (UnsupportedEncodingException e)
    {
      return Response.error(new ParseError (e));
    }
    catch (IOException e)
    {
      return Response.error(new ParseError (e));
    }
  }
}
