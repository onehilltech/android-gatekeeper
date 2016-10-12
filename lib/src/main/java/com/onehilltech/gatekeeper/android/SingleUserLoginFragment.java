package com.onehilltech.gatekeeper.android;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import com.onehilltech.gatekeeper.android.http.JsonBearerToken;

import okhttp3.OkHttpClient;
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
  implements SingleUserSessionClient.OnInitializedListener
{
  private LoginFragmentListener loginFragmentListener_;

  private SingleUserSessionClient sessionClient_;

  public SingleUserLoginFragment ()
  {
    // Required empty public constructor
  }

  protected LoginFragmentListener getLoginFragmentListener ()
  {
    return this.loginFragmentListener_;
  }

  protected OkHttpClient getHttpClient ()
  {
    return new OkHttpClient.Builder ().build ();
  }

  @Override
  public void onViewCreated (View view, Bundle savedInstanceState)
  {
    super.onViewCreated (view, savedInstanceState);

    SingleUserSessionClient.initialize (this.getActivity (), this.getHttpClient (), this);
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
    this.sessionClient_ = client;

    if (client.isLoggedIn ())
      this.loginFragmentListener_.onLoginComplete (this);
  }

  @Override
  public void onInitializeFailed (Throwable t)
  {
    this.loginFragmentListener_.onLoginError (this, t);
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
    void onLoginComplete (SingleUserLoginFragment fragment);

    void onLoginError (SingleUserLoginFragment fragment, Throwable t);

    void onCreateNewAccount (SingleUserLoginFragment fragment);
  }
}
