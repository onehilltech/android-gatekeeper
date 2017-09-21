package com.onehilltech.gatekeeper.android;

import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.config.*;

public class Gatekeeper
{
  public static void initialize ()
  {
    FlowManager.initModule (GatekeeperGeneratedDatabaseHolder.class);
  }
}
