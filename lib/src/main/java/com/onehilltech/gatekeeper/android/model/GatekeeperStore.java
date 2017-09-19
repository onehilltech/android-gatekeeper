package com.onehilltech.gatekeeper.android.model;

import android.content.Context;

import com.onehilltech.backbone.data.DataStore;
import com.onehilltech.gatekeeper.android.GatekeeperSessionClient;

public class GatekeeperStore
{
  private static DataStore dataStore_;

  public static DataStore getInstance (Context context)
  {
    if (dataStore_ != null)
      return dataStore_;

    GatekeeperSessionClient sessionClient = new GatekeeperSessionClient.Builder (context).build ();

    dataStore_ =
        new DataStore.Builder (GatekeeperDatabase.class)
            .setBaseUrl (sessionClient.getClient ().getBaseUrlWithVersion ())
            .setHttpClient (sessionClient.getUserClient ())
            .build ();

    return dataStore_;
  }
}
