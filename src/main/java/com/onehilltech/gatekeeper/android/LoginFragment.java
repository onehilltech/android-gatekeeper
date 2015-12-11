package com.onehilltech.gatekeeper.android;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.onehilltech.gatekeeper.android.data.UserToken;
import com.onehilltech.gatekeeper.android.utils.InputError;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link LoginFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LoginFragment extends Fragment
  implements SingleUserSessionClient.OnInitializedListener
{
  private static final String TAG = "LoginFragment";

  private static final String ARG_USERNAME = "username";
  private static final String ARG_PASSWORD = "password";
  private static final String ARG_AUTO_LOGIN = "auto_login";

  private OnFragmentInteractionListener onLoginFragmentListener_;

  private TextView usernameView_;
  private TextView passwordView_;

  private SingleUserSessionClient userSessionClient_;

  private JsonRequest loginRequest_;

  private View loginForm_;
  private View progressView_;
  private TextView progressTextView_;
  private TextView errorMessageView_;

  private boolean autoLogin_ = false;

  /**
   * Create a new instance of the fragment.
   *
   * @return  LoginFragment object
   */
  public static LoginFragment newInstance ()
  {
    LoginFragment fragment = new LoginFragment ();

    return fragment;
  }

  /**
   * Create an instance of the LoginFragment with the username initialized.
   *
   * @param username
   * @return
   */
  public static LoginFragment newInstance (String username)
  {
    LoginFragment fragment = new LoginFragment ();

    Bundle args = new Bundle ();
    args.putString (ARG_USERNAME, username);

    fragment.setArguments (args);

    return fragment;
  }


  /**
   * Create an instance of the LoginFragment with the username/password initialized.
   *
   * @param username
   * @param password
   * @return
   */
  public static LoginFragment newInstance (String username, String password)
  {
    LoginFragment fragment = new LoginFragment ();

    Bundle args = new Bundle ();
    args.putString (ARG_USERNAME, username);
    args.putString (ARG_PASSWORD, password);

    fragment.setArguments (args);

    return fragment;
  }

  public LoginFragment ()
  {
    // Required empty public constructor
  }

  @Override
  public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
  {
    return inflater.inflate (R.layout.fragment_login, container, false);
  }

  @Override
  public void onViewCreated (View view, Bundle savedInstanceState)
  {
    super.onViewCreated (view, savedInstanceState);

    // Setup the UI controls.
    this.usernameView_ = (TextView)view.findViewById (R.id.username);
    this.passwordView_ = (TextView)view.findViewById (R.id.password);

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
        onLoginFragmentListener_.onCreateNewAccount (LoginFragment.this);
      }
    });

    this.loginForm_ = view.findViewById (R.id.login_form);
    this.progressView_ = view.findViewById (R.id.progress);
    this.progressTextView_ = (TextView)this.progressView_.findViewById (R.id.progress_text);
    this.errorMessageView_ = (TextView)view.findViewById (R.id.error_message);

    // Load the application icon from the package information.
    ImageView iconView = (ImageView)view.findViewById (R.id.app_icon);

    try
    {
      PackageManager pm = this.getActivity ().getPackageManager ();
      Drawable appIcon = pm.getApplicationIcon (this.getActivity ().getPackageName ());

      iconView.setImageDrawable (appIcon);
    }
    catch (PackageManager.NameNotFoundException e)
    {
      // Hide the image view since we do not have an icon to load. We should really
      // set it with the default Gatekeeper icon.
      iconView.setVisibility (View.GONE);
    }

    // Initialize the view with data.
    Bundle args = this.getArguments ();

    if (args != null)
    {
      if (args.containsKey (ARG_USERNAME))
        this.usernameView_.setText (args.getString (ARG_USERNAME));

      if (args.containsKey (ARG_PASSWORD))
        this.passwordView_.setText (args.getString (ARG_PASSWORD));

      if (args.containsKey (ARG_AUTO_LOGIN))
        this.autoLogin_ = args.getBoolean (ARG_AUTO_LOGIN);
    }

    try
    {
      this.showProgress (true, this.getString (R.string.progress_initializing_app));
      SingleUserSessionClient.initialize (this.getActivity (), this);
    }
    catch (Exception e)
    {
      throw new RuntimeException (this.getString (R.string.error_failed_to_initialize_client), e);
    }
  }

  @Override
  public void onAttach (Activity activity)
  {
    super.onAttach (activity);

    try
    {
      this.onLoginFragmentListener_ = (OnFragmentInteractionListener) activity;
    }
    catch (ClassCastException e)
    {
      throw new ClassCastException (activity + " must implement OnFragmentInteractionListener");
    }
  }

  @Override
  public void onDetach ()
  {
    super.onDetach ();
    this.onLoginFragmentListener_ = null;
  }

  @Override
  public void onInitialized (SingleUserSessionClient client)
  {
    // Store our reference to the client, and check if the client is logged in.
    // This way, we can short circuit the login process.
    this.userSessionClient_ = client;

    if (client.isLoggedIn ())
      this.onLoginFragmentListener_.onLoginComplete (this);

    // In case the activity does not finish, we still need to hide the progress
    // and show the login form.
    if (this.autoLogin_)
      this.performSignIn ();
    else
      this.hideProgress ();
  }

  @Override
  public void onError (Throwable t)
  {
    Log.e (TAG, t.getMessage (), t);

    // Hide the progress, and make sure the login form is not shown since we could
    // not initialize the client.
    this.hideProgress ();
    this.showLoginForm (false);

    // Now, show the error message to the client.
    this.showErrorMessage (this.getString (R.string.error_failed_to_initialize_client));

    if (this.onLoginFragmentListener_ != null)
      this.onLoginFragmentListener_.onLoginError (this, t);
  }

  /**
   * Show an error message. This causes all views to be hidden except for the logo and
   * the error message. When this happens, the user must exit the parent activity.
   *
   * @param errMsg
   */
  private void showErrorMessage (String errMsg)
  {
    this.errorMessageView_.setVisibility (View.VISIBLE);
    this.errorMessageView_.setText (errMsg);
  }

  /**
   * Hide the error message shown to the user.
   */
  private void hideErrorMessage ()
  {
    this.errorMessageView_.setVisibility (View.GONE);
  }

  /**
   * Perform the signin process with the Gatekeeper client.
   */
  private void performSignIn ()
  {
    if (this.loginRequest_ != null)
      return;

    String username = this.usernameView_.getText ().toString ();
    String password = this.passwordView_.getText ().toString ();

    // Make sure the username and password are not empty. If either is empty, then we
    // need to discontinue the sign in process, and display an error message to the
    // user.

    InputError inputError = new InputError ();

    if (TextUtils.isEmpty (username))
      inputError.addError (this.usernameView_, this.getString (R.string.error_field_required));

    if (TextUtils.isEmpty (password))
      inputError.addError (this.passwordView_, this.getString (R.string.error_field_required));

    if (!inputError.hasError ())
    {
      this.loginRequest_ =
          this.userSessionClient_.loginUser (
              username,
              password,
              new ResponseListener<UserToken> ()
              {
                @Override
                public void onErrorResponse (VolleyError error)
                {
                  Log.e (TAG, error.getMessage (), error);
                  completeSignInProcess (false);

                  // Notify the parent view there was an error.
                  onLoginFragmentListener_.onLoginError (LoginFragment.this, error);
                }

                @Override
                public void onResponse (UserToken response)
                {
                  completeSignInProcess (true);
                }
              });
    }
    else
    {
      inputError.requestFocus ();
    }
  }

  /**
   * Complete the sign-in process for the client.
   *
   * @param finish      The process is finished
   */
  private void completeSignInProcess (boolean finish)
  {
    this.loginRequest_ = null;
    this.hideProgress ();

    if (finish)
      this.onLoginFragmentListener_.onLoginComplete (this);
  }

  /**
   * Hide the progress view
   */
  private void hideProgress ()
  {
    this.showProgress (false, null);
  }

  /**
   * Shows the progress UI and hides the login form.
   */
  @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
  private void showProgress (final boolean show, String progressText)
  {
    // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
    // for very easy animations. If available, use these APIs to fade-in
    // the progress spinner.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2)
    {
      int shortAnimTime = this.getResources ().getInteger (android.R.integer.config_shortAnimTime);

      // Show the login form, and make sure the error message is gone.
      this.showLoginForm (!show, shortAnimTime);
      this.errorMessageView_.setVisibility (View.GONE);

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
      this.progressView_.setVisibility (show ? View.VISIBLE : View.GONE);
      this.loginForm_.setVisibility (show ? View.GONE : View.VISIBLE);
    }

    if (show)
      this.progressTextView_.setText (progressText);
  }

  @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
  private void showLoginForm (final boolean show, int animationTime)
  {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2)
    {
      this.loginForm_.setVisibility (show ? View.VISIBLE : View.GONE);
      this.loginForm_.animate ().setDuration (animationTime).alpha (
          show ? 1 : 0).setListener (new AnimatorListenerAdapter ()
      {
        @Override
        public void onAnimationEnd (Animator animation)
        {
          loginForm_.setVisibility (show ? View.VISIBLE : View.GONE);
        }
      });
    }
    else
    {
      this.loginForm_.setVisibility (show ? View.GONE : View.VISIBLE);
    }
  }

  @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
  private void showLoginForm (boolean show)
  {
    int shortAnimTime = this.getResources ().getInteger (android.R.integer.config_shortAnimTime);
    this.showLoginForm (show, shortAnimTime);
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
    void onLoginComplete (LoginFragment fragment);
    void onCreateNewAccount (LoginFragment fragment);

    void onLoginError (LoginFragment fragment, Throwable t);
  }
}
