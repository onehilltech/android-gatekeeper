package com.onehilltech.gatekeeper.android.utils;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.onehilltech.gatekeeper.android.R;
import com.onehilltech.gatekeeper.android.SingleUserSessionClient;

public abstract class BaseLoginFragment extends Fragment
  implements SingleUserSessionClient.Listener
{
  private static final String TAG = "BaseLoginFragment";

  private LoginFragmentListener loginFragmentListener_;

  private SingleUserSessionClient userSessionClient_;

  @Override
  public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
  {
    return inflater.inflate (R.layout.fragment_login, container, false);
  }

  @Override
  public void onViewCreated (View view, Bundle savedInstanceState)
  {
    super.onViewCreated (view, savedInstanceState);
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
  }

  @Override
  public void onError (Throwable t)
  {
    Log.e (TAG, t.getLocalizedMessage (), t);
  }

  /**
   * Complete the sign-in process for the client.
   *
   * @param finish      The process is finished
   */
  protected void completeSignInProcess (boolean finish)
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
    void onLoginComplete (BaseLoginFragment fragment);

    void onCreateNewAccount (BaseLoginFragment fragment);

    void onLoginError (BaseLoginFragment fragment, Throwable t);
  }
}
