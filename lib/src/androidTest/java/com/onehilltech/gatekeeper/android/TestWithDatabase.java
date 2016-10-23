package com.onehilltech.gatekeeper.android;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import com.raizlabs.android.dbflow.config.FlowConfig;
import com.raizlabs.android.dbflow.config.FlowManager;

import org.junit.Before;

public class TestWithDatabase
{
  private boolean deleteDatabase_;

  protected TestWithDatabase ()
  {
    this (true);
  }

  protected TestWithDatabase (boolean deleteDatabase)
  {
    this.deleteDatabase_ = deleteDatabase;
  }

  @Before
  public void setup ()
      throws Exception
  {
    // Initialize the Gatekeeper framework.
    Context context = InstrumentationRegistry.getTargetContext ();

    // Delete the old database
    if (this.deleteDatabase_)
      context.deleteDatabase ("gatekeeper.db");

    // Initialize the DBFlow framework.
    FlowManager.init (
        new FlowConfig.Builder (context)
            .openDatabasesOnInit (true)
            .build ());

    // Initialize the Gatekeeper database module.
    Gatekeeper.initialize (context);
  }
}
