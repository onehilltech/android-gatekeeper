package com.onehilltech.gatekeeper.android;

import com.onehilltech.backbone.http.BackboneHttp;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.config.*;

public class Gatekeeper
{
  public static void initialize ()
  {
    BackboneHttp.initialize ();
    FlowManager.initModule (GatekeeperGeneratedDatabaseHolder.class);
  }
}
