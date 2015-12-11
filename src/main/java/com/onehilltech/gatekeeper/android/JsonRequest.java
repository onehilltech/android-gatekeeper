package com.onehilltech.gatekeeper.android;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onehilltech.gatekeeper.android.data.AccessToken;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class JsonRequest <T> extends Request <T>
{
  public static final String CHARSET = "utf-8";

  public static final String CONTENT_TYPE =
      String.format ("application/json; charset=%s", CHARSET);

  private final ObjectMapper objMapper_ = new ObjectMapper ();

  private final ResponseListener <T> listener_;

  private final AccessToken token_;

  private final TypeReference <T> typeReference_;

  private Object dataObj_;

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
                      AccessToken token,
                      TypeReference<T> typeReference,
                      ResponseListener<T> listener)
  {
    super (method, url, listener);

    this.token_ = token;
    this.listener_ = listener;
    this.typeReference_ = typeReference;
  }

  /**
   * Set the data for the request.
   *
   * @param data
   */
  public void setData (Object data)
  {
    this.dataObj_ = data;
  }

  public Object getData ()
  {
    return this.dataObj_;
  }

  @Override
  protected void deliverResponse (T response)
  {
    this.listener_.onResponse (response);
  }

  @Override
  public String getBodyContentType ()
  {
    return CONTENT_TYPE;
  }

  @Override
  public byte[] getBody () throws AuthFailureError
  {
    try
    {
      return this.objMapper_.writeValueAsBytes (this.dataObj_);
    }
    catch (JsonProcessingException e)
    {
      return null;
    }
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
      T value = this.objMapper_.readValue (json, this.typeReference_);

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
