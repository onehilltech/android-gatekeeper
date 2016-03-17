package com.onehilltech.gatekeeper.android;

import com.android.volley.Response;

/**
 * Helper class that implements both the Response.OnRegistrationCompleteListener and Response.ErrorListener
 * interface to reduce the number of objects created.
 *
 * @param <T>
 */
public interface ResponseListener <T>
    extends Response.Listener<T>, Response.ErrorListener
{

}
