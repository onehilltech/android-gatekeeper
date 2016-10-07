package com.onehilltech.gatekeeper.android;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.onehilltech.gatekeeper.android.model.UserToken;

/**
 * @class LoginFragment
 *
 * Base class for all login fragments. The LoginFragment initializes a
 * session client, and provide a login() method to perform the login task.
 */
public abstract class LoginFragment extends Fragment
  implements SingleUserSessionClient.OnInitializedListener
{
  private LoginFragmentListener loginFragmentListener_;

  private SingleUserSessionClient sessionClient_;

  private RequestQueue requestQueue_;

  public LoginFragment ()
  {
    // Required empty public constructor
  }

  protected LoginFragmentListener getLoginFragmentListener ()
  {
    return this.loginFragmentListener_;
  }

  /**
   * Allow the subclass to provide it's own request queue.
   *
   * @return
   */
  protected RequestQueue onCreateRequestQueue ()
  {
    return Volley.newRequestQueue (this.getContext ());
  }

  @Override
  public void onViewCreated (View view, Bundle savedInstanceState)
  {
    super.onViewCreated (view, savedInstanceState);

    // Initialize the Gatekeeper session client. During the initialization process, allow
    // the subclass to provide its own RequestQueue. This is important since applications
    // may have special networking needs that still need to be honored.
    this.requestQueue_ = this.onCreateRequestQueue ();
    SingleUserSessionClient.initialize (this.getActivity (), this.requestQueue_, this);
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
  protected void login (String username, String password)
  {
    this.sessionClient_.loginUser (
        username,
        password,
        new ResponseListener<UserToken> ()
        {
          @Override
          public void onErrorResponse (VolleyError error)
          {
            loginFragmentListener_.onLoginError (LoginFragment.this, error);
          }

          @Override
          public void onResponse (UserToken response)
          {
            loginFragmentListener_.onLoginComplete (LoginFragment.this);
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
    void onLoginComplete (LoginFragment fragment);

    void onLoginError (LoginFragment fragment, Throwable t);

    void onCreateNewAccount (LoginFragment fragment);
  }
}
