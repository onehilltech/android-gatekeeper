package com.onehilltech.gatekeeper.android;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
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
import com.onehilltech.gatekeeper.android.model.ClientToken;
import com.onehilltech.gatekeeper.android.model.ClientToken$Table;
import com.onehilltech.gatekeeper.android.model.UserToken;
import com.onehilltech.gatekeeper.android.model.UserToken$Table;
import com.onehilltech.metadata.ManifestMetadata;
import com.onehilltech.metadata.MetadataProperty;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.language.SQLite;

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

  /// DBFlow module name for Gatekeeper.
  private static final String DBFLOW_MODULE_NAME = "Gatekeeper";

  /**
   * OnRegistrationCompleteListener interface for initializing the client.
   */
  public interface OnInitialized
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
     * @param error       Error object
     */
    void onError (VolleyError error);
  }

  public interface OnResultListener <T>
  {
    void onResult (T result);
    void onError (VolleyError e);
  }

  /**
   * Configuration options for the GatekeeperClient. The options can be loaded
   * from the AndroidManifest.xml.
   */
  public static class Options
  {
    public static final String CLIENT_ID = "com.onehilltech.gatekeeper.android.client_id";
    public static final String CLIENT_SECRET = "com.onehilltech.gatekeeper.android.client_secret";
    public static final String BASE_URI = "com.onehilltech.gatekeeper.android.baseuri";
    public static final String BASE_URI_EMULATOR = "com.onehilltech.gatekeeper.android.baseuri_emulator";

    @MetadataProperty(name=CLIENT_ID, fromResource=true)
    public String clientId;

    @MetadataProperty(name=CLIENT_SECRET, fromResource=true)
    public String clientSecret;

    @MetadataProperty(name=BASE_URI, fromResource=true)
    public String baseUri;

    @MetadataProperty(name=BASE_URI_EMULATOR, fromResource=true)
    public String baseUriEmulator;

    /**
     * Get the correct base uri based on where the application is running. This will return
     * either baseUri or baseUriEmulator.
     *
     * @return      Base uri
     */
    public String getBaseUri ()
    {
      boolean isEmulator = Build.PRODUCT.startsWith ("sdk_google");
      return isEmulator ? this.baseUriEmulator : this.baseUri;
    }
  }

  /**
   * Initialize a new GatekeeperClient using information from the metadata in
   * AndroidManifest.xml.
   *
   * @param context         Target context
   * @param onInitialized   Callback for initialization
   *
   * @throws PackageManager.NameNotFoundException
   * @throws IllegalAccessException
   * @throws ClassNotFoundException
   * @throws InvocationTargetException
   */
  public static JsonRequest<Token> initialize (Context context, OnInitialized onInitialized)
      throws PackageManager.NameNotFoundException,
      IllegalAccessException,
      ClassNotFoundException,
      InvocationTargetException
  {
    RequestQueue requestQueue = Volley.newRequestQueue (context);
    return initialize (context, requestQueue, onInitialized);
  }

  public static JsonRequest<Token> initialize (Context context,
                                               RequestQueue requestQueue,
                                               OnInitialized onInitialized)
      throws PackageManager.NameNotFoundException,
      IllegalAccessException,
      ClassNotFoundException,
      InvocationTargetException
  {
    Options options = new Options ();
    ManifestMetadata.get (context).initFromMetadata (options);

    return initialize (options, requestQueue, onInitialized);
  }

  /**
   * Initialize a new GatekeeperClient object.
   *
   * @param options           Initialization options
   * @param requestQueue      Volley RequestQueue for requests
   * @param onInitialized     Callback for initialization.
   */
  public static JsonRequest<Token> initialize (final Options options,
                                               final RequestQueue requestQueue,
                                               final OnInitialized onInitialized)
  {
    // First, initialize the Gatekeeper DBFlow module.
    FlowManager.initModule (DBFLOW_MODULE_NAME);

    // Check if the client already has a token stored in the database.
    ClientToken clientToken =
        SQLite.select ()
              .from (ClientToken.class)
              .where (ClientToken$Table.client_id.eq (options.clientId))
              .querySingle ();

    if (clientToken != null)
    {
      makeGatekeeperClient (options, clientToken, requestQueue, onInitialized);
      return null;
    }

    // To initialize the client, we must first get a token for the client. This
    // allows us to determine if the client is enabled. It also setups the client
    // object with the required token.
    final String baseUri = options.getBaseUri ();
    String url = baseUri + "/oauth2/token";

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
                onInitialized.onError (error);
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
                    ClientToken clientToken = ClientToken.fromToken (options.clientId, token);
                    clientToken.save ();

                    makeGatekeeperClient (options, clientToken, requestQueue, onInitialized);
                  }
                });
              }
            });

    ClientCredentials clientCredentials = new ClientCredentials ();
    clientCredentials.clientId = options.clientId;
    clientCredentials.clientSecret = options.clientSecret;

    request.setData (clientCredentials);
    request.setShouldCache (false);

    requestQueue.add (request);

    return request;
  }

  /**
   * Helper method to make the GatekeeperClient object, and call the onRegistrationComplete
   * callback with the client.
   *
   * @param options
   * @param clientToken
   * @param requestQueue
   * @param onInitialized
   */
  private static void makeGatekeeperClient (Options options,
                                            ClientToken clientToken,
                                            RequestQueue requestQueue,
                                            OnInitialized onInitialized)
  {
    // Create a GatekeeperClient with the token.
    GatekeeperClient client =
        new GatekeeperClient (
            options.getBaseUri (),
            options.clientId,
            clientToken,
            requestQueue);

    onInitialized.onInitialized (client);
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
   * Get an access token for the user.
   *
   * @param username        Username
   * @param password        Password
   * @param listener        Callback listener
   */
  public JsonRequest getUserToken (String username, String password, ResponseListener<UserToken> listener)
  {
    // Check if the token already exists in the database.
    UserToken userToken =
        SQLite.select ()
              .from (UserToken.class)
              .where (UserToken$Table.username.eq (username))
              .querySingle ();

    if (userToken != null)
    {
      listener.onResponse (userToken);
      return null;
    }

    UserCredentials userCredentials = new UserCredentials ();
    userCredentials.clientId = this.clientId_;
    userCredentials.username = username;
    userCredentials.password = password;

    return this.requestUserToken (username, userCredentials, listener);
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
    String relativeUrl = "/me/whoami";
    String url = this.getCompleteUrl (relativeUrl);

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
    return this.baseUri_ + relativePath;
  }
}
