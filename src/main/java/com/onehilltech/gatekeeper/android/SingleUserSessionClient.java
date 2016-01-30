package com.onehilltech.gatekeeper.android;

import android.content.Context;
import android.content.pm.PackageManager;

import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.fasterxml.jackson.core.type.TypeReference;
import com.onehilltech.gatekeeper.android.model.Account;
import com.onehilltech.gatekeeper.android.model.AccountProfile;
import com.onehilltech.gatekeeper.android.model.UserToken;
import com.raizlabs.android.dbflow.sql.language.Select;

import java.lang.reflect.InvocationTargetException;

public class SingleUserSessionClient extends UserSessionClient
{
  private UserToken userToken_;

  private Account whoami_;

  public interface OnInitializedListener
  {
    void onInitialized (SingleUserSessionClient sessionClient);
    void onError (Throwable t);
  }

  /**
   * Initialize the single user session client.
   *
   * @param context
   * @param listener
   */
  public static void initialize (Context context, OnInitializedListener listener)
  {
    initialize (context, Volley.newRequestQueue (context), listener);
  }

  public static void initialize (Context context,
                                 RequestQueue queue,
                                 final OnInitializedListener listener)
  {
    try
    {
      GatekeeperClient.initialize (context, queue, new GatekeeperClient.OnInitialized ()
      {
        @Override
        public void onInitialized (GatekeeperClient client)
        {
          SingleUserSessionClient sessionClient = new SingleUserSessionClient (client);
          listener.onInitialized (sessionClient);
        }

        @Override
        public void onError (VolleyError error)
        {
          listener.onError (new RuntimeException ("Failed to initialize session client", error));
        }
      });
    }
    catch (PackageManager.NameNotFoundException | IllegalAccessException | InvocationTargetException | ClassNotFoundException e)
    {
      listener.onError (new RuntimeException ("Failed to initialize session client", e));
    }
  }

  /**
   * Initializing constructor.
   *
   * @param client
   */
  private SingleUserSessionClient (GatekeeperClient client)
  {
    super (client);

    this.initUserToken ();
  }

  private void initUserToken ()
  {
    this.userToken_ = new Select ().from (UserToken.class).querySingle ();
  }

  /**
   * Log out the current user.
   */
  public void logout (final ResponseListener <Boolean> listener)
  {
    if (this.userToken_ == null)
      throw new IllegalStateException ("User is already logged out");

    this.client_.logout (this.userToken_, new ResponseListener<Boolean> ()
    {
      @Override
      public void onErrorResponse (VolleyError error)
      {
        listener.onErrorResponse (new VolleyError ("Failed to logout user", error));
      }

      @Override
      public void onResponse (Boolean response)
      {
        if (response)
          completeLogout ();

        listener.onResponse (response);
      }
    });
  }

  /**
   * Complete the logout process.
   */
  private void completeLogout ()
  {
    this.userToken_.delete ();
    this.userToken_ = null;
  }

  /**
   * Get the user token.
   *
   * @return
   */
  public UserToken getUserToken ()
  {
    return this.userToken_;
  }

  /**
   * Get the account information for the current user.
   *
   * @return
   */
  public void getMyAccount (final ResponseListener <Account> listener)
  {
    if (this.whoami_ != null)
    {
      listener.onResponse (this.whoami_);
      return;
    }

    this.client_.whoami (this.userToken_, new ResponseListener<Account> ()
    {
      @Override
      public void onErrorResponse (VolleyError error)
      {
        listener.onErrorResponse (new VolleyError ("Failed to get account information", error));
      }

      @Override
      public void onResponse (Account response)
      {
        whoami_ = response;
        listener.onResponse (response);
      }
    });
  }

  /**
   * Get the profile for an account.
   *
   * @param listener
   * @return
   */
  public JsonRequest getAccountProfile (ResponseListener <AccountProfile> listener)
  {
    return this.getClient ().getAccountProfile (this.userToken_, listener);
  }

  /**
   * Test if the client is logged in.
   *
   * @return
   */
  public boolean isLoggedIn ()
  {
    return this.userToken_ != null;
  }


  /**
   * Make a JsonRequest object for the current user. The request will set the
   * authorization header for the server to validate.
   *
   * @param method
   * @param path
   * @param typeReference
   * @param listener
   * @param <T>
   * @return
   */
  public <T> JsonRequest <T> makeJsonRequest (int method,
                                              String path,
                                              TypeReference<T> typeReference,
                                              ResponseListener<T> listener)
  {
    return this.client_.makeJsonRequest (method, path, this.userToken_, typeReference, listener);
  }
}
