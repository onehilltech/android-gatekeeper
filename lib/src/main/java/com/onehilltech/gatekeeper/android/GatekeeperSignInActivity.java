package com.onehilltech.gatekeeper.android;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.onehilltech.metadata.ManifestMetadata;

public class GatekeeperSignInActivity extends AppCompatActivity
    implements GatekeeperSignInFragment.LoginFragmentListener
{
  public static class Builder
  {
    private Intent intent_;

    public Builder (Context context)
    {
      this (context, GatekeeperSignInActivity.class);
    }

    public <T extends GatekeeperSignInActivity> Builder (Context context, Class <T> clazz)
    {
      this.intent_ = new Intent (context, clazz);
    }

    public Builder setRedirectTo (Intent intent)
    {
      this.intent_.putExtra (ARG_REDIRECT_INTENT, intent);
      return this;
    }

    public Builder setErrorMessage (String errorMessage)
    {
      this.intent_.putExtra (ARG_ERROR_MESSAGE, errorMessage);
      return this;
    }

    public Intent build ()
    {
      return this.intent_;
    }
  }

  private final GatekeeperMetadata metadata_ = new GatekeeperMetadata ();
  
  public static final String ARG_REDIRECT_INTENT = "redirect_intent";

  public static final String ARG_ERROR_MESSAGE = "error_message";

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
      this.getFragmentManager ()
          .beginTransaction ()
          .replace (R.id.container, this.onCreateFragment (savedInstanceState))
          .commit ();
    }
    catch (Exception e)
    {
      throw new IllegalStateException ("Failed to create activity", e);
    }
  }

  /**
   * Get the fragment to display in the signIn activity. Subclasses can customize
   * the look and feel of the signIn by overriding this method.
   */
  protected GatekeeperSignInFragment onCreateFragment (Bundle savedInstanceState)
  {
    GatekeeperSignInFragment.Builder builder =
        new GatekeeperSignInFragment.Builder ()
            .setTitle (this.getApplicationName ())
            .setCreateAccountIntent (GatekeeperCreateAccountActivity.newIntent (this, this.getIntent ()));

    Bundle extras = this.getIntent ().getExtras ();

    if (savedInstanceState == null && extras.containsKey (ARG_ERROR_MESSAGE))
      builder.setErrorMessage (extras.getString (ARG_ERROR_MESSAGE));

    return builder.build ();
  }

  protected String getApplicationName ()
  {
    return this.getApplicationInfo ().loadLabel (this.getPackageManager ()).toString ();
  }

  @Override
  public void onSignInComplete (GatekeeperSignInFragment loginFragment)
  {
    Intent targetIntent = null;

    if (this.getIntent ().hasExtra (ARG_REDIRECT_INTENT))
      targetIntent = this.getIntent ().getParcelableExtra (ARG_REDIRECT_INTENT);
    else if (this.metadata_.loginSuccessRedirectActivity != null)
      targetIntent = this.metadata_.getLoginSuccessRedirectIntent (this);

    if (targetIntent != null)
    {
      int flags = targetIntent.getFlags ();
      targetIntent.setFlags (flags | Intent.FLAG_ACTIVITY_CLEAR_TOP);

      this.startActivity (targetIntent);
    }

    this.finish ();
  }
}

