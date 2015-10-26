package com.onehilltech.gatekeeper.android;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.onehilltech.metadata.ManifestMetadata;
import com.onehilltech.metadata.MetadataProperty;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

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

  /// Authorization token for the current user.
  private BearerToken userToken_;

  private BearerToken clientToken_;

  public interface OnInitialized
  {
    void onInitialized (GatekeeperClient client);
    void onInitializeFailed ();
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
    String clientId;

    @MetadataProperty(name=CLIENT_SECRET, fromResource=true)
    String clientSecret;

    @MetadataProperty(name=BASE_URI, fromResource=true)
    String baseUri;

    @MetadataProperty(name=BASE_URI_EMULATOR, fromResource=true)
    String getBaseUriEmulator;

    /**
     * Get the correct base uri based on where the application is running. This will return
     * either baseUri or getBaseUriEmulator.
     *
     * @param context
     * @return
     */
    public String getBaseUri (Context context)
    {
      return Build.PRODUCT.startsWith ("sdk_google") ? this.getBaseUriEmulator : this.baseUri;
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
  public static ProtectedRequest <Token> initialize (Context context, OnInitialized onInitialized)
      throws PackageManager.NameNotFoundException,
      IllegalAccessException,
      ClassNotFoundException,
      InvocationTargetException
  {
    Options options = new Options ();
    ManifestMetadata.get (context).initFromMetadata (options);

    return initialize (context, options, onInitialized);
  }

  /**
   * Initialize a new GatekeeperClient.
   *
   * @param context           Target context
   * @param options           Initialization options
   * @param onInitialized     Initialization callback
   */
  public static ProtectedRequest <Token> initialize (Context context,
                                                     Options options,
                                                     OnInitialized onInitialized)
  {
    RequestQueue requestQueue = Volley.newRequestQueue (context);
    return initialize (context, options, requestQueue, onInitialized);
  }

  /**
   * Initialize a new GatekeeperClient object.
   *
   * @param options           Initialization options
   * @param requestQueue      Volley RequestQueue for requests
   * @param onInitialized     Callback for initialization.
   */
  public static ProtectedRequest <Token> initialize (Context context,
                                                     final Options options,
                                                     final RequestQueue requestQueue,
                                                     final OnInitialized onInitialized)
  {
    // To initialize the client, we must first get a token for the client. This
    // allows us to determine if the client is enabled. It also setups the client
    // object with the required token.
    String url = options.getBaseUri (context) + "/oauth2/token";

    ProtectedRequest <Token> request =
        new ProtectedRequest<> (
            Request.Method.POST,
            url,
            null,
            new ResponseListener <Token> () {
              @Override
              public void onErrorResponse (VolleyError error)
              {
                Log.e (TAG, error.getMessage (), error.getCause ());
                onInitialized.onInitializeFailed ();
              }

              @Override
              public void onResponse (Token response)
              {
                response.accept (new TokenVisitor () {
                  @Override
                  public void visitBearerToken (BearerToken token)
                  {
                    GatekeeperClient client =
                        new GatekeeperClient (
                            options.baseUri,
                            options.clientId,
                            token,
                            requestQueue);

                    onInitialized.onInitialized (client);
                  }
                });
              }
            });

    request
        .addParam ("grant_type", "client_credentials")
        .addParam ("client_id", options.clientId)
        .addParam ("client_secret", options.clientSecret);

    requestQueue.add (request);

    return request;
  }

  /**
   * Initializing constructor.
   *
   * @param baseUri
   * @param clientToken
   */
  GatekeeperClient (String baseUri,
                    String clientId,
                    BearerToken clientToken,
                    RequestQueue requestQueue)
  {
    this.baseUri_ = baseUri;
    this.clientId_ = clientId;
    this.clientToken_ = clientToken;
    this.requestQueue_ = requestQueue;
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
   * Test if the client has a token.
   *
   * @return
   */
  public boolean isLoggedIn ()
  {
    return this.userToken_ != null;
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
   * @param username
   * @param password
   */
  public void createAccount (final String username,
                             final String password,
                             final String email,
                             final ResponseListener<Boolean> listener)
  {
    String url = this.getCompleteUrl ("/accounts");

    ProtectedRequest <Boolean> request =
        new ProtectedRequest<> (
            Request.Method.POST,
            url,
            this.clientToken_,
            listener);

    request
        .addParam ("client_id", this.clientId_)
        .addParam ("username", username)
        .addParam ("password", password)
        .addParam ("email", email);

    this.requestQueue_.add (request);
  }

  /**
   * Get an access token for the user.
   *
   * @param username
   * @param password
   * @param listener
   */
  public ProtectedRequest<Token> getUserToken (String username,
                                               String password,
                                               ResponseListener<BearerToken> listener)
  {
    HashMap <String, String> params = new HashMap<> ();

    params.put ("grant_type", "password");
    params.put ("client_id", this.clientId_);
    params.put ("username", username);
    params.put ("password", password);

    return this.getToken (params, listener);
  }

  /**
   * Refresh the current access token.
   *
   * @param listener
   */
  public ProtectedRequest<Token> refreshToken (ResponseListener <BearerToken> listener)
  {
    if (!this.userToken_.canRefresh ())
      throw new IllegalStateException ("Current token cannot be refreshed");

    HashMap <String, String> params = new HashMap <> ();
    params.put ("grant_type", "refresh_token");
    params.put ("client_id", this.clientId_);
    params.put ("refresh_token", this.userToken_.getRefreshToken ());

    return this.getToken (params, listener);
  }

  /**
   * Helper method that gets the token using the provided body params.
   *
   * @param params
   */
  private ProtectedRequest<Token> getToken (final Map<String, String> params, final ResponseListener<BearerToken> listener)
  {
    final String url = this.getCompleteUrl ("/oauth2/token");

    ProtectedRequest<Token> request =
        this.makeRequest (Request.Method.POST, url, new ResponseListener<Token> () {
          @Override
          public void onErrorResponse (VolleyError error)
          {
            listener.onErrorResponse (error);
          }

          @Override
          public void onResponse (Token response)
          {
            response.accept (new TokenVisitor () {
              @Override
              public void visitBearerToken (BearerToken token)
              {
                userToken_ = token;
                listener.onResponse (token);
              }
            });
          }
        });

    request.addParams (params);
    this.requestQueue_.add (request);

    return request;
  }

  /**
   * Logout the current user.
   *
   * @param listener      Response listener
   */
  public void logout (ResponseListener <Boolean> listener)
  {
    String url = this.getCompleteUrl ("/oauth2/logout");
    ProtectedRequest<Boolean> request = this.makeRequest (Request.Method.GET, url, listener);

    this.requestQueue_.add (request);
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
  public <T> ProtectedRequest<T> makeRequest (int method, String path, ResponseListener <T> listener)
  {
    return new ProtectedRequest<> (method, path, this.userToken_, listener);
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
  private String getCompleteUrl (String relativePath)
  {
    return this.baseUri_ + relativePath;
  }
}
