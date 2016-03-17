package com.onehilltech.gatekeeper.android.examples.standard;

import android.app.Application;

import com.raizlabs.android.dbflow.config.FlowManager;

public class TheApplication extends Application
{
  @Override
  public void onCreate ()
  {
    super.onCreate ();
    FlowManager.init (this);
  }
}
