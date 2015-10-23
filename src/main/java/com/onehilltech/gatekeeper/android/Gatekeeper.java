package com.onehilltech.gatekeeper.android;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

/**
 * Main entry point class for the Gatekeeper framework.
 */
public class Gatekeeper
{
  private static GatekeeperClient client_;

  /**
   * Initialize the framework.
   *
   * @param context
   * @param baseUri
   * @param clientId
   * @param clientSecret
   * @return
   */
  public static GatekeeperClient initialize (Context context, String baseUri, String clientId, String clientSecret)
  {
    return initialize (baseUri, clientId, clientSecret, Volley.newRequestQueue (context));
  }

  public static GatekeeperClient initialize (String baseUri,
                                             String clientId,
                                             String clientSecret,
                                             RequestQueue requestQueue)
  {
    if (client_ != null)
      throw new IllegalStateException ("Gatekeeper already initialized");

    client_ = new GatekeeperClient (baseUri, clientId, clientSecret, requestQueue);
    return client_;
  }

  /**
   * Get the GatekeeperClient object.
   *
   * @return
   */
  public static GatekeeperClient getClient ()
  {
    if (client_ == null)
      throw new IllegalStateException ("Gatekeeper is not initialized");

    return client_;
  }

  /**
   * Test if the framework has been initialized.
   *
   * @return
   */
  public static boolean isInitialized ()
  {
    return client_  != null;
  }
}
