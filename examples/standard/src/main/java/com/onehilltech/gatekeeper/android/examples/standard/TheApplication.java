package com.onehilltech.gatekeeper.android.examples.standard;

import android.app.Application;

import com.onehilltech.gatekeeper.android.Gatekeeper;
import com.raizlabs.android.dbflow.config.FlowConfig;
import com.raizlabs.android.dbflow.config.FlowManager;

public class TheApplication extends Application
{
  @Override
  public void onCreate ()
  {
    super.onCreate ();

    FlowManager.init (
        new FlowConfig.Builder (this)
            .openDatabasesOnInit (true)
            .build ());

    Gatekeeper.initialize ();
  }
}
