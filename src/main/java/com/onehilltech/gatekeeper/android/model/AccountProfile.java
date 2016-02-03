package com.onehilltech.gatekeeper.android.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.ForeignKey;
import com.raizlabs.android.dbflow.annotation.ForeignKeyAction;
import com.raizlabs.android.dbflow.annotation.ModelContainer;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.structure.BaseModel;
import com.raizlabs.android.dbflow.structure.container.ForeignKeyContainer;

import java.util.Map;

@ModelContainer
@Table (database=GatekeeperDatabase.class, name="account_profile")
public class AccountProfile extends BaseModel
{
  @PrimaryKey(autoincrement=true)
  long _id;

  @ForeignKey(
      onDelete=ForeignKeyAction.CASCADE,
      onUpdate=ForeignKeyAction.CASCADE,
      saveForeignKeyModel=false
  )
  ForeignKeyContainer <Account> account;

  @Column(name="first_name")
  String firstName;

  @Column(name="middle_name")
  String middleName;

  @Column(name="last_name")
  String lastName;

  @Column(name="email")
  String email;

  @Column(name="image")
  String image;

  public AccountProfile ()
  {

  }

  @JsonCreator
  public static AccountProfile fromJson (Map<String,Object> props)
  {
    String accountId = (String)props.get ("_id");
    Account account = Account.lookupOrCreate (accountId);

    AccountProfile profile =
        SQLite.select ().from (AccountProfile.class)
              .where (AccountProfile_Table.account__id.eq (accountId))
              .querySingle ();

    if (profile == null)
    {
      profile = new AccountProfile ();
      profile.account = FlowManager.getContainerAdapter (Account.class).toForeignKeyContainer (account);
    }

    profile.firstName = (String)props.get ("first_name");
    profile.middleName = (String)props.get ("middle_name");
    profile.lastName = (String)props.get ("last_name");
    profile.email = (String)props.get ("email");
    profile.image = (String)props.get ("image");

    return profile;
  }

  public Account getAccount ()
  {
    return this.account.toModel ();
  }

  public String getFirstName ()
  {
    return this.firstName;
  }

  public String getMiddleName ()
  {
    return this.middleName;
  }

  public String getLastName ()
  {
    return this.lastName;
  }

  public String getFullName ()
  {
    StringBuilder builder = new StringBuilder ();

    if (this.firstName != null)
      builder.append (this.firstName).append (" ");

    if (this.middleName != null)
      builder.append (this.middleName.charAt (0)).append (". ");

    if (this.lastName != null)
      builder.append (this.lastName);

    return builder.toString ();
  }

  public String getEmail ()
  {
    return this.email;
  }

  public String getImage ()
  {
    return this.image;
  }
}
