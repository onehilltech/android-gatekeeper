package com.onehilltech.gatekeeper.android.model;

import com.raizlabs.android.dbflow.annotation.Database;

@Database (
    name=GatekeeperDatabase.DATABASE_NAME,
    version=GatekeeperDatabase.VERSION,
    generatedClassSeparator="$")
public class GatekeeperDatabase
{
  public static final int VERSION = 2;
  public static final String DATABASE_NAME = "gatekeeper";
}
