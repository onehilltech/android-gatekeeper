package com.onehilltech.gatekeeper.android;

import com.android.volley.Response;
import com.android.volley.VolleyError;

/**
 * Helper class that implements both the Response.Listener and Response.ErrorListener
 * interface to reduce the number of objects created.
 *
 * @param <T>
 */
public class ResponseListener<T> implements Response.Listener <T>, Response.ErrorListener
{
    @Override
    public void onErrorResponse (VolleyError error)
    {

    }

    @Override
    public void onResponse (T response)
    {

    }
}
