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

import com.onehilltech.gatekeeper.android.http.JsonAccount;
import com.onehilltech.gatekeeper.android.http.jsonapi.Resource;
import com.onehilltech.gatekeeper.android.utils.InputError;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NewAccountFragment extends Fragment
  implements GatekeeperClient.OnInitializedListener
{
  public interface Listener
  {
    void onAccountCreated (NewAccountFragment fragment, JsonAccount account);
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
    GatekeeperClient.initialize (this.getContext (), this.getHttpClient (), this);
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

  protected OkHttpClient getHttpClient ()
  {
    return new OkHttpClient.Builder ().build ();
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

    String username = this.usernameView_.getText ().toString ();
    String password = this.passwordView_.getText ().toString ();
    String email = this.emailView_.getText ().toString ();

    this.gatekeeper_.createAccount (username, password, email)
                    .enqueue (new Callback<Resource> ()
                    {
                      @Override
                      public void onResponse (Call<Resource> call, Response<Resource> response)
                      {
                        if (response.isSuccessful ())
                          onAccountCreated ((JsonAccount)response.body ().get ("account"));
                      }

                      @Override
                      public void onFailure (Call<Resource> call, Throwable t)
                      {
                        listener_.onError (NewAccountFragment.this, t);
                      }
                    });
  }

  public void onAccountCreated (JsonAccount account)
  {
    // Save the account to our local database.

    // Notify the listener the account has been created.
    this.listener_.onAccountCreated (this, account);
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
