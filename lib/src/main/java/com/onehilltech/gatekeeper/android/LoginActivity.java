package com.onehilltech.gatekeeper.android;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.onehilltech.metadata.ManifestMetadata;
import com.onehilltech.metadata.MetadataProperty;

import java.lang.reflect.InvocationTargetException;

/**
 * Activity for using Gatekeeper to login
 */
public class LoginActivity extends AppCompatActivity
    implements LoginFragment.LoginFragmentListener
{
  private static final String TAG = "LoginActivity";

  private static final int REQUEST_USER_CREDENTIALS = 9000;

  private static final String METADATA_LOGIN_SUCCESS_REDIRECT_ACTIVITY = "com.onehilltech.gatekeeper.android.LOGIN_SUCCESS_REDIRECT_ACTIVITY";

  private static class LocalMetadata
  {
    @MetadataProperty(name=METADATA_LOGIN_SUCCESS_REDIRECT_ACTIVITY)
    public String loginSuccessRedirectActivity;

    public Intent getLoginSuccessRedirectIntent (Context context)
    {
      String className =
          this.loginSuccessRedirectActivity.startsWith (".") ?
              context.getPackageName () + this.loginSuccessRedirectActivity :
              this.loginSuccessRedirectActivity;

      Intent intent = new Intent ();
      intent.setComponent (new ComponentName (context, className));

      return intent;
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

  protected LoginFragment onCreateFragment ()
  {
    return new LoginFragment ();
  }

  @Override
  public void onLoginComplete (LoginFragment loginFragment)
  {
    try
    {
      // Load the local metadata for this activity.
      LocalMetadata metadata = new LocalMetadata ();
      ManifestMetadata.get (this).initFromMetadata (metadata);

      if (metadata.loginSuccessRedirectActivity != null)
      {
        // There is an activity to be started after success login. Get its intent
        // and start it before finishing this activity.
        Intent intent = metadata.getLoginSuccessRedirectIntent (this);
        this.startActivity (intent);
      }

      // Finish this activity.
      this.finish ();
    }
    catch (PackageManager.NameNotFoundException | ClassNotFoundException | InvocationTargetException | IllegalAccessException e)
    {
      throw new IllegalStateException ("Cannot launch successful login activity", e);
    }
  }

  @Override
  public void onCreateNewAccount (LoginFragment fragment)
  {
    Intent intent = NewAccountActivity.newIntent (this);
    this.startActivityForResult (intent, REQUEST_USER_CREDENTIALS);
  }

  @Override
  public void onLoginError (LoginFragment loginFragment, Throwable t)
  {
    Log.e (TAG, t.getLocalizedMessage (), t);
  }
}

