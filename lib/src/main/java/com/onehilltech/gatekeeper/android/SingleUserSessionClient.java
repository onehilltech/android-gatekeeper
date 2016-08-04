package com.onehilltech.gatekeeper.android;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.fasterxml.jackson.core.type.TypeReference;
import com.onehilltech.gatekeeper.android.model.UserToken;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.structure.database.transaction.QueryTransaction;

public class SingleUserSessionClient extends UserSessionClient
{
  private UserToken userToken_;

  public interface OnInitializedListener
  {
    void onInitialized (SingleUserSessionClient sessionClient);
    void onInitializeFailed (Throwable t);
  }

  /**
   * Initialize the single user session client.
   *
   * @param context
   * @param onInitializedListener
   */
  public static void initialize (Context context,
                                 @NonNull OnInitializedListener onInitializedListener)
  {
    initialize (context, Volley.newRequestQueue (context), onInitializedListener);
  }

  /**
   * Initialize the single user session client.
   *
   * @param context
   * @param queue
   * @param onInitializedListener
   */
  public static void initialize (Context context,
                                 RequestQueue queue,
                                 @NonNull final OnInitializedListener onInitializedListener)
  {
    GatekeeperClient.initialize (context, queue, new GatekeeperClient.OnInitializedListener () {
      @Override
      public void onInitialized (final GatekeeperClient client)
      {
        SQLite.select ()
              .from (UserToken.class)
              .async ()
              .querySingleResultCallback (new QueryTransaction.QueryResultSingleCallback<UserToken> ()
              {
                @Override
                public void onSingleQueryResult (QueryTransaction transaction, @Nullable UserToken userToken)
                {
                  SingleUserSessionClient sessionClient = new SingleUserSessionClient (client, userToken);
                  onInitializedListener.onInitialized (sessionClient);
                }
              }).execute ();
      }

      @Override
      public void onInitializeFailed (Throwable e)
      {
        onInitializedListener.onInitializeFailed (e);
      }
    });
  }

  /**
   * Initializing constructor.
   *
   * @param client
   */
  private SingleUserSessionClient (GatekeeperClient client, UserToken userToken)
  {
    super (client);

    this.userToken_ = userToken;
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
   * Test if the client is logged in.
   *
   * @return
   */
  public boolean isLoggedIn ()
  {
    return this.userToken_ != null;
  }

  public <T> SignedRequest<T> newSignedRequest (int method,
                                               String path,
                                               TypeReference<T> typeReference,
                                               ResponseListener<T> listener)
  {
    return this.client_.newSignedRequest (method, path, this.userToken_, typeReference, listener);
  }

  public <T> SignedRequest<T> newSignedRequest (int method,
                                                String path,
                                                TypeReference<T> typeReference,
                                                Object data,
                                                ResponseListener<T> listener)
  {
    return this.client_.newSignedRequest (method, path, this.userToken_, typeReference, data, listener);
  }
}
