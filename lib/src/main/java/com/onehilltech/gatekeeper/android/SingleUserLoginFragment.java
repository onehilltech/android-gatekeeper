package com.onehilltech.gatekeeper.android;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.onehilltech.gatekeeper.android.http.JsonBearerToken;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * @class SingleUserLoginFragment
 *
 * Base class for all login fragments. The SingleUserLoginFragment initializes a
 * session client, and provide a login() method to perform the login task.
 */
public abstract class SingleUserLoginFragment extends Fragment
{
  public interface LoginFragmentListener
  {
    void onLoginComplete (SingleUserLoginFragment fragment);
    void onLoginError (SingleUserLoginFragment fragment, Throwable t);
  }

  private LoginFragmentListener loginFragmentListener_;

  private SingleUserSessionClient sessionClient_;

  public SingleUserLoginFragment ()
  {
    // Required empty public constructor
  }

  protected SingleUserSessionClient getSessionClient ()
  {
    return new SingleUserSessionClient.Builder (this.getContext ()).build ();
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
  public void onCreate (@Nullable Bundle savedInstanceState)
  {
    super.onCreate (savedInstanceState);

    this.sessionClient_ = this.getSessionClient ();
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

  public LoginFragmentListener getLoginFragmentListener ()
  {
    return this.loginFragmentListener_;
  }

  /**
   * Perform the signin process with the Gatekeeper client.
   */
  protected void login (final String username, String password)
  {
    this.sessionClient_.login (username, password, new Callback<JsonBearerToken> ()
    {
      @Override
      public void onResponse (Call<JsonBearerToken> call, Response<JsonBearerToken> response)
      {
        if (response.isSuccessful ())
          loginFragmentListener_.onLoginComplete (SingleUserLoginFragment.this);
      }

      @Override
      public void onFailure (Call<JsonBearerToken> call, Throwable t)
      {
        loginFragmentListener_.onLoginError (SingleUserLoginFragment.this, t);
      }
    });
  }
}
