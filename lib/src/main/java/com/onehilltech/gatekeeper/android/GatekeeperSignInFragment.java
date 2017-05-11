package com.onehilltech.gatekeeper.android;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.onehilltech.backbone.http.HttpError;
import com.onehilltech.gatekeeper.android.http.JsonBearerToken;
import com.rengwuxian.materialedittext.MaterialEditText;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * @class GatekeeperSignInFragment
 *
 * Base class for all signIn fragments. The GatekeeperSignInFragment initializes a
 * session client, and provide a signIn() method to perform the signIn task.
 */
public class GatekeeperSignInFragment extends Fragment
{
  public interface LoginFragmentListener
  {
    void onSignInComplete (GatekeeperSignInFragment fragment);
  }

  public static final class Builder
  {
    private final Bundle args_ = new Bundle ();

    public Builder setTitle (String title)
    {
      this.args_.putString (ARG_TITLE, title);
      return this;
    }

    public Builder setUsername (String username)
    {
      this.args_.putString (ARG_USERNAME, username);
      return this;
    }

    public Builder setUsernameHint (String hint)
    {
      this.args_.putString (ARG_USERNAME_HINT, hint);
      return this;
    }

    public Builder setPasswordHint (String hint)
    {
      this.args_.putString (ARG_PASSWORD_HINT, hint);
      return this;
    }

    public Builder setPassword (String password)
    {
      this.args_.putString (ARG_PASSWORD, password);
      return this;
    }

    public Builder setSignInButtonText (String text)
    {
      this.args_.putString (ARG_SIGN_IN_BUTTON_TEXT, text);
      return this;
    }

    public Builder setUsernameLabelText (String text)
    {
      this.args_.putString (ARG_USERNAME_LABEL, text);
      return this;
    }

    public Builder setPasswordLabelText (String text)
    {
      this.args_.putString (ARG_PASSWORD_LABEL, text);
      return this;
    }

    public Builder setCreateAccountIntent (Intent intent)
    {
      this.args_.putParcelable (ARG_CREATE_ACCOUNT_INTENT, intent);
      return this;
    }

    public Builder setErrorMessage (String errorMessage)
    {
      this.args_.putString (ARG_ERROR_MESSAGE, errorMessage);
      return this;
    }

    public Builder setLayout (int layout)
    {
      this.args_.putInt (ARG_LAYOUT, layout);
      return this;
    }

    public GatekeeperSignInFragment build ()
    {
      GatekeeperSignInFragment fragment = new GatekeeperSignInFragment ();
      fragment.setArguments (this.args_);

      return fragment;
    }
  }

  private LoginFragmentListener loginFragmentListener_;

  private GatekeeperSessionClient sessionClient_;

  private static final String ARG_TITLE = "title";

  private static final String ARG_USERNAME = "username";
  private static final String ARG_USERNAME_HINT = "username_hint";
  private static final String ARG_USERNAME_LABEL = "username_label";

  private static final String ARG_PASSWORD = "password";
  private static final String ARG_PASSWORD_HINT = "password_hint";
  private static final String ARG_PASSWORD_LABEL = "password_label";

  private static final String ARG_CREATE_ACCOUNT_INTENT = "create_account_intent";

  private static final String ARG_SIGN_IN_BUTTON_TEXT = "sign_in_button_text";
  private static final String ARG_ERROR_MESSAGE = "error_message";

  private static final String ARG_LAYOUT = "layout";

  private MaterialEditText username_;

  private MaterialEditText password_;

  private TextView errorMessage_;

  private int layout_ = R.layout.fragment_login;

  /**
   * Default constructor.
   */
  public GatekeeperSignInFragment ()
  {
    // Required empty public constructor
  }

  protected GatekeeperSessionClient getSessionClient ()
  {
    return new GatekeeperSessionClient.Builder (this.getActivity ()).build ();
  }

  @Override
  public void onAttach (Activity activity)
  {
    super.onAttach (activity);
    this.onAttachImpl (activity);
  }

  @Override
  public void onAttach (Context context)
  {
    super.onAttach (context);
    this.onAttachImpl (context);
  }

  private void onAttachImpl (Context context)
  {
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
  public void onCreate (@Nullable Bundle savedInstanceState)
  {
    super.onCreate (savedInstanceState);

    this.sessionClient_ = this.getSessionClient ();

    Bundle args = this.getArguments ();

    if (args != null)
    {
      if (args.containsKey (ARG_LAYOUT))
        this.layout_ = args.getInt (ARG_LAYOUT);
    }
  }

  @Override
  public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
  {
    View view = inflater.inflate (this.layout_, container, false);

    // Setup the UI controls.
    this.username_ = (MaterialEditText) view.findViewById (R.id.username);
    this.username_.addValidator (new NotEmptyValidator ());

    this.password_ = (MaterialEditText) view.findViewById (R.id.password);
    this.password_.addValidator (new NotEmptyValidator ());

    Button signInButton = (Button)view.findViewById (R.id.button_sign_in);
    signInButton.setOnClickListener (new View.OnClickListener ()
    {
      @Override
      public void onClick (View v)
      {
        performSignIn ();
      }
    });

    TextView title = (TextView)view.findViewById (R.id.title);
    this.errorMessage_ = (TextView)view.findViewById (R.id.error_message);

    // Initialize the view with data.
    Bundle args = this.getArguments ();

    if (args != null)
    {
      if (args.containsKey (ARG_TITLE))
      {
        title.setVisibility (View.VISIBLE);
        title.setText (args.getString (ARG_TITLE));
      }
      else
      {
        title.setVisibility (View.GONE);
      }

      if (args.containsKey (ARG_USERNAME))
        this.username_.setText (args.getString (ARG_USERNAME));

      if (args.containsKey (ARG_USERNAME_HINT))
        this.username_.setHint (args.getString (ARG_USERNAME_HINT));

      if (args.containsKey (ARG_PASSWORD))
        this.password_.setText (args.getString (ARG_PASSWORD));

      if (args.containsKey (ARG_PASSWORD_HINT))
        this.password_.setHint (args.getString (ARG_PASSWORD_HINT));

      if (args.containsKey (ARG_SIGN_IN_BUTTON_TEXT))
        signInButton.setText (args.getString (ARG_SIGN_IN_BUTTON_TEXT));

      if (args.containsKey (ARG_USERNAME_LABEL))
        this.username_.setFloatingLabelText (args.getString (ARG_USERNAME_LABEL));

      if (args.containsKey (ARG_PASSWORD_LABEL))
        this.password_.setFloatingLabelText (args.getString (ARG_PASSWORD_LABEL));

      if (args.containsKey (ARG_ERROR_MESSAGE))
        showErrorMessage (args.getString (ARG_ERROR_MESSAGE));

      TextView actionCreateNewAccount = (TextView)view.findViewById (R.id.action_create_account);

      if (args.containsKey (ARG_CREATE_ACCOUNT_INTENT))
      {
        final Intent targetIntent = args.getParcelable (ARG_CREATE_ACCOUNT_INTENT);

        actionCreateNewAccount.setVisibility (View.VISIBLE);
        actionCreateNewAccount.setOnClickListener (new View.OnClickListener ()
        {
          @Override
          public void onClick (View v)
          {
            startNewAccountActivity (targetIntent);
          }
        });
      }
      else
      {
        actionCreateNewAccount.setVisibility (View.GONE);
      }
    }

    return view;
  }

  @Override
  public void onDetach ()
  {
    super.onDetach ();

    this.loginFragmentListener_ = null;
  }

  @Override
  public void onDestroy ()
  {
    super.onDestroy ();

    if (this.sessionClient_ != null)
      this.sessionClient_.onDestroy ();
  }

  /**
   * Start the activity for creating a new account.
   *
   * @param targetIntent
   */
  protected void startNewAccountActivity (Intent targetIntent)
  {
    // Start the activity for creating a new account.
    Activity activity = getActivity ();

    Intent upIntent = activity.getIntent ();
    Intent redirectIntent = upIntent.getParcelableExtra (GatekeeperSignInActivity.ARG_REDIRECT_INTENT);

    targetIntent.putExtra (GatekeeperCreateAccountActivity.EXTRA_REDIRECT_INTENT, redirectIntent);
    targetIntent.putExtra (GatekeeperCreateAccountActivity.EXTRA_UP_INTENT, upIntent);

    // Start the activity for creating the account, and finish this activity.
    activity.startActivity (targetIntent);
    activity.finish ();
  }

  /**
   * Sign in the user with the provided credentials.
   */
  private void performSignIn ()
  {
    boolean isValid = this.username_.validate ();
    isValid &= this.password_.validate ();

    if (!isValid)
      return;

    String username = this.username_.getText ().toString ();
    String password = this.password_.getText ().toString ();
    this.signIn (username, password);
  }

  /**
   * Sign in the user.
   *
   * @param username
   * @param password
   */
  protected void signIn (final String username, String password)
  {
    this.sessionClient_.signIn (username, password, new Callback<JsonBearerToken> ()
    {
      @Override
      public void onResponse (Call<JsonBearerToken> call, Response<JsonBearerToken> response)
      {
        if (response.isSuccessful ())
        {
          loginFragmentListener_.onSignInComplete (GatekeeperSignInFragment.this);
        }
        else
        {
          try
          {
            HttpError error = sessionClient_.getError (response.errorBody ());
            showErrorMessage (error.getMessage ());
          }
          catch (IOException e)
          {
            showErrorMessage ("Sign in failed");
          }
        }
      }

      @Override
      public void onFailure (Call<JsonBearerToken> call, Throwable t)
      {
        showErrorMessage ("Sign in failed");
      }
    });
  }

  /**
   * Show an error message. This causes all views to be hidden except for the logo and
   * the error message. When this happens, the user must exit the parent activity.
   *
   * @param errMsg
   */
  protected void showErrorMessage (String errMsg)
  {
    if (this.errorMessage_.getVisibility () != View.VISIBLE)
      this.errorMessage_.setVisibility (View.VISIBLE);

    this.errorMessage_.setText (errMsg);
  }
}
