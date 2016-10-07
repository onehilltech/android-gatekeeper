package com.onehilltech.gatekeeper.android;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.onehilltech.metadata.ManifestMetadata;

/**
 * @class LoginActivity
 *
 * Simple activity that displays the login fragment.
 */
public class LoginActivity extends AppCompatActivity
    implements SimpleLoginFragment.LoginFragmentListener
{
  private static final String TAG = "LoginActivity";

  private static final int REQUEST_USER_CREDENTIALS = 9000;

  private final LoginMetadata metadata_ = new LoginMetadata ();

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

        onAccountCreated (username, password);
      }
    }
  }

  /**
   * Handle account creation.
   *
   * @param username
   * @param password
   */
  private void onAccountCreated (String username, String password)
  {
    LoginFragment loginFragment = this.onCreateFragment (username, password);

    this.getSupportFragmentManager ()
        .beginTransaction ()
        .replace (R.id.fragment_container, loginFragment)
        .commitAllowingStateLoss ();
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

  /**
   * Create the LoginFragment for the activity.
   *
   * @return
   */
  protected LoginFragment onCreateFragment ()
  {
    return new SimpleLoginFragment ();
  }

  /**
   * Create the LoginFragment initialized with the username and password.
   *
   * @param username
   * @param password
   * @return
   */
  protected LoginFragment onCreateFragment (String username, String password)
  {
    return SimpleLoginFragment.newInstance (username, password);
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
    if (!this.metadata_.hasNewAccountActivity ())
      return;

    Intent intent = this.metadata_.getNewAccountActivity (this);
    this.startActivityForResult (intent, REQUEST_USER_CREDENTIALS);
  }

  @Override
  public void onLoginError (LoginFragment loginFragment, Throwable t)
  {
    Log.e (TAG, t.getLocalizedMessage (), t);
  }
}

