package com.onehilltech.gatekeeper.android;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.onehilltech.gatekeeper.android.model.UserToken;
import com.onehilltech.gatekeeper.android.utils.ErrorMessageUtil;
import com.onehilltech.gatekeeper.android.utils.InputError;

public class LoginFragment extends Fragment
  implements SingleUserSessionClient.Listener
{
  private static final String TAG = "LoginFragment";

  private static final String ARG_USERNAME = "username";
  private static final String ARG_PASSWORD = "password";
  private static final String ARG_AUTO_LOGIN = "auto_login";

  private LoginFragmentListener loginFragmentListener_;

  private TextView usernameView_;
  private TextView passwordView_;

  private TextView errorMessageView_;

  private boolean autoLogin_ = false;

  private SingleUserSessionClient userSessionClient_;

  private Button signInButton_;

  /**
   * Create a new instance of the fragment.
   *
   * @return  LoginFragment object
   */
  public static LoginFragment newInstance ()
  {
    return new LoginFragment ();
  }

  /**
   * Create an instance of the LoginFragment with the username initialized.
   *
   * @param username
   * @return
   */
  @SuppressWarnings ("unused")
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

    this.signInButton_ = (Button)view.findViewById (R.id.button_sign_in);
    this.signInButton_.setOnClickListener (new View.OnClickListener ()
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
        loginFragmentListener_.onCreateNewAccount (LoginFragment.this);
      }
    });

    this.errorMessageView_ = (TextView)view.findViewById (R.id.error_message);

    // Load the application icon from the package information.
    ImageView iconView = (ImageView)view.findViewById (R.id.app_logo);

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

    // Initialize the Gatekeeper session client.
    SingleUserSessionClient.initialize (this.getActivity (), this);
  }

  @Override
  public void onAttach (Context context)
  {
    super.onAttach (context);

    try
    {
      this.loginFragmentListener_ = (LoginFragmentListener) context;
    }
    catch (ClassCastException e)
    {
      throw new ClassCastException (context + " must implement LoginFragmentListener");
    }
  }

  @Override
  public void onDetach ()
  {
    super.onDetach ();
    this.loginFragmentListener_ = null;
  }

  @Override
  public void onInitialized (SingleUserSessionClient client)
  {
    // Store our reference to the client, and check if the client is logged in.
    // This way, we can short circuit the login process.
    this.userSessionClient_ = client;

    if (client.isLoggedIn ())
      this.loginFragmentListener_.onLoginComplete (this);

    // In case the activity does not finish, we still need to hide the progress
    // and show the login form.
    if (this.autoLogin_)
      this.performSignIn ();
  }

  @Override
  public void onError (Throwable t)
  {
    if ((t instanceof VolleyError))
      this.handleVolleyError ((VolleyError)t);

    if (this.loginFragmentListener_ != null)
      this.loginFragmentListener_.onLoginError (this, t);
  }

  private void handleVolleyError (VolleyError e)
  {
    String errorMsg = ErrorMessageUtil.instance ().getErrorMessage (e);
    this.showErrorMessage (errorMsg);
  }

  /**
   * Show an error message. This causes all views to be hidden except for the logo and
   * the error message. When this happens, the user must exit the parent activity.
   *
   * @param errMsg
   */
  private void showErrorMessage (String errMsg)
  {
    if (this.errorMessageView_.getVisibility () != View.VISIBLE)
      this.errorMessageView_.setVisibility (View.VISIBLE);

    this.errorMessageView_.setText ("Error: " + errMsg);
  }

  /**
   * Perform the signin process with the Gatekeeper client.
   */
  private void performSignIn ()
  {
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
      this.userSessionClient_.loginUser (
          username,
          password,
          new ResponseListener<UserToken> ()
          {
            @Override
            public void onErrorResponse (VolleyError error)
            {
              handleVolleyError (error);
              completeSignInProcess (false);

              // Notify the parent view there was an error.
              loginFragmentListener_.onLoginError (LoginFragment.this, error);
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
    if (finish)
      this.loginFragmentListener_.onLoginComplete (this);
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
  public interface LoginFragmentListener
  {
    void onLoginComplete (LoginFragment fragment);

    void onCreateNewAccount (LoginFragment fragment);

    void onLoginError (LoginFragment fragment, Throwable t);
  }
}
