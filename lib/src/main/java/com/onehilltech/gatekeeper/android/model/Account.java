package com.onehilltech.gatekeeper.android.model;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.ModelContainer;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.structure.BaseModel;

@ModelContainer
@Table (database=GatekeeperDatabase.class, name="accounts")
public class Account extends BaseModel
{
  @PrimaryKey
  String _id;

  @Column
  public String username;

  @Column
  public String email;

  Account ()
  {
    // required constructor
  }

  public Account (String id)
  {
    this._id = id;
  }

  public String getId ()
  {
    return this._id;
  }
}
