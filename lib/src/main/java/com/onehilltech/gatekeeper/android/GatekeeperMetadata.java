package com.onehilltech.gatekeeper.android;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import com.onehilltech.metadata.MetadataProperty;

public final class GatekeeperMetadata
{
  private static final String METADATA_LOGIN_SUCCESS_REDIRECT_ACTIVITY =
      "com.onehilltech.gatekeeper.android.LOGIN_SUCCESS_REDIRECT_ACTIVITY";

  private static final String METADATA_NEW_ACCOUNT_ACTIVITY =
      "com.onehilltech.gatekeeper.android.NEW_ACCOUNT_ACTIVITY";

  @MetadataProperty(name = METADATA_LOGIN_SUCCESS_REDIRECT_ACTIVITY)
  public String loginSuccessRedirectActivity;

  @MetadataProperty(name = METADATA_NEW_ACCOUNT_ACTIVITY)
  public String newAccountActivity;

  public Intent getLoginSuccessRedirectIntent (Context context)
  {
    String className = this.getClassName (context, this.loginSuccessRedirectActivity);
    Intent intent = new Intent ();
    intent.setComponent (new ComponentName (context, className));

    return intent;
  }

  public Intent getNewAccountActivity (Context context)
  {
    String className = this.getClassName (context, this.newAccountActivity);
    Intent intent = new Intent ();
    intent.setComponent (new ComponentName (context, className));

    return intent;
  }

  private String getClassName (Context context, String name)
  {
    return name != null && name.startsWith (".") ? context.getPackageName () + name : name;
  }
}
