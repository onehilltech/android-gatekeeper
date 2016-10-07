package com.onehilltech.gatekeeper.android;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import com.onehilltech.metadata.MetadataProperty;

/**
 * Created by hilljh on 10/7/16.
 */
class LoginMetadata
{
  private static final String METADATA_LOGIN_SUCCESS_REDIRECT_ACTIVITY =
      "com.onehilltech.gatekeeper.android.LOGIN_SUCCESS_REDIRECT_ACTIVITY";

  private static final String METADATA_NEW_ACCOUNT_ACTIVITY =
      "com.onehilltech.gatekeeper.android.NEW_ACCOUNT_ACTIVITY";

  @MetadataProperty(name = METADATA_LOGIN_SUCCESS_REDIRECT_ACTIVITY)
  String loginSuccessRedirectActivity;

  @MetadataProperty(name = METADATA_NEW_ACCOUNT_ACTIVITY)
  String newAccountActivity;

  Intent getLoginSuccessRedirectIntent (Context context)
  {
    String className = this.getClassName (context, this.loginSuccessRedirectActivity);
    Intent intent = new Intent ();
    intent.setComponent (new ComponentName (context, className));

    return intent;
  }

  boolean hasNewAccountActivity ()
  {
    return this.newAccountActivity != null;
  }

  Intent getNewAccountActivity (Context context)
  {
    String className = this.getClassName (context, this.newAccountActivity);
    Intent intent = new Intent ();
    intent.setComponent (new ComponentName (context, className));

    return intent;
  }

  String getClassName (Context context, String name)
  {
    return name != null && name.startsWith (".") ? context.getPackageName () + name : name;
  }
}
