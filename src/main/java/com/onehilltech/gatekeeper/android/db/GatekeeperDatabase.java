package com.onehilltech.gatekeeper.android.db;

import com.raizlabs.android.dbflow.annotation.Database;

@Database (name=GatekeeperDatabase.NAME, version=GatekeeperDatabase.VERSION)
public class GatekeeperDatabase
{
  public static final String NAME = "gatekeeper";

  public static final int VERSION = 1;
}
