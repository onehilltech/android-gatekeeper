package com.onehilltech.gatekeeper.android.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.raizlabs.android.dbflow.annotation.ModelContainer;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.sql.language.SQLite;
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

  /**
   * Lookup an account, or create a new account if one does not exist.
   *
   * @param id      The account id
   * @return        Account object
   */
  @JsonCreator
  public static Account lookupOrCreate (@JsonProperty("_id") String id)
  {
    Account account =
        SQLite.select ()
              .from (Account.class)
              .where (Account$Table._id.eq (id))
              .querySingle ();

    if (account != null)
      return account;

    account = new Account (id);
    account.insert ();

    return account;
  }

  public String getId ()
  {
    return this._id;
  }
}
