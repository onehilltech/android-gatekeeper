package com.onehilltech.gatekeeper.android;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;

import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.onehilltech.gatekeeper.android.utils.InputError;

public class NewAccountFragment extends Fragment
  implements GatekeeperClient.OnInitializedListener
{
  public interface Listener
  {
    void onAccountCreated (NewAccountFragment fragment);
    void onError (NewAccountFragment fragment, Throwable t);
  }

  private static final String TAG = "NewAccountFragment";

  private Listener listener_;

  private GatekeeperClient gatekeeper_;

  // UI references.
  private EditText usernameView_;
  private EditText confirmPasswordView_;
  private AutoCompleteTextView emailView_;
  private EditText passwordView_;

  private String id_;
  private String username_;
  private String password_;
  private String email_;

  public NewAccountFragment ()
  {
    // Required empty public constructor
  }

  protected RequestQueue onCreateRequestQueue ()
  {
    return Volley.newRequestQueue (this.getContext ());
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

    this.usernameView_ = (EditText) view.findViewById (R.id.username);
    this.emailView_ = (AutoCompleteTextView) view.findViewById (R.id.email);
    this.passwordView_ = (EditText) view.findViewById (R.id.password);
    this.confirmPasswordView_ = (EditText) view.findViewById (R.id.confirm_password);

    Button buttonCreateAccount = (Button) view.findViewById (R.id.button_create_account);
    buttonCreateAccount.setOnClickListener (new View.OnClickListener ()
    {
      @Override
      public void onClick (View view)
      {
        onCreateAccount ();
      }
    });

    // Initialize the client.
    RequestQueue requestQueue = this.onCreateRequestQueue ();
    GatekeeperClient.initialize (this.getContext (), requestQueue, this);
  }

  @Override
  public void onAttach (Context context)
  {
    super.onAttach (context);

    try
    {
      this.listener_ = (Listener) context;
    }
    catch (ClassCastException e)
    {
      throw new ClassCastException (context.toString () + " must implement LoginFragmentListener");
    }
  }

  @Override
  public void onDetach ()
  {
    super.onDetach ();
    this.listener_ = null;
  }

  @Override
  public void onInitialized (GatekeeperClient client)
  {
    this.gatekeeper_ = client;
  }

  @Override
  public void onInitializeFailed (Throwable t)
  {
    if (this.listener_ != null)
      this.listener_.onError (this, t);
  }

  /**
   * Get the username from the view.
   *
   * @return
   */
  public String getUsername ()
  {
    return this.usernameView_.getText ().toString ();
  }

  /**
   * Get the password from the view.
   *
   * @return
   */
  public String getPassword ()
  {
    return this.passwordView_.getText ().toString ();
  }

  public String getUserId ()
  {
    return this.id_;
  }
  
  /**
   * Attempts to sign in or register the account specified by the login form. If
   * there are form errors (invalid email, missing fields, etc.), the errors are
   * presented and no actual login attempt is made.
   */
  private void onCreateAccount ()
  {
    if (this.gatekeeper_ == null)
      throw new IllegalStateException ("Gatekeeper client is not initialized");

    if (!this.validateInput ())
      return;

    this.username_ = this.usernameView_.getText ().toString ();
    this.password_ = this.passwordView_.getText ().toString ();
    this.email_ = this.emailView_.getText ().toString ();

    final GatekeeperClient.OnResultListener <AccountId> resultListener =
        new GatekeeperClient.OnResultListener<AccountId> ()
        {
          @Override
          public void onResult (AccountId result)
          {
            id_ = result._id;

            if (listener_ != null)
              listener_.onAccountCreated (NewAccountFragment.this);
          }

          @Override
          public void onError (VolleyError error)
          {
            if (listener_ != null)
              listener_.onError (NewAccountFragment.this, error);
          }
        };

    this.gatekeeper_.createAccount (this.username_, this.password_, this.email_, resultListener);
  }

  /**
   * Validate the input on the forms.
   *
   * @return
   */
  private boolean validateInput ()
  {
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

    if (inputError.hasError ())
      inputError.requestFocus ();

    return !inputError.hasError ();
  }
}
