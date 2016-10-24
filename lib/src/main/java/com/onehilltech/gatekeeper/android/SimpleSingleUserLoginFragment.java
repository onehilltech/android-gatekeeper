package com.onehilltech.gatekeeper.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.onehilltech.gatekeeper.android.utils.InputError;

/**
 * @class SimpleSingleUserLoginFragment
 *
 * Simple implementation of the SingleUserLoginFragment that displays a view for
 * entering the username and password. The view also has a button for logging
 * in and creating a new account.
 */
public class SimpleSingleUserLoginFragment extends SingleUserLoginFragment
{
  private static final String TAG = "SimpleSingleUserLoginFragment";

  private static final String ARG_USERNAME = "username";

  private static final String ARG_PASSWORD = "password";

  private TextView username_;

  private TextView password_;

  private TextView errorMessage_;

  /**
   * Create a new instance of the fragment.
   *
   * @return  SimpleSingleUserLoginFragment object
   */
  public static SimpleSingleUserLoginFragment newInstance ()
  {
    return new SimpleSingleUserLoginFragment ();
  }

  /**
   * Create an instance of the SimpleSingleUserLoginFragment with the username initialized.
   *
   * @param username
   * @return
   */
  @SuppressWarnings ("unused")
  public static SimpleSingleUserLoginFragment newInstance (String username)
  {
    SimpleSingleUserLoginFragment fragment = new SimpleSingleUserLoginFragment ();

    Bundle args = new Bundle ();
    args.putString (ARG_USERNAME, username);

    fragment.setArguments (args);

    return fragment;
  }

  /**
   * Create an instance of the SimpleSingleUserLoginFragment with the username/password initialized.
   *
   * @param username
   * @param password
   * @return
   */
  public static SimpleSingleUserLoginFragment newInstance (String username, String password)
  {
    SimpleSingleUserLoginFragment fragment = new SimpleSingleUserLoginFragment ();

    Bundle args = new Bundle ();
    args.putString (ARG_USERNAME, username);
    args.putString (ARG_PASSWORD, password);

    fragment.setArguments (args);

    return fragment;
  }

  public SimpleSingleUserLoginFragment ()
  {
    // Required empty public constructor
  }

  @Override
  public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
  {
    View view = inflater.inflate (R.layout.fragment_login, container, false);

    // Setup the UI controls.
    this.username_ = (TextView)view.findViewById (R.id.username);
    this.password_ = (TextView)view.findViewById (R.id.password);

    Button signInButton = (Button)view.findViewById (R.id.button_sign_in);
    signInButton.setOnClickListener (new View.OnClickListener ()
    {
      @Override
      public void onClick (View v)
      {
        performSignIn ();
      }
    });

    View actionCreateNewAccount = view.findViewById (R.id.action_create_account);
    actionCreateNewAccount.setOnClickListener (new View.OnClickListener ()
    {
      @Override
      public void onClick (View v)
      {
        // Start the activity for creating a new account.
        Activity activity = getActivity ();

        Intent upIntent = activity.getIntent ();
        Intent intent = NewAccountActivity.newIntent (getContext (), upIntent);

        // Start the activity for creating the account, and finish this activity.
        activity.startActivity (intent);
        activity.finish ();
      }
    });

    this.errorMessage_ = (TextView)view.findViewById (R.id.error_message);

    // Initialize the view with data.
    Bundle args = this.getArguments ();

    if (args != null)
    {
      if (args.containsKey (ARG_USERNAME))
        this.username_.setText (args.getString (ARG_USERNAME));

      if (args.containsKey (ARG_PASSWORD))
        this.password_.setText (args.getString (ARG_PASSWORD));
    }

    return view;
  }

  /**
   * Show an error message. This causes all views to be hidden except for the logo and
   * the error message. When this happens, the user must exit the parent activity.
   *
   * @param errMsg
   */
  private void showErrorMessage (String errMsg)
  {
    if (this.errorMessage_.getVisibility () != View.VISIBLE)
      this.errorMessage_.setVisibility (View.VISIBLE);

    this.errorMessage_.setText (errMsg);
  }

  /**
   * Perform the signin process with the Gatekeeper client.
   */
  private void performSignIn ()
  {
    String username = this.username_.getText ().toString ();
    String password = this.password_.getText ().toString ();

    // Make sure the username and password are not empty. If either is empty, then we
    // need to discontinue the sign in process, and display an error message to the
    // user.

    InputError inputError = new InputError ();

    if (TextUtils.isEmpty (username))
      inputError.addError (this.username_, this.getString (R.string.error_field_required));

    if (TextUtils.isEmpty (password))
      inputError.addError (this.password_, this.getString (R.string.error_field_required));

    if (!inputError.hasError ())
      this.login (username, password);
  }
}
