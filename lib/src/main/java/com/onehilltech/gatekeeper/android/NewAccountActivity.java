package com.onehilltech.gatekeeper.android;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.onehilltech.gatekeeper.android.http.JsonAccount;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Activity that allows the user to create a new account.
 */
public class NewAccountActivity extends AppCompatActivity
    implements NewAccountFragment.Listener
{
  private final Logger logger_ = LoggerFactory.getLogger (NewAccountActivity.class);

  private static final String EXTRA_UP_INTENT = "upIntent";

  /**
   * Create a new Intent object for starting this activity.
   *
   * @param context       Target context
   * @return              Intent object
   */
  public static Intent newIntent (Context context, Intent upIntent)
  {
    Intent intent = new Intent (context, NewAccountActivity.class);
    intent.putExtra (EXTRA_UP_INTENT, upIntent);

    return intent;
  }

  @Override
  protected void onCreate (Bundle savedInstanceState)
  {
    super.onCreate (savedInstanceState);

    this.setContentView (R.layout.activity_new_account);

    // Show the fragment for creating the new account.
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

  @Override
  public void onAccountCreated (NewAccountFragment fragment, JsonAccount account)
  {
    Intent upIntent = this.getIntent ().getParcelableExtra (EXTRA_UP_INTENT);

    if (upIntent != null)
    {
      upIntent.putExtra (MessageConstants.ARG_USERNAME, fragment.getUsername ());
      upIntent.putExtra (MessageConstants.ARG_PASSWORD, fragment.getPassword ());

      int flags = upIntent.getFlags ();
      upIntent.setFlags (flags | Intent.FLAG_ACTIVITY_CLEAR_TOP);

      this.startActivity (upIntent);
    }

    this.finish ();
  }

  @Override
  public void onError (NewAccountFragment fragment, Throwable t)
  {
    this.logger_.error (t.getLocalizedMessage (), t);
  }
}

