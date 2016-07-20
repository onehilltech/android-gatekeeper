package com.onehilltech.gatekeeper.android;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.onehilltech.metadata.ManifestMetadata;
import com.onehilltech.metadata.MetadataProperty;

/**
 * Activity for using Gatekeeper to login
 */
public class LoginActivity extends AppCompatActivity
    implements LoginFragment.LoginFragmentListener
{
  private static final String TAG = "LoginActivity";

  private static final int REQUEST_USER_CREDENTIALS = 9000;

  private static final String METADATA_LOGIN_SUCCESS_REDIRECT_ACTIVITY = "com.onehilltech.gatekeeper.android.LOGIN_SUCCESS_REDIRECT_ACTIVITY";
  private static final String METADATA_NEW_ACCOUNT_ACTIVITY = "com.onehilltech.gatekeeper.android.NEW_ACCOUNT_ACTIVITY";

  private final LocalMetadata metadata_ = new LocalMetadata ();

  private static class LocalMetadata
  {
    @MetadataProperty(name=METADATA_LOGIN_SUCCESS_REDIRECT_ACTIVITY)
    public String loginSuccessRedirectActivity;

    @MetadataProperty(name=METADATA_NEW_ACCOUNT_ACTIVITY)
    public String newAccountActivity;

    public Intent getLoginSuccessRedirectIntent (Context context)
    {
      String className = this.getClassName (context, this.loginSuccessRedirectActivity);
      Intent intent = new Intent ();
      intent.setComponent (new ComponentName (context, className));

      return intent;
    }

    public boolean hasNewAccountActivity ()
    {
      return this.newAccountActivity != null;
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

  @Override
  protected void onActivityResult (int requestCode, int resultCode, Intent data)
  {
    if (requestCode == REQUEST_USER_CREDENTIALS)
    {
      if (resultCode == RESULT_OK)
      {
        // Update the current fragment with the username/password. We are going to replace
        // the current fragment since we want to make sure the username/password is retained
        // across the lifetime of the fragment.
        String username = data.getStringExtra (NewAccountActivity.RESULT_DATA_USERNAME);
        String password = data.getStringExtra (NewAccountActivity.RESULT_DATA_PASSWORD);

        LoginFragment loginFragment = LoginFragment.newInstance (username, password);
        FragmentTransaction transaction = this.getSupportFragmentManager ().beginTransaction ();
        transaction.replace (R.id.fragment_container, loginFragment);
        transaction.commitAllowingStateLoss ();
      }
    }
  }

  @Override
  protected void onCreate (Bundle savedInstanceState)
  {
    super.onCreate (savedInstanceState);

    // Set the content view for this activity.
    this.setContentView (R.layout.activity_login);

    try
    {
      ManifestMetadata.get (this).initFromMetadata (this.metadata_);

      // If we're being restored from a previous state, then we don't need to do
      // anything and should return or else we could end up with overlapping fragments.
      LoginFragment loginFragment = this.onCreateFragment ();

      this.getSupportFragmentManager ()
          .beginTransaction ()
          .replace (R.id.fragment_container, loginFragment)
          .commit ();

      // Load the metadata for this activity. There is a good chance that
      // the this activity has a LOGIN_SUCCESS_REDIRECT_ACTIVITY meta-data
      // property defined.
    }
    catch (Exception e)
    {
      throw new IllegalStateException ("Failed to create activity", e);
    }
  }

  protected LoginFragment onCreateFragment ()
  {
    return new LoginFragment ();
  }

  @Override
  public void onLoginComplete (LoginFragment loginFragment)
  {
    if (this.metadata_.loginSuccessRedirectActivity != null)
    {
      // There is an activity to be started after success login. Get its intent
      // and start it before finishing this activity.
      Intent intent = this.metadata_.getLoginSuccessRedirectIntent (this);
      this.startActivity (intent);
    }

    // Finish this activity.
    this.finish ();
  }

  @Override
  public void onCreateNewAccount (LoginFragment fragment)
  {
    Intent intent =
        this.metadata_.hasNewAccountActivity () ?
            this.metadata_.getNewAccountActivity (this) :
            NewAccountActivity.newIntent (this);

    this.startActivityForResult (intent, REQUEST_USER_CREDENTIALS);
  }

  @Override
  public void onLoginError (LoginFragment loginFragment, Throwable t)
  {
    Log.e (TAG, t.getLocalizedMessage (), t);
  }
}

