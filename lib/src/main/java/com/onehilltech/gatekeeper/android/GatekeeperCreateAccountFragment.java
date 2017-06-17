package com.onehilltech.gatekeeper.android;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.onehilltech.gatekeeper.android.http.JsonAccount;
import com.rengwuxian.materialedittext.MaterialEditText;

import okhttp3.OkHttpClient;


public class GatekeeperCreateAccountFragment extends Fragment
{
  public interface Listener
  {
    void onAccountCreated (GatekeeperCreateAccountFragment fragment, JsonAccount account);
    void onError (GatekeeperCreateAccountFragment fragment, Throwable t);
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

    public Builder setCreateButtonText (String text)
    {
      this.args_.putString (ARG_CREATE_BUTTON_TEXT, text);
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

    public Builder setUsernameIsEmail (boolean value)
    {
      this.args_.putBoolean (ARG_USERNAME_IS_EMAIL, value);
      return this;
    }

    public GatekeeperCreateAccountFragment build ()
    {
      GatekeeperCreateAccountFragment fragment = new GatekeeperCreateAccountFragment ();
      fragment.setArguments (this.args_);

      return fragment;
    }
  }

  private static final String ARG_TITLE = "title";

  private static final String ARG_USERNAME = "username";
  private static final String ARG_USERNAME_HINT = "username_hint";
  private static final String ARG_USERNAME_LABEL = "username_label";

  private static final String ARG_PASSWORD = "password";
  private static final String ARG_PASSWORD_HINT = "password_hint";
  private static final String ARG_PASSWORD_LABEL = "password_label";

  private static final String ARG_CREATE_ACCOUNT_INTENT = "create_account_intent";

  private static final String ARG_CREATE_BUTTON_TEXT = "create_button_text";

  private static final String ARG_USERNAME_IS_EMAIL = "username_is_email";

  private Listener listener_;

  private GatekeeperSessionClient session_;

  private MaterialEditText username_;

  private MaterialEditText password_;

  private MaterialEditText email_;

  private boolean usernameIsEmail_ = false;

  public GatekeeperCreateAccountFragment ()
  {
    // Required empty public constructor
  }

  @Override
  public void onAttach (Context context)
  {
    super.onAttach (context);

    this.listener_ = (Listener) context;

    GatekeeperClient gatekeeper =
        new GatekeeperClient.Builder (context)
            .setClient (this.getHttpClient ())
            .build ();

    this.session_ =
        new GatekeeperSessionClient.Builder (context)
            .setGatekeeperClient (gatekeeper)
            .build ();
  }

  @Override
  public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
  {
    View view = inflater.inflate (R.layout.fragment_new_account, container, false);

    TextView title = (TextView)view.findViewById (R.id.title);
    this.username_ = (MaterialEditText) view.findViewById (R.id.username);
    this.email_ = (MaterialEditText) view.findViewById (R.id.email);
    this.password_ = (MaterialEditText) view.findViewById (R.id.password);

    Button btnCreate = (Button) view.findViewById (R.id.button_create_account);
    btnCreate.setOnClickListener (new View.OnClickListener ()
    {
      @Override
      public void onClick (View view)
      {
        createAccount ();
      }
    });

    TextView signIn = (TextView)view.findViewById (R.id.action_sign_in);
    signIn.setOnClickListener (new View.OnClickListener ()
    {
      @Override
      public void onClick (View view)
      {
        Intent upIntent =
            getActivity ().getIntent ()
                          .getParcelableExtra (GatekeeperCreateAccountActivity.EXTRA_UP_INTENT);

        getActivity ().startActivity (upIntent);
        getActivity ().finish ();
      }
    });

    this.username_.addValidator (new NotEmptyValidator ());
    this.password_.addValidator (new NotEmptyValidator ());
    this.email_.addValidator (new NotEmptyValidator ());

    Bundle args = this.getArguments ();

    if (args != null)
    {
      if (args.containsKey (ARG_TITLE))
      {
        title.setText (args.getString (ARG_TITLE));
        title.setVisibility (View.VISIBLE);
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

      if (args.containsKey (ARG_CREATE_BUTTON_TEXT))
        btnCreate.setText (args.getString (ARG_CREATE_BUTTON_TEXT));

      if (args.containsKey (ARG_USERNAME_LABEL))
        this.username_.setFloatingLabelText (args.getString (ARG_USERNAME_LABEL));

      if (args.containsKey (ARG_PASSWORD_LABEL))
        this.password_.setFloatingLabelText (args.getString (ARG_PASSWORD_LABEL));

      if (args.containsKey (ARG_USERNAME_IS_EMAIL))
      {
        // The username is an email address. We do not need to show the email address
        // input field. We also need to update the input type for the username to that
        // of an email.
        this.usernameIsEmail_ = args.getBoolean (ARG_USERNAME_IS_EMAIL);

        if (this.usernameIsEmail_)
        {
          this.username_.setInputType (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
          this.email_.setVisibility (View.GONE);
        }
        else
        {
          this.username_.setInputType (InputType.TYPE_CLASS_TEXT);
          this.email_.setVisibility (View.VISIBLE);
        }
      }
    }

    return view;
  }

  @Override
  public void onDetach ()
  {
    super.onDetach ();

    this.listener_ = null;
  }

  protected OkHttpClient getHttpClient ()
  {
    return new OkHttpClient.Builder ().build ();
  }

  public String getUsername ()
  {
    return this.username_.getText ().toString ();
  }

  public String getPassword ()
  {
    return this.password_.getText ().toString ();
  }

  public String getEmail ()
  {
    return this.usernameIsEmail_ ? this.getUsername () : this.email_.getText ().toString ();
  }

  /**
   * Attempts to sign in or register the account specified by the signIn form. If
   * there are form errors (invalid email, missing fields, etc.), the errors are
   * presented and no actual signIn attempt is made.
   */
  private void createAccount ()
  {
    if (!this.validateInput ())
      return;

    String username = this.getUsername ();
    String password = this.getPassword ();
    String email = this.getEmail ();

    this.session_
        .createAccount (username, password, email, true)
        .then ((value, cont) -> this.listener_.onAccountCreated (this, value),
               (reason, cont) -> this.listener_.onError (this, reason));
  }

  /**
   * Validate the input on the forms.
   *
   * @return
   */
  private boolean validateInput ()
  {
    boolean isValid = this.username_.validate ();
    isValid &= this.password_.validate ();

    if (!this.usernameIsEmail_)
      isValid &= this.email_.validate ();

    return isValid;
  }
}
