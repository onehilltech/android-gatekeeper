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
 * Created by hillj on 9/19/15.
 */
public class GatekeeperRequest <T> extends Request <T>
{
  private final ObjectMapper objMapper_ = new ObjectMapper ();

  private final Class <T> clazz_;

  private final Response.Listener <T> listener_;

  private final BearerToken token_;

  private final HashMap<String, String> params_ = new HashMap <> ();

  /**
   * Make a GET request and return a parsed object from JSON.
   *
   * @param url URL of the request to make
   * @param clazz Relevant class object, for Gson's reflection
   */
  public GatekeeperRequest (int method,
                            String url,
                            Class<T> clazz,
                            BearerToken token,
                            Response.Listener<T> listener,
                            Response.ErrorListener errorListener)
  {
    super (method, url, errorListener);

    this.clazz_ = clazz;
    this.token_ = token;
    this.listener_ = listener;
  }

  @Override
  protected void deliverResponse (T response)
  {
    this.listener_.onResponse (response);
  }

  public GatekeeperRequest <T> addParams (Map <String, String> params)
  {
    this.params_.putAll (params);
    return this;
  }

  public GatekeeperRequest <T> addParam (String name, String value)
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
  public Map<String, String> getHeaders () throws AuthFailureError
  {
    Map<String, String> headers = super.getHeaders ();
    headers.put ("Authorization", "Bearer " + this.token_.getAccessToken ());

    return headers;
  }

  @Override
  protected Response <T> parseNetworkResponse (NetworkResponse response)
  {
    try
    {
      String json = new String(response.data, HttpHeaderParser.parseCharset (response.headers));

      return Response.success (
          this.objMapper_.readValue (json, this.clazz_),
          HttpHeaderParser.parseCacheHeaders (response));
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
