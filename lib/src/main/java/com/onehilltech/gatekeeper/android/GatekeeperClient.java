package com.onehilltech.gatekeeper.android;

import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.onehilltech.gatekeeper.android.data.BearerToken;
import com.onehilltech.gatekeeper.android.data.Token;
import com.onehilltech.gatekeeper.android.data.TokenVisitor;
import com.onehilltech.gatekeeper.android.model.AccessToken;
import com.onehilltech.gatekeeper.android.model.Account;
import com.onehilltech.gatekeeper.android.model.AccountProfile;
import com.onehilltech.gatekeeper.android.model.ClientToken;
import com.onehilltech.gatekeeper.android.model.ClientToken_Table;
import com.onehilltech.gatekeeper.android.model.UserToken;
import com.onehilltech.gatekeeper.android.model.UserToken_Table;
import com.raizlabs.android.dbflow.config.*;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.structure.database.transaction.QueryTransaction;

import java.lang.reflect.InvocationTargetException;

// There is a nasty bug that we cannot figure out. The only want to get the line below
// to actually import the database holder is to use wildcard import. Hopefully, we can
// remove the wildcard import statement when we have more time to investigate the problem.
//
//import com.raizlabs.android.dbflow.config.GatekeeperGeneratedDatabaseHolder;

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

  private static final int VERSION = 1;

  /**
   * OnRegistrationCompleteListener interface for initializing the client.
   */
  public interface Listener
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
    void onError (Throwable e);
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
   * @param listener   Callback for initialization
   *
   * @throws PackageManager.NameNotFoundException
   * @throws IllegalAccessException
   * @throws ClassNotFoundException
   * @throws InvocationTargetException
   */
  public static void initialize (Context context, Listener listener)
  {
    RequestQueue requestQueue = Volley.newRequestQueue (context);
    initialize (context, requestQueue, listener);
  }

  /**
   * Initialize a new GatekeeperClient object.
   *
   * @param context
   * @param requestQueue
   * @param listener
   * @return
   * @throws PackageManager.NameNotFoundException
   * @throws IllegalAccessException
   * @throws ClassNotFoundException
   * @throws InvocationTargetException
   */
  public static void initialize (Context context, RequestQueue requestQueue, Listener listener)
  {
    try
    {
      Configuration configuration = Configuration.loadFromMetadata (context);
      initialize (configuration, requestQueue, listener);
    }
    catch (PackageManager.NameNotFoundException | IllegalAccessException | InvocationTargetException | ClassNotFoundException e)
    {
      listener.onError (new RuntimeException ("Invalid or missing configuration", e));
    }
  }


  /**
   * Initialize a new GatekeeperClient object.
   *
   * @param config            Client configuration
   * @param requestQueue      Volley RequestQueue for requests
   * @param listener     Callback for initialization.
   */
  public static void initialize (final Configuration config, final RequestQueue requestQueue, final Listener listener)
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
              initialize (config, clientToken, requestQueue, listener);
            }
          }).execute ();
  }

  /**
   * Initialize a new GatekeeperClient object.
   *
   * @param config
   * @param clientToken
   * @param requestQueue
   * @param listener
   */
  private static void initialize (Configuration config,
                                  ClientToken clientToken,
                                  RequestQueue requestQueue,
                                  Listener listener)
  {
    if (clientToken != null)
      makeGatekeeperClient (config, clientToken, requestQueue, listener);
    else
      requestClientToken (config, requestQueue, listener);
  }

  /**
   * Request a new client token. Requesting a new client token will result a initializing
   * a new GatekeeperClient object.
   */
  private static void requestClientToken (final Configuration config,
                                          final RequestQueue requestQueue,
                                          final Listener listener)
  {
    // To initialize the client, we must first get a token for the client. This
    // allows us to determine if the client is enabled. It also setups the client
    // object with the required token.
    String url = config.baseUri + "/v" + VERSION + "/oauth2/token";

    JsonRequest<Token> request =
        new JsonRequest<> (
            Request.Method.POST,
            url,
            null,
            new TypeReference<Token> () { },
            new ResponseListener<Token> ()
            {
              @Override
              public void onErrorResponse (VolleyError error)
              {
                listener.onError (error);
              }

              @Override
              public void onResponse (Token response)
              {
                response.accept (new TokenVisitor ()
                {
                  @Override
                  public void visitBearerToken (BearerToken token)
                  {
                    // Set remaining properties on the bearer token, then save token to database.
                    Log.d (TAG, "Saving client token to database");
                    ClientToken clientToken = ClientToken.fromToken (config.clientId, token);
                    clientToken.save ();

                    makeGatekeeperClient (config, clientToken, requestQueue, listener);
                  }
                });
              }
            });

    ClientCredentials clientCredentials = new ClientCredentials ();
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
   * @param listener       Callback for initialization complete
   */
  private static void makeGatekeeperClient (Configuration config,
                                            ClientToken clientToken,
                                            RequestQueue requestQueue,
                                            Listener listener)
  {
    // Create a GatekeeperClient with the token.
    GatekeeperClient client =
        new GatekeeperClient (config.baseUri,
                              config.clientId,
                              clientToken,
                              requestQueue);

    listener.onInitialized (client);
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

  /**
   * Create a new account on the Gatekeeper server. The result of this method is a Boolean
   * value that determines if the account was created. Once the account has been created,
   * the application should request a token on behalf of the newly created user.
   *
   * @param username        Username
   * @param password        Password
   */
  public JsonRequest createAccount (String username, String password, String email, final OnResultListener <Boolean> listener)
  {
    class Data
    {
      @JsonProperty("client_id")
      public String clientId;

      public String username;

      public String password;

      public String email;
    }

    String url = this.getCompleteUrl ("/accounts");

    JsonRequest <Boolean> request =
        new JsonRequest<> (
            Request.Method.POST,
            url,
            this.clientToken_,
            new TypeReference<Boolean> () { },
            new ResponseListener<Boolean> ()
            {
              @Override
              public void onErrorResponse (VolleyError error)
              {
                listener.onError (error);
              }

              @Override
              public void onResponse (Boolean response)
              {
                listener.onResult (response);
              }
            });

    Data data = new Data ();
    data.clientId = this.clientId_;
    data.username = username;
    data.password = password;
    data.email = email;

    request.setData (data);

    this.requestQueue_.add (request);

    return request;
  }

  /**
   * Get the account profile for the user associated with the token.
   *
   * @param token
   * @param listener
   * @return
   */
  public JsonRequest getAccountProfile (UserToken token, ResponseListener <AccountProfile> listener)
  {
    String url = this.getCompleteUrl ("/me/profile");

    JsonRequest <AccountProfile> request =
        new JsonRequest<> (
            Request.Method.GET,
            url,
            token,
            new TypeReference <AccountProfile> () {},
            listener);

    this.requestQueue_.add (request);
    return request;
  }

  /**
   * Get an access token for the user.
   *
   * @param username        Username
   * @param password        Password
   * @param listener        Callback listener
   */
  public void getUserToken (final String username, final String password, final ResponseListener<UserToken> listener)
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
                UserCredentials userCredentials = new UserCredentials ();
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
  public JsonRequest refreshToken (UserToken token, ResponseListener <UserToken> listener)
  {
    if (!token.canRefresh ())
      throw new IllegalStateException ("Current token cannot be refreshed");

    RefreshToken refreshToken = new RefreshToken ();
    refreshToken.clientId = this.clientId_;
    refreshToken.refreshToken = token.getRefreshToken ();

    return this.requestUserToken (token.getUsername (), refreshToken, listener);
  }

  /**
   * Get a BearerToken for the grant information. The token can come from the database, if
   * it already exists, or it can come from the service.
   *
   * @param grant           Grant object
   * @param listener
   * @return
   */
  private JsonRequest requestUserToken (final String username,
                                        final Grant grant,
                                        final ResponseListener<UserToken> listener)
  {
    // Since we do not have this as a token, we must contact the server to get a
    // token for the grant.
    final String url = this.getCompleteUrl ("/oauth2/token");

    JsonRequest<Token> request =
        this.makeJsonRequest (
            Request.Method.POST,
            url,
            this.clientToken_,
            new TypeReference<Token> ()
            {
            },
            new ResponseListener<Token> ()
            {
              @Override
              public void onErrorResponse (VolleyError error)
              {
                listener.onErrorResponse (error);
              }

              @Override
              public void onResponse (Token response)
              {
                response.accept (new TokenVisitor ()
                {
                  @Override
                  public void visitBearerToken (BearerToken token)
                  {
                    completeUserLogin (username, token, listener);
                  }
                });
              }
            });

    request.setShouldCache (false);
    request.setData (grant);

    this.requestQueue_.add (request);

    return request;
  }

  /**
   * Complete the user login process.
   *
   * @param username
   * @param token
   * @param listener
   */
  private void completeUserLogin (String username, BearerToken token, ResponseListener<UserToken> listener)
  {
    // Save the token to the database.
    UserToken userToken = UserToken.fromToken (username, token);
    userToken.save ();

    // Keep a reference to the token, and call the listener.
    listener.onResponse (userToken);
  }

  /**
   * Get the id of the current user.
   *
   * @return
   */
  public JsonRequest whoami (UserToken token, ResponseListener <Account> listener)
  {
    String url = this.getCompleteUrl ("/me/whoami");

    JsonRequest request =
        this.makeJsonRequest (
            Request.Method.GET,
            url,
            token,
            new TypeReference <Account> () {},
            listener);

    this.requestQueue_.add (request);

    return request;
  }

  /**
   * Logout the current user.
   *
   * @param listener      Response listener
   */
  public JsonRequest logout (final UserToken token, final ResponseListener <Boolean> listener)
  {
    String url = this.getCompleteUrl ("/oauth2/logout");
    JsonRequest<Boolean> request =
        this.makeJsonRequest (
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
    this.requestQueue_.add (request);

    return request;
  }

  /**
   * Factory method for creating a protected request. The request will include the
   * current token in the HTTP request header.
   *
   * @param method        HTTP method
   * @param path          Full path of the resource
   * @param listener      Response listener
   * @param <T>           Object type of response body
   * @return              Request object
   */
  public <T> JsonRequest<T> makeJsonRequest (int method,
                                             String path,
                                             AccessToken token,
                                             TypeReference<T> typeReference,
                                             ResponseListener<T> listener)
  {
    return new JsonRequest<> (method, path, token, typeReference, listener);
  }

  /**
   * Add a request to the queue.
   *
   * @param request        Add request to queue
   */
  public void addRequest (Request <?> request)
  {
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
