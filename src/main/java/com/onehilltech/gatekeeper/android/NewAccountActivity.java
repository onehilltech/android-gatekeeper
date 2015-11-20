package com.onehilltech.gatekeeper.android;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.VolleyError;

import java.lang.reflect.InvocationTargetException;
import java.util.List;


/**
 * A login screen that offers login via email/password.
 */
public class NewAccountActivity extends AppCompatActivity
    implements GatekeeperClient.OnInitialized
{
  private static final String TAG = "NewAccountActivity";

  public static Intent newIntent (Context context)
  {
    return new Intent (context, NewAccountActivity.class);
  }

  // UI references.
  private EditText usernameView_;
  private EditText confirmPasswordView_;
  private AutoCompleteTextView emailView_;
  private EditText passwordView_;
  private View progressView_;
  private View newAccountFormView_;

  /// Reference to the initialized Kick client.
  private GatekeeperClient gatekeeperClient_;

  /// The request for creating a new account.
  private Request createAccountRequest_;

  @Override
  protected void onCreate (Bundle savedInstanceState)
  {
    super.onCreate (savedInstanceState);
    this.setContentView (R.layout.activity_new_account);

    // Setup the account creation form.
    this.usernameView_ = (EditText)this.findViewById (R.id.username);
    this.emailView_ = (AutoCompleteTextView)this.findViewById (R.id.email);

    this.passwordView_ = (EditText)this.findViewById (R.id.password);
    this.confirmPasswordView_ = (EditText)this.findViewById (R.id.confirm_password);

    Button createAccountButton = (Button)this.findViewById (R.id.create_account_button);
    createAccountButton.setOnClickListener (new OnClickListener ()
    {
      @Override
      public void onClick (View view)
      {
        attemptCreateAccount ();
      }
    });

    this.newAccountFormView_ = findViewById (R.id.new_account_form);
    this.progressView_ = findViewById (R.id.creation_progress);

    // Initialize the KickClient.
    this.showProgress (true);

    try
    {
      GatekeeperClient.initialize (this, this);
    }
    catch (PackageManager.NameNotFoundException | IllegalAccessException | ClassNotFoundException | InvocationTargetException e)
    {
      throw new RuntimeException ("Failed to initialize Gatekeeper client", e);
    }
  }

  /**
   * Attempts to sign in or register the account specified by the login form.
   * If there are form errors (invalid email, missing fields, etc.), the
   * errors are presented and no actual login attempt is made.
   */
  private void attemptCreateAccount ()
  {
    if (this.createAccountRequest_ != null)
      return;

    // Reset errors.
    this.usernameView_.setError (null);
    this.emailView_.setError (null);
    this.passwordView_.setError (null);
    this.confirmPasswordView_.setError (null);

    // Store values at the time of the login attempt.
    String username = this.usernameView_.getText ().toString ();
    String password = this.passwordView_.getText ().toString ();
    String confirmPassword = this.confirmPasswordView_.getText ().toString ();
    String email = this.emailView_.getText ().toString ();

    InputError inputError = new InputError ();

    // Make sure the username is not empty, and is valid.
    if (TextUtils.isEmpty (username))
      inputError.addError (this.usernameView_, this.getString (R.string.error_field_required));

    // Check for a valid password, if the user entered one.
    if (!this.isPasswordValid (password))
      inputError.addError (this.passwordView_, this.getString (R.string.error_invalid_password));

    // Make sure both passwords match.
    if (!confirmPassword.equals (password))
      inputError.addError (this.confirmPasswordView_, this.getString (R.string.error_password_not_match));

    // Check for a valid email address.
    if (TextUtils.isEmpty (email))
      inputError.addError (this.emailView_, this.getString (R.string.error_field_required));
    else if (!this.isEmailValid (email))
      inputError.addError (this.emailView_, this.getString (R.string.error_invalid_email));

    if (!inputError.hasError ())
    {
      // Show the progress bar, and create the new account.
      this.showProgress (true);

      this.createAccountRequest_ =
          this.gatekeeperClient_.createAccount (
              username,
              password,
              email,
              new GatekeeperClient.OnResultListener<Boolean> ()
              {
                @Override
                public void onResult (Boolean result)
                {
                  if (result)
                    finish ();
                  else
                    Toast.makeText (NewAccountActivity.this, "Failed to create account", Toast.LENGTH_SHORT).show ();

                  endCreateAccount ();
                }

                @Override
                public void onError (VolleyError error)
                {
                  Toast.makeText (NewAccountActivity.this, "Failed to create account", Toast.LENGTH_SHORT).show ();
                  Log.e (TAG, error.getMessage (), error);

                  endCreateAccount ();
                }
              });
    }
    else
    {
      inputError.requestFocus ();
    }
  }

  /**
   * End the account creation activity.
   */
  private void endCreateAccount ()
  {
    this.showProgress (false);
    this.createAccountRequest_ = null;

    // TODO Automatically login the user after creating the account.
    this.finish ();
  }

  private boolean isEmailValid (String email)
  {
    return email.contains ("@");
  }

  private boolean isPasswordValid (String password)
  {
    return password.length () > 4;
  }

  /**
   * Shows the progress UI and hides the login form.
   */
  @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
  private void showProgress (final boolean show)
  {
    // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
    // for very easy animations. If available, use these APIs to fade-in
    // the progress spinner.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2)
    {
      int shortAnimTime = getResources ().getInteger (android.R.integer.config_shortAnimTime);

      this.newAccountFormView_.setVisibility (show ? View.GONE : View.VISIBLE);
      this.newAccountFormView_.animate ().setDuration (shortAnimTime).alpha (
          show ? 0 : 1).setListener (new AnimatorListenerAdapter ()
      {
        @Override
        public void onAnimationEnd (Animator animation)
        {
          newAccountFormView_.setVisibility (show ? View.GONE : View.VISIBLE);
        }
      });

      this.progressView_.setVisibility (show ? View.VISIBLE : View.GONE);
      this.progressView_.animate ().setDuration (shortAnimTime).alpha (
          show ? 1 : 0).setListener (new AnimatorListenerAdapter ()
      {
        @Override
        public void onAnimationEnd (Animator animation)
        {
          progressView_.setVisibility (show ? View.VISIBLE : View.GONE);
        }
      });
    }
    else
    {
      // The ViewPropertyAnimator APIs are not available, so simply show
      // and hide the relevant UI components.
      this.progressView_.setVisibility (show ? View.VISIBLE : View.GONE);
      this.newAccountFormView_.setVisibility (show ? View.GONE : View.VISIBLE);
    }
  }

  @Override
  public void onInitialized (GatekeeperClient client)
  {
    Log.d (TAG, "Gatekeeper client initialized");
    this.gatekeeperClient_ = client;

    // Enable the new account form.
    this.showProgress (false);
  }

  @Override
  public void onError (VolleyError error)
  {
    Log.e (TAG, "Failed to initialize Gatekeeper client");
    Log.e (TAG, error.getMessage (), error);

    this.showProgress (false);
  }

  private interface ProfileQuery
  {
    String[] PROJECTION = {
        ContactsContract.CommonDataKinds.Email.ADDRESS,
        ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
    };

    int ADDRESS = 0;
    int IS_PRIMARY = 1;
  }

  private void addEmailsToAutoComplete (List<String> emailAddressCollection)
  {
    //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
    ArrayAdapter<String> adapter =
        new ArrayAdapter<> (
            this,
            android.R.layout.simple_dropdown_item_1line,
            emailAddressCollection);

    this.emailView_.setAdapter (adapter);
  }
}

