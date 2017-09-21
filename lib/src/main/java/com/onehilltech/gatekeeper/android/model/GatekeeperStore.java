package com.onehilltech.gatekeeper.android.model;

import android.content.Context;

import com.onehilltech.backbone.data.DataStore;
import com.onehilltech.backbone.data.DataStoreAdapter;
import com.onehilltech.backbone.data.serializers.ObjectIdSerializer;
import com.onehilltech.backbone.objectid.ObjectId;
import com.onehilltech.gatekeeper.android.GatekeeperSessionClient;

import okhttp3.CacheControl;

public class GatekeeperStore
{
  private static DataStore dataStore_;

  private static DataStoreAdapter dataStoreAdapter_ = request ->
      request.url ().encodedPath ().endsWith ("/accounts/me") && request.method ().equals ("GET") ?
          request.newBuilder ().cacheControl (CacheControl.FORCE_NETWORK).build () :
          request;

  public static DataStore getInstance (Context context)
  {
    if (dataStore_ != null)
      return dataStore_;

    GatekeeperSessionClient sessionClient = GatekeeperSessionClient.getInstance (context);

    dataStore_ = new DataStore.Builder (context, GatekeeperDatabase.class)
        .setBaseUrl (sessionClient.getClient ().getBaseUrlWithVersion ())
        .setApplicationAdapter (dataStoreAdapter_)
        .setHttpClient (sessionClient.getUserClient ())
        .addTypeAdapter (ObjectId.class, new ObjectIdSerializer ())
        .build ();

    return dataStore_;
  }
}
