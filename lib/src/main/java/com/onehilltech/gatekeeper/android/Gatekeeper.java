package com.onehilltech.gatekeeper.android;

import com.onehilltech.backbone.http.BackboneHttp;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.config.*;

public class Gatekeeper
{
  public static final String ACTION_SIGNED_IN = "com.onehilltech.gatekeeper.ACTION_SIGNED_IN";

  public static final String ACTION_SIGNED_OUT = "com.onehilltech.gatekeeper.ACTION_SIGNED_OUT";

  public static void initialize ()
  {
    BackboneHttp.initialize ();
    FlowManager.initModule (GatekeeperGeneratedDatabaseHolder.class);
  }
}
