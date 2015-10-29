package com.onehilltech.gatekeeper.android;

import android.animation.TypeEvaluator;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.fasterxml.jackson.core.type.TypeReference;

/**
 * Helper class that implements both the Response.Listener and Response.ErrorListener
 * interface to reduce the number of objects created.
 *
 * @param <T>
 */
public class ResponseListener <T> implements Response.Listener<T>, Response.ErrorListener
{
  private final TypeReference <T> type_;

  public ResponseListener (TypeReference <T> type)
  {
    this.type_ = type;
  }

  /**
   * Get the TypeReference of the response type.
   *
   * @return
   */
  public TypeReference <T> getResponseType ()
  {
    return this.type_;
  }

  @Override
  public void onErrorResponse (VolleyError error)
  {

  }

  @Override
  public void onResponse (T response)
  {

  }
}
