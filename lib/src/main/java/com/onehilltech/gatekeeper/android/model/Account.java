package com.onehilltech.gatekeeper.android.model;

import com.onehilltech.backbone.data.DataModel;
import com.onehilltech.backbone.data.serializers.ObjectIdSerializer;
import com.onehilltech.backbone.objectid.ObjectId;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;

@Table (database=GatekeeperDatabase.class, name="accounts")
public class Account extends DataModel <Account>
{
  @PrimaryKey
  @Column(typeConverter = ObjectIdSerializer.class)
  public ObjectId _id;

  @Column
  public String username;

  @Column
  public String email;

  Account ()
  {
    // required constructor
  }

  public Account (ObjectId id)
  {
    this._id = id;
  }
}
