package com.onehilltech.gatekeeper.android;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;


/**
 * Activity for using Gatekeeper to login
 */
public class LoginActivity extends AppCompatActivity
    implements LoginFragment.OnLoginFragmentListener
{
  private static final String TAG = "LoginActivity";

  @Override
  protected void onCreate (Bundle savedInstanceState)
  {
    super.onCreate (savedInstanceState);
    this.setContentView (R.layout.activity_login);
  }

  @Override
  public void onLoginComplete (LoginFragment loginFragment)
  {
    this.finish ();
  }

  @Override
  public void onCreateNewAccount (LoginFragment fragment)
  {
    // TODO Start the NewAccountActivity.
  }

  @Override
  public void onLoginError (LoginFragment loginFragment, Throwable t)
  {
    Log.e (TAG, t.getLocalizedMessage (), t);
  }
}

