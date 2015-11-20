package com.onehilltech.gatekeeper.android;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;


/**
 * A login screen that offers login via email/password.
 */
public class NewAccountActivity extends AppCompatActivity
    implements NewAccountFragment.OnFragmentInteractionListener
{
  private static final String TAG = "NewAccountActivity";

  public static Intent newIntent (Context context)
  {
    return new Intent (context, NewAccountActivity.class);
  }

  @Override
  public void onAccountCreated (NewAccountFragment fragment)
  {
    // Finish the activity and go back. Or, we could automatically login the user
    // to their account by passing their username/password back to the login activity.
    this.finish ();
  }

  @Override
  public void onError (NewAccountFragment fragment, Throwable t)
  {
    Log.e (TAG, t.getLocalizedMessage (), t);
  }

  @Override
  protected void onCreate (Bundle savedInstanceState)
  {
    super.onCreate (savedInstanceState);
    this.setContentView (R.layout.activity_new_account);
  }
}

