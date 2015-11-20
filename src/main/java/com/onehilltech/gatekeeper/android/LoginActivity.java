package com.onehilltech.gatekeeper.android;

import android.content.Intent;
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

  /// Intent to start when login is complete.
  private Intent onLoginCompleteIntent_;

  @Override
  protected void onCreate (Bundle savedInstanceState)
  {
    super.onCreate (savedInstanceState);
    this.setContentView (R.layout.activity_login);
  }

  /**
   * Set the intent to start when the user is successfully authenticated within
   * the application.
   *
   * @param intent
   */
  public void setOnLoginCompleteIntent (Intent intent)
  {
    this.onLoginCompleteIntent_ = intent;
  }

  @Override
  public void onLoginComplete (LoginFragment loginFragment)
  {
    if (this.onLoginCompleteIntent_ != null)
      this.startActivity (this.onLoginCompleteIntent_);

    // Finish the activity.
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

