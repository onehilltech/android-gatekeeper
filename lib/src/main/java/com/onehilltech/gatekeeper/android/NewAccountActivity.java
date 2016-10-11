package com.onehilltech.gatekeeper.android;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.onehilltech.gatekeeper.android.http.JsonAccount;

/**
 * Activity that allows the user to create a new account.
 */
public class NewAccountActivity extends AppCompatActivity
    implements NewAccountFragment.Listener
{
  public static final String RESULT_DATA_USERNAME = "username";

  public static final String RESULT_DATA_PASSWORD = "password";

  private static final String TAG = "NewAccountActivity";

  /**
   * Factory method for the activity intents.
   *
   * @param context
   * @return
   */
  public static Intent newIntent (Context context)
  {
    return new Intent (context, NewAccountActivity.class);
  }

  @Override
  public void onAccountCreated (NewAccountFragment fragment, JsonAccount account)
  {
    // Finish the activity and go back. Or, we could automatically login the user
    // to their account by passing their username/password back to the login activity.
    Intent intent = new Intent ();
    intent.putExtra (RESULT_DATA_USERNAME, fragment.getUsername ());
    intent.putExtra (RESULT_DATA_PASSWORD, fragment.getPassword ());
    this.setResult (RESULT_OK, intent);

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

    NewAccountFragment fragment = this.onCreateFragment ();

    this.getSupportFragmentManager ()
        .beginTransaction ()
        .replace (R.id.container, fragment)
        .commit ();
  }

  protected NewAccountFragment onCreateFragment ()
  {
    return new NewAccountFragment ();
  }
}

