package com.onehilltech.gatekeeper.android;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;

import com.onehilltech.backbone.http.Resource;
import com.onehilltech.gatekeeper.android.http.JsonAccount;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class NewAccountFragment extends Fragment
{
  public interface Listener
  {
    void onAccountCreated (NewAccountFragment fragment, JsonAccount account);
    void onError (NewAccountFragment fragment, Throwable t);
  }

  private Listener listener_;

  private GatekeeperSessionClient sessionClient_;

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
  public void onAttach (Context context)
  {
    super.onAttach (context);

    this.listener_ = (Listener) context;

    GatekeeperClient gatekeeper =
        new GatekeeperClient.Builder (context)
            .setClient (this.getHttpClient ())
            .build ();

    this.sessionClient_ =
        new GatekeeperSessionClient.Builder (context)
            .setGatekeeperClient (gatekeeper)
            .build ();
  }

  @Override
  public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
  {
    View view = inflater.inflate (R.layout.fragment_new_account, container, false);

    this.usernameView_ = (EditText) view.findViewById (R.id.username);
    this.emailView_ = (AutoCompleteTextView) view.findViewById (R.id.email);
    this.passwordView_ = (EditText) view.findViewById (R.id.password);
    this.confirmPasswordView_ = (EditText) view.findViewById (R.id.confirm_password);

    Button btnCreate = (Button) view.findViewById (R.id.button_create_account);
    btnCreate.setOnClickListener (new View.OnClickListener ()
    {
      @Override
      public void onClick (View view)
      {
        onCreateAccount ();
      }
    });

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
   * Attempts to sign in or register the account specified by the signIn form. If
   * there are form errors (invalid email, missing fields, etc.), the errors are
   * presented and no actual signIn attempt is made.
   */
  private void onCreateAccount ()
  {
    if (!this.validateInput ())
      return;

    String username = this.usernameView_.getText ().toString ();
    String password = this.passwordView_.getText ().toString ();
    String email = this.emailView_.getText ().toString ();

    this.sessionClient_.createAccount (username, password, email, new Callback<Resource> ()
    {
      @Override
      public void onResponse (Call<Resource> call, Response<Resource> response)
      {
        if (response.isSuccessful ())
        {
          JsonAccount account = response.body ().get ("account");
          listener_.onAccountCreated (NewAccountFragment.this, account);
        }
      }

      @Override
      public void onFailure (Call<Resource> call, Throwable t)
      {
        listener_.onError (NewAccountFragment.this, t);
      }
    });
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

    // Store values at the time of the signIn attempt.
    String username = this.usernameView_.getText ().toString ();
    String password = this.passwordView_.getText ().toString ();
    String confirmPassword = this.confirmPasswordView_.getText ().toString ();
    String email = this.emailView_.getText ().toString ();


    return true;
  }
}
