package com.onehilltech.gatekeeper.android;

import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.fasterxml.jackson.core.type.TypeReference;
import com.onehilltech.gatekeeper.android.model.AccessToken;
import com.onehilltech.gatekeeper.android.model.ClientToken;
import com.onehilltech.gatekeeper.android.model.ClientToken_Table;
import com.onehilltech.gatekeeper.android.model.UserToken;
import com.onehilltech.gatekeeper.android.model.UserToken_Table;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.config.*;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.structure.database.transaction.QueryTransaction;

import java.lang.reflect.InvocationTargetException;

/**
 * Client interface for communicating with a Gatekeeper service.
 */
public class GatekeeperClient
{
  /// Logging tag.
  private static final String TAG = "GatekeeperClient";

  /// Base URI for the Gatekeeper service.
  private final String baseUri_;

  /// The client id.
  private final String clientId_;

  /// Underlying HTTP client.
  private final RequestQueue requestQueue_;

  /// Authorization token for the client.
  private ClientToken clientToken_;

  public static final int VERSION = 1;

  private String tag_;

  /**
   * OnRegistrationCompleteListener interface for initializing the client.
   */
  public interface OnInitializedListener
  {
    /**
     * Callback for completion of the initialization process.
     *
     * @param client      Initialized client
     */
    void onInitialized (GatekeeperClient client);

    /**
     * Callback for an error.
     *
     * @param e       The error that occurred.
     */
    void onInitializeFailed (Throwable e);
  }

  public interface OnResultListener <T>
  {
    void onResult (T result);
    void onError (VolleyError e);
  }

  /**
   * Initialize a new GatekeeperClient using information from the metadata in
   * AndroidManifest.xml.
   *
   * @param context         Target context
   * @param onInitializedListener   Callback for initialization
   *
   * @throws PackageManager.NameNotFoundException
   * @throws IllegalAccessException
   * @throws ClassNotFoundException
   * @throws InvocationTargetException
   */
  public static void initialize (Context context, OnInitializedListener onInitializedListener)
  {
    RequestQueue requestQueue = Volley.newRequestQueue (context);
    initialize (context, requestQueue, onInitializedListener);
  }

  /**
   * Initialize a new GatekeeperClient object.
   *
   * @param context
   * @param requestQueue
   * @param onInitializedListener
   * @return
   * @throws PackageManager.NameNotFoundException
   * @throws IllegalAccessException
   * @throws ClassNotFoundException
   * @throws InvocationTargetException
   */
  public static void initialize (Context context,
                                 RequestQueue requestQueue,
                                 OnInitializedListener onInitializedListener)
  {
    try
    {
      Configuration configuration = Configuration.loadFromMetadata (context);
      initialize (configuration, requestQueue, onInitializedListener);
    }
    catch (PackageManager.NameNotFoundException | IllegalAccessException | InvocationTargetException | ClassNotFoundException e)
    {
      onInitializedListener.onInitializeFailed (new RuntimeException ("Invalid or missing configuration", e));
    }
  }

  /**
   * Initialize a new GatekeeperClient object.
   *
   * @param config            Client configuration
   * @param requestQueue      Volley RequestQueue for requests
   * @param onInitializedListener     Callback for initialization.
   */
  public static void initialize (final Configuration config,
                                 final RequestQueue requestQueue,
                                 final OnInitializedListener onInitializedListener)
  {
    // First, initialize the Gatekeeper DBFlow module.
    FlowManager.initModule (GatekeeperGeneratedDatabaseHolder.class);

    SQLite.select ()
          .from (ClientToken.class)
          .where (ClientToken_Table.client_id.eq (config.clientId))
          .async ()
          .querySingleResultCallback (new QueryTransaction.QueryResultSingleCallback<ClientToken> ()
          {
            @Override
            public void onSingleQueryResult (QueryTransaction transaction, @Nullable ClientToken clientToken)
            {
              initialize (config, clientToken, requestQueue, onInitializedListener);
            }
          }).execute ();
  }

  /**
   * Initialize a new GatekeeperClient object.
   *
   * @param config
   * @param clientToken
   * @param requestQueue
   * @param onInitializedListener
   */
  private static void initialize (Configuration config,
                                  ClientToken clientToken,
                                  RequestQueue requestQueue,
                                  OnInitializedListener onInitializedListener)
  {
    if (clientToken != null)
      makeGatekeeperClient (config, clientToken, requestQueue, onInitializedListener);
    else
      requestClientToken (config, requestQueue, onInitializedListener);
  }

  /**
   * Request a new client token. Requesting a new client token will result a initializing
   * a new GatekeeperClient object.
   */
  private static void requestClientToken (final Configuration config,
                                          final RequestQueue requestQueue,
                                          final OnInitializedListener onInitializedListener)
  {
    // To initialize the client, we must first get a token for the client. This
    // allows us to determine if the client is enabled. It also setups the client
    // object with the required token.
    String url = config.baseUri + "/v" + VERSION + "/oauth2/token";

    SignedRequest<JsonToken> request =
        new SignedRequest<> (
            Request.Method.POST,
            url,
            null,
            new TypeReference<JsonToken> () { },
            new ResponseListener<JsonToken> ()
            {
              @Override
              public void onErrorResponse (VolleyError error)
              {
                onInitializedListener.onInitializeFailed (error);
              }

              @Override
              public void onResponse (JsonToken response)
              {
                response.accept (new JsonTokenVisitor ()
                {
                  @Override
                  public void visitBearerToken (JsonBearerToken token)
                  {
                    // Set remaining properties on the bearer token, then save token to database.
                    Log.d (TAG, "Saving client token to database");
                    ClientToken clientToken = ClientToken.fromToken (config.clientId, token);
                    clientToken.save ();

                    makeGatekeeperClient (config, clientToken, requestQueue, onInitializedListener);
                  }
                });
              }
            });

    JsonClientCredentials clientCredentials = new JsonClientCredentials ();
    clientCredentials.clientId = config.clientId;
    clientCredentials.clientSecret = config.clientSecret;

    request.setData (clientCredentials);
    request.setShouldCache (false);

    requestQueue.add (request);
  }

  /**
   * Helper method to make the GatekeeperClient object, and call the onRegistrationComplete
   * callback with the client.
   *
   * @param config              Client configuration
   * @param clientToken         Client access token
   * @param requestQueue        Request queue for sending request to server
   * @param onInitializedListener       Callback for initialization complete
   */
  private static void makeGatekeeperClient (Configuration config,
                                            ClientToken clientToken,
                                            RequestQueue requestQueue,
                                            OnInitializedListener onInitializedListener)
  {
    // Create a GatekeeperClient with the token.
    GatekeeperClient client =
        new GatekeeperClient (config.baseUri,
                              config.clientId,
                              clientToken,
                              requestQueue);

    onInitializedListener.onInitialized (client);
  }

  /**
   * Initializing constructor.
   *
   * @param baseUri
   * @param clientToken
   */
  GatekeeperClient (String baseUri, String clientId, ClientToken clientToken, RequestQueue requestQueue)
  {
    this.baseUri_ = baseUri;
    this.clientId_ = clientId;
    this.clientToken_ = clientToken;
    this.requestQueue_ = requestQueue;
  }

  /**
   * Get the client token.
   *
   * @return
   */
  ClientToken getClientToken ()
  {
    return this.clientToken_;
  }

  /**
   * Get the baseuri for the GatekeeperClient.
   *
   * @return
   */
  public String getBaseUri ()
  {
    return this.baseUri_;
  }

  /**
   * Get the client id.
   *
   * @return
   */
  public String getClientId ()
  {
    return this.clientId_;
  }

  public String getTag ()
  {
    return this.tag_;
  }

  public void setTag (String tag)
  {
    this.tag_ = tag;
  }

  /**
   * Create a new account on the Gatekeeper server. The result of this method is a Boolean
   * value that determines if the account was created. Once the account has been created,
   * the application should request a token on behalf of the newly created user.
   *
   * @param username        Username
   * @param password        Password
   */
  public SignedRequest <?> createAccount (String username,
                                          String password,
                                          String email,
                                          final OnResultListener <JsonAccount> listener)
  {
    JsonNewAccount newAccount = new JsonNewAccount ();
    newAccount.clientId = this.clientId_;
    newAccount.username = username;
    newAccount.password = password;
    newAccount.email = email;

    String url = this.getCompleteUrl ("/accounts");

    SignedRequest<JsonAccount> request =
        new SignedRequest<> (
            Request.Method.POST,
            url,
            this.clientToken_,
            new TypeReference<JsonAccount> () { },
            newAccount,
            new ResponseListener<JsonAccount> ()
            {
              @Override
              public void onErrorResponse (VolleyError error)
              {
                listener.onError (error);
              }

              @Override
              public void onResponse (JsonAccount response)
              {
                listener.onResult (response);
              }
            });

    this.addRequest (request);

    return request;
  }

  /**
   * Get an access token for the user.
   *
   * @param username        Username
   * @param password        Password
   * @param listener        Callback listener
   */
  public void getUserToken (final String username,
                            final String password,
                            final ResponseListener<UserToken> listener)
  {
    // First, see if there is a token on the device for the username/password
    // combination. If it does not exist, then we need to request the token
    // for the username/password from the service.
    SQLite.select ()
          .from (UserToken.class)
          .where (UserToken_Table.username.eq (username))
          .async ()
          .querySingleResultCallback (new QueryTransaction.QueryResultSingleCallback<UserToken> ()
          {
            @Override
            public void onSingleQueryResult (QueryTransaction transaction, @Nullable UserToken userToken)
            {
              if (userToken != null)
              {
                listener.onResponse (userToken);
              }
              else
              {
                JsonUserCredentials userCredentials = new JsonUserCredentials ();
                userCredentials.clientId = clientId_;
                userCredentials.username = username;
                userCredentials.password = password;

                requestUserToken (username, userCredentials, listener);
              }
            }
          }).execute ();
  }

  /**
   * Refresh the current access token.
   *
   * @param listener        Callback listener
   */
  public SignedRequest <?> refreshToken (UserToken token, ResponseListener <UserToken> listener)
  {
    if (!token.canRefresh ())
      throw new IllegalStateException ("Current token cannot be refreshed");

    JsonRefreshToken refreshToken = new JsonRefreshToken ();
    refreshToken.clientId = this.clientId_;
    refreshToken.refreshToken = token.getRefreshToken ();

    return this.requestUserToken (token.getUsername (), refreshToken, listener);
  }

  /**
   * Get a JsonBearerToken for the jsonGrant information. The token can come from the database, if
   * it already exists, or it can come from the service.
   *
   * @param grantType           JsonGrant object
   * @param listener
   * @return
   */
  private SignedRequest <?> requestUserToken (final String username,
                                              final JsonGrant grantType,
                                              final ResponseListener<UserToken> listener)
  {
    // Since we do not have this as a token, we must contact the server to get a
    // token for the jsonGrant.
    final String url = this.getCompleteUrl ("/oauth2/token");

    SignedRequest<JsonToken> request =
        this.newSignedRequest (
            Request.Method.POST,
            url,
            this.clientToken_,
            new TypeReference<JsonToken> () { },
            grantType,
            new ResponseListener<JsonToken> ()
            {
              @Override
              public void onErrorResponse (VolleyError error)
              {
                listener.onErrorResponse (error);
              }

              @Override
              public void onResponse (JsonToken response)
              {
                response.accept (new JsonTokenVisitor ()
                {
                  @Override
                  public void visitBearerToken (JsonBearerToken token)
                  {
                    completeUserLogin (username, token, listener);
                  }
                });
              }
            });

    this.addRequest (request);

    return request;
  }

  /**
   * Complete the user login process.
   *
   * @param username
   * @param token
   * @param listener
   */
  private void completeUserLogin (String username,
                                  JsonBearerToken token,
                                  ResponseListener<UserToken> listener)
  {
    // Save the token to the database.
    UserToken userToken = UserToken.fromToken (username, token);
    userToken.save ();

    // Keep a reference to the token, and call the listener.
    listener.onResponse (userToken);
  }

  /**
   * Logout the current user.
   *
   * @param listener      Response listener
   */
  public SignedRequest <?> logout (final UserToken token, final ResponseListener <Boolean> listener)
  {
    String url = this.getCompleteUrl ("/oauth2/logout");

    SignedRequest<Boolean> request =
        this.newSignedRequest (
            Request.Method.GET,
            url,
            token,
            new TypeReference<Boolean> () { },
            new ResponseListener<Boolean> ()
            {
              @Override
              public void onErrorResponse (VolleyError error)
              {
                listener.onErrorResponse (error);
              }

              @Override
              public void onResponse (Boolean response)
              {
                if (response)
                  token.delete ();

                listener.onResponse (response);
              }
            });

    request.setShouldCache (false);

    this.addRequest (request);

    return request;
  }

  public <T> SignedRequest<T> newSignedRequest (int method,
                                                String path,
                                                AccessToken token,
                                                TypeReference<T> typeReference,
                                                ResponseListener<T> listener)
  {
    return new SignedRequest<> (method, path, token, typeReference, listener);
  }

  public <T> SignedRequest<T> newSignedRequest (int method,
                                                String path,
                                                AccessToken token,
                                                TypeReference<T> typeReference,
                                                Object data,
                                                ResponseListener<T> listener)
  {
    return new SignedRequest<> (method, path, token, typeReference, data, listener);
  }

  /**
   * Add a request to the queue.
   *
   * @param request        Add request to queue
   */
  public void addRequest (Request <?> request)
  {
    if (this.tag_ != null)
      request.setTag (this.tag_);

    this.requestQueue_.add (request);
  }

  /**
   * Get the complete URL for a path.
   *
   * @param relativePath   Relative path of the url
   * @return String containing the complete url path
   */
  public String getCompleteUrl (String relativePath)
  {
    return this.baseUri_ + "/v" + VERSION + relativePath;
  }
}
