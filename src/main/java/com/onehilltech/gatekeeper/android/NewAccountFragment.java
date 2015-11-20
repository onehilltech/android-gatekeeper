package com.onehilltech.gatekeeper.android;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;

import com.android.volley.VolleyError;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link NewAccountFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link NewAccountFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class NewAccountFragment extends Fragment
  implements GatekeeperClient.OnInitialized
{
  private static final String TAG = "NewAccountFragment";

  private OnFragmentInteractionListener listner_;

  private GatekeeperClient gatekeeperClient_;

  // UI references.
  private EditText usernameView_;
  private EditText confirmPasswordView_;
  private AutoCompleteTextView emailView_;
  private EditText passwordView_;
  private View progressView_;
  private View newAccountFormView_;

  private JsonRequest newAccountRequest_;

  public static NewAccountFragment newInstance ()
  {
    NewAccountFragment fragment = new NewAccountFragment ();

    return fragment;
  }

  public NewAccountFragment ()
  {
    // Required empty public constructor
  }

  @Override
  public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
  {
    return inflater.inflate (R.layout.fragment_new_account, container, false);
  }

  @Override
  public void onViewCreated (View view, Bundle savedInstanceState)
  {
    super.onViewCreated (view, savedInstanceState);

    // Setup the account creation form.
    this.usernameView_ = (EditText)view.findViewById (R.id.username);
    this.emailView_ = (AutoCompleteTextView)view.findViewById (R.id.email);

    this.passwordView_ = (EditText)view.findViewById (R.id.password);
    this.confirmPasswordView_ = (EditText)view.findViewById (R.id.confirm_password);

    this.newAccountFormView_ = view.findViewById (R.id.new_account_form);
    this.progressView_ = view.findViewById (R.id.creation_progress);

    Button createAccountButton = (Button)view.findViewById (R.id.create_account_button);
    createAccountButton.setOnClickListener (new View.OnClickListener ()
    {
      @Override
      public void onClick (View view)
      {
        startCreateAccount ();
      }
    });
  }

  @Override
  public void onAttach (Activity activity)
  {
    super.onAttach (activity);

    try
    {
      this.listner_ = (OnFragmentInteractionListener) activity;
    }
    catch (ClassCastException e)
    {
      throw new ClassCastException (activity.toString () + " must implement OnFragmentInteractionListener");
    }
  }

  @Override
  public void onDetach ()
  {
    super.onDetach ();
    this.listner_ = null;
  }

  @Override
  public void onInitialized (GatekeeperClient client)
  {
    this.gatekeeperClient_ = client;
  }

  @Override
  public void onError (VolleyError error)
  {
    if (this.listner_ != null)
      this.listner_.onError (this, error);
  }

  /**
   * Attempts to sign in or register the account specified by the login form.
   * If there are form errors (invalid email, missing fields, etc.), the
   * errors are presented and no actual login attempt is made.
   */
  private void startCreateAccount ()
  {
    if (this.newAccountRequest_ != null)
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
    if (TextUtils.isEmpty (password))
      inputError.addError (this.passwordView_, this.getString (R.string.error_invalid_password));
    else if (!confirmPassword.equals (password))
      inputError.addError (this.confirmPasswordView_, this.getString (R.string.error_password_not_match));

    // Check for a valid email address.
    if (TextUtils.isEmpty (email))
      inputError.addError (this.emailView_, this.getString (R.string.error_field_required));

    if (!inputError.hasError ())
    {
      // Show the progress bar, and create the new account.
      this.showProgress (true);

      this.newAccountRequest_ =
          this.gatekeeperClient_.createAccount (
              username,
              password,
              email,
              new GatekeeperClient.OnResultListener<Boolean> () {
                @Override
                public void onResult (Boolean result)
                {
                  finishRequest (result);
                }

                @Override
                public void onError (VolleyError error)
                {
                  if (listner_ != null)
                    listner_.onError (NewAccountFragment.this, error);

                  finishRequest (false);
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
  private void finishRequest (boolean isComplete)
  {
    this.showProgress (false);
    this.newAccountRequest_ = null;

    if (this.listner_ != null && isComplete)
      this.listner_.onAccountCreated (this);
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

  /**
   * This interface must be implemented by activities that contain this
   * fragment to allow an interaction in this fragment to be communicated
   * to the activity and potentially other fragments contained in that
   * activity.
   * <p>
   * See the Android Training lesson <a href=
   * "http://developer.android.com/training/basics/fragments/communicating.html"
   * >Communicating with Other Fragments</a> for more information.
   */
  public interface OnFragmentInteractionListener
  {
    void onAccountCreated (NewAccountFragment fragment);
    void onError (NewAccountFragment fragment, Throwable t);
  }
}
