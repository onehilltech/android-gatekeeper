package com.onehilltech.gatekeeper.android;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class ProtectedRequest<T> extends Request <T>
{
  private final ObjectMapper objMapper_ = new ObjectMapper ();

  private final ResponseListener <T> listener_;

  private final BearerToken token_;

  private final HashMap<String, String> params_ = new HashMap <> ();

  private final Class <T> type_;

  /**
   * Initializing constructor.
   *
   * @param method
   * @param url
   * @param token
   * @param listener
   */
  public ProtectedRequest (int method,
                           String url,
                           BearerToken token,
                           ResponseListener <T> listener)
  {
    super (method, url, listener);

    this.token_ = token;
    this.listener_ = listener;

    Type superClass = this.getClass ().getGenericSuperclass ();

    if (superClass instanceof Class <?>)
      throw new IllegalArgumentException ("Internal error: TypeReference constructed without actual type information");

    this.type_ = (Class<T>)((ParameterizedType) superClass).getActualTypeArguments()[0];
  }

  @Override
  protected void deliverResponse (T response)
  {
    this.listener_.onResponse (response);
  }

  public ProtectedRequest<T> addParams (Map <String, String> params)
  {
    this.params_.putAll (params);
    return this;
  }

  public ProtectedRequest<T> addParam (String name, String value)
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
      T value = this.objMapper_.readValue (json, this.type_);

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
