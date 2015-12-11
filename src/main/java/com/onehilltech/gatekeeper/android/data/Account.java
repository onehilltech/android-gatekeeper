package com.onehilltech.gatekeeper.android.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.ModelContainer;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.sql.language.Select;
import com.raizlabs.android.dbflow.structure.BaseModel;

@ModelContainer
@Table (database=GatekeeperDatabase.class, name="accounts")
public class Account extends BaseModel
{
  @Column
  @PrimaryKey
  public String _id;

  public Account ()
  {
    // required constructor
  }

  /**
   * Lookup an account, or create a new account if one does not exist.
   *
   * @param id
   * @return
   */
  @JsonCreator
  public static Account lookupOrCreate (@JsonProperty("_id") String id)
  {
    Account account =
        new Select ()
            .from (Account.class)
            .where (Account_Table._id.eq (id))
            .querySingle ();

    if (account == null)
    {
      account = new Account (id);
      account.save ();
    }

    return account;
  }

  public String getId ()
  {
    return this._id;
  }

  private Account (String id)
  {
    this._id = id;
  }
}
