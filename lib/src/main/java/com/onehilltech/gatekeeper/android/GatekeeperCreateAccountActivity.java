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
public class GatekeeperCreateAccountActivity extends AppCompatActivity
    implements GatekeeperCreateAccountFragment.Listener
{
  private final Logger logger_ = LoggerFactory.getLogger (GatekeeperCreateAccountActivity.class);

  public static final String EXTRA_UP_INTENT = "up_intent";
  public static final String EXTRA_REDIRECT_INTENT = "redirect_intent";

  /**
   * Create a new Intent object for starting this activity.
   *
   * @param context       Target context
   * @return              Intent object
   */
  public static Intent newIntent (Context context, Intent upIntent)
  {
    Intent intent = new Intent (context, GatekeeperCreateAccountActivity.class);
    intent.putExtra (EXTRA_UP_INTENT, upIntent);

    return intent;
  }

  @Override
  protected void onCreate (Bundle savedInstanceState)
  {
    super.onCreate (savedInstanceState);

    this.setContentView (R.layout.activity_new_account);

    // Show the fragment for creating the new account.
    GatekeeperCreateAccountFragment fragment = this.onCreateFragment ();

    this.getSupportFragmentManager ()
        .beginTransaction ()
        .replace (R.id.container, fragment)
        .commit ();
  }

  protected GatekeeperCreateAccountFragment onCreateFragment ()
  {
    return new GatekeeperCreateAccountFragment.Builder ()
        .setTitle (this.getApplicationName ())
        .build ();
  }

  protected String getApplicationName ()
  {
    return this.getApplicationInfo ().loadLabel (this.getPackageManager ()).toString ();
  }

  @Override
  public void onAccountCreated (GatekeeperCreateAccountFragment fragment, JsonAccount account)
  {
    Intent redirectIntent = this.getIntent ().getParcelableExtra (EXTRA_REDIRECT_INTENT);

    if (redirectIntent != null)
    {
      // The account has been created. Now, we can redirect to the activity
      // that we originally started.
      int flags = redirectIntent.getFlags ();
      redirectIntent.setFlags (flags | Intent.FLAG_ACTIVITY_CLEAR_TOP);

      this.startActivity (redirectIntent);
    }

    this.finish ();
  }

  @Override
  public void onError (GatekeeperCreateAccountFragment fragment, Throwable t)
  {
    this.logger_.error (t.getLocalizedMessage (), t);
  }
}

