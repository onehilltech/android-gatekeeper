package com.onehilltech.gatekeeper.android.data;

import com.raizlabs.android.dbflow.annotation.Database;

@Database (
    name=GatekeeperDatabase.NAME,
    version=GatekeeperDatabase.VERSION,
    module=GatekeeperDatabase.MODULE)
public class GatekeeperDatabase
{
  public static final String NAME = "gatekeeper";

  public static final int VERSION = 1;

  public static final String MODULE = "Gatekeeper";
}
