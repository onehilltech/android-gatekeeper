package com.onehilltech.gatekeeper.android;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

/**
 * Activity for using Gatekeeper to login
 */
public class LoginActivity extends AppCompatActivity
    implements LoginFragment.OnFragmentInteractionListener
{
  private static final String TAG = "LoginActivity";

  private static final int REQUEST_USER_CREDENTIALS = 9000;

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
    this.setContentView (R.layout.activity_login);

    // Check that the activity is using the layout version with
    // the fragment_container FrameLayout
    if (this.findViewById(R.id.fragment_container) != null)
    {
      // However, if we're being restored from a previous state, then we don't need to do
      // anything and should return or else we could end up with overlapping fragments.
      if (savedInstanceState != null)
        return;

      // Create a new Fragment to be placed in the activity layout
      LoginFragment loginFragment = LoginFragment.newInstance ();
      this.getSupportFragmentManager ()
          .beginTransaction ()
          .add (R.id.fragment_container, loginFragment)
          .commit();
    }
  }

  @Override
  public void onLoginComplete (LoginFragment loginFragment)
  {
    this.finish ();
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

