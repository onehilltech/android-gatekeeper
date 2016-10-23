package com.onehilltech.gatekeeper.android;

import android.content.Context;

import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.config.*;

import okhttp3.OkHttpClient;

public class Gatekeeper
{
  /// Global OkHttpClient used by Gatekeeper
  private static OkHttpClient httpClient_ = new OkHttpClient.Builder ().build ();

  /**
   *
   * @param context
   */
  public static void initialize (Context context)
  {
    FlowManager.initModule (GatekeeperGeneratedDatabaseHolder.class);
  }

  public static OkHttpClient getHttpClient ()
  {
    return httpClient_;
  }

  public static void setHttpClient (OkHttpClient httpClient)
  {
    httpClient_ = httpClient;
  }
}
