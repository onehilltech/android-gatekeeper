package com.onehilltech.gatekeeper.android.model;

import com.raizlabs.android.dbflow.annotation.ModelContainer;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.structure.BaseModel;

@ModelContainer
@Table (database=GatekeeperDatabase.class, name="account")
public class Account extends BaseModel
{
  @PrimaryKey
  public String _id;

  public Account ()
  {
    // required constructor
  }

  private Account (String id)
  {
    this._id = id;
  }

  public String getId ()
  {
    return this._id;
  }
}
