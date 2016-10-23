package com.onehilltech.gatekeeper.android;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.onehilltech.metadata.ManifestMetadata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @class SingleUserLoginActivity
 *
 * Simple activity that displays the login fragment.
 */
public class SingleUserLoginActivity extends AppCompatActivity
    implements SimpleSingleUserLoginFragment.LoginFragmentListener
{
  private final LoginMetadata metadata_ = new LoginMetadata ();
  private final Logger logger_ = LoggerFactory.getLogger (SingleUserLoginActivity.class);

  public static final String ARG_ON_LOGIN_COMPLETE_INTENT = "onLoginCompleteIntent";

  public static Intent newIntent (Context context, Intent loginComplete)
  {
    Intent intent = new Intent (context, SingleUserLoginActivity.class);
    intent.putExtra (ARG_ON_LOGIN_COMPLETE_INTENT, loginComplete);

    return intent;
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

      Bundle extras = this.getIntent ().getExtras ();
      String username = extras.getString (MessageConstants.ARG_USERNAME);
      String password = extras.getString (MessageConstants.ARG_PASSWORD);

      SingleUserLoginFragment loginFragment =
          username != null && password != null ?
              this.onCreateFragment (username, password) :
              this.onCreateFragment ();

      // If we're being restored from a previous state, then we don't need to do
      // anything and should return or else we could end up with overlapping fragments.

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
   * Create the SingleUserLoginFragment for the activity.
   *
   * @return
   */
  protected SingleUserLoginFragment onCreateFragment ()
  {
    return new SimpleSingleUserLoginFragment ();
  }

  /**
   * Create the SingleUserLoginFragment initialized with the username and password.
   *
   * @param username
   * @param password
   * @return
   */
  protected SingleUserLoginFragment onCreateFragment (String username, String password)
  {
    return SimpleSingleUserLoginFragment.newInstance (username, password);
  }

  @Override
  public void onLoginComplete (SingleUserLoginFragment loginFragment)
  {
    Intent targetIntent = null;

    if (this.getIntent ().hasExtra (ARG_ON_LOGIN_COMPLETE_INTENT))
      targetIntent = this.getIntent ().getParcelableExtra (ARG_ON_LOGIN_COMPLETE_INTENT);
    else if (this.metadata_.loginSuccessRedirectActivity != null)
      targetIntent = this.metadata_.getLoginSuccessRedirectIntent (this);

    int flags = targetIntent.getFlags ();
    targetIntent.setFlags (flags | Intent.FLAG_ACTIVITY_CLEAR_TOP);

    this.startActivity (targetIntent);
    this.finish ();
  }

  @Override
  public void onLoginError (SingleUserLoginFragment loginFragment, Throwable t)
  {
    this.logger_.error (t.getLocalizedMessage (), t);
  }
}

