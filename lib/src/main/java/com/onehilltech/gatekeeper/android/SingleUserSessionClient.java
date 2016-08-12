package com.onehilltech.gatekeeper.android;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.android.volley.Request;
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

  /**
   * Create a new signed request.
   *
   * @param method
   * @param path
   * @param typeReference
   * @param listener
   * @param <T>
   * @return
   */
  public <T> SignedRequest<T> newSignedRequest (int method,
                                               String path,
                                               TypeReference<T> typeReference,
                                               ResponseListener<T> listener)
  {
    AutoRefreshResponseListener <T> autoRefresh = new AutoRefreshResponseListener<> (listener);

    SignedRequest <T> signedRequest =
        this.client_.newSignedRequest (
            method,
            path,
            this.userToken_,
            typeReference,
            autoRefresh);

    autoRefresh.setOriginalRequest (signedRequest);

    return signedRequest;
  }

  /**
   * Create a new signed request.
   *
   * @param method
   * @param path
   * @param typeReference
   * @param data
   * @param listener
   * @param <T>
   * @return
   */
  public <T> SignedRequest<T> newSignedRequest (int method,
                                                String path,
                                                TypeReference<T> typeReference,
                                                Object data,
                                                ResponseListener<T> listener)
  {
    AutoRefreshResponseListener <T> autoRefresh = new AutoRefreshResponseListener<> (listener);

    SignedRequest <T> signedRequest =
        this.client_.newSignedRequest (
            method,
            path,
            this.userToken_,
            typeReference,
            data,
            autoRefresh);

    autoRefresh.setOriginalRequest (signedRequest);

    return signedRequest;
  }

  /**
   *
   * @param <T>
   */
  private class AutoRefreshResponseListener <T> implements ResponseListener <T>
  {
    private Request<T> request_;
    private final ResponseListener <T> responseListener_;

    private final ResponseListener<UserToken> refreshResponseListener_ =
        new ResponseListener<UserToken> ()
        {
          @Override
          public void onErrorResponse (VolleyError error)
          {
            // We failed to refresh to user token. So, we need to just return control
            // to the original response listener.
            responseListener_.onErrorResponse (error);
          }

          @Override
          public void onResponse (UserToken response)
          {
            // The token is refreshed. Let's try the same request again.
            client_.addRequest (request_);
          }
        };

    public AutoRefreshResponseListener (ResponseListener<T> responseListener)
    {
      this.responseListener_ = responseListener;
    }

    public void setOriginalRequest (Request <T> request)
    {
      this.request_ = request;
    }

    @Override
    public void onErrorResponse (VolleyError error)
    {
      if (error.networkResponse != null)
      {
        int statusCode = error.networkResponse.statusCode;

        if (statusCode == 401)
        {
          // Check if unauthorized access because of bad token.
          client_.refreshToken (userToken_, refreshResponseListener_);
        }
        else
        {
          // Pass the response to the original response listener.
          this.responseListener_.onErrorResponse (error);
        }
      }
      else
      {
        // Pass the response to the origin response listener.
        this.responseListener_.onErrorResponse (error);
      }
    }

    @Override
    public void onResponse (T response)
    {
      this.responseListener_.onResponse (response);
    }
  }
}
