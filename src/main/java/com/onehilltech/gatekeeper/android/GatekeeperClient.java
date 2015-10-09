package com.onehilltech.gatekeeper.android;

import android.content.Context;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;

import java.util.HashMap;
import java.util.Map;

/**
 * @class GatekeeperClient
 */
public class GatekeeperClient
{
  /// Logging tag.
  private static final String TAG = GatekeeperClient.class.getSimpleName ();

  /// Client id.
  private final String clientId_;

  /// Client secret.
  private final String clientSecret_;

  /// Base URI for the Gatekeeper service.
  private final String baseUri_;

  /// Underlying HTTP client.
  private final RequestQueue requestQueue_;

  /// Authorization token for the current user.
  private BearerToken token_;

  public interface ResultListener <T>
  {
    void onResponse (T result);

    void onError (int statusCode, String response);
  }

  /**
   * Initializing constructor.
   *
   * @param clientId
   * @param baseUri
   */
  GatekeeperClient (Context context, String baseUri, String clientId, String clientSecret)
  {
    this (baseUri, clientId, clientSecret, Volley.newRequestQueue (context));
  }

  /**
   * Package level constructor. Allows customization of the internal HTTP client.
   *
   * @param baseUri
   * @param clientId
   * @param clientSecret
   */
  GatekeeperClient (String baseUri, String clientId, String clientSecret, RequestQueue requestQueue)
  {
    this.baseUri_ = baseUri;
    this.clientId_ = clientId;
    this.clientSecret_ = clientSecret;
    this.requestQueue_ = requestQueue;
  }

  /**
   * Get the id of the client.
   *
   * @return
   */
  public String getClientId ()
  {
    return this.clientId_;
  }

  /**
   * Get the secret for the client.
   *
   * @return
   */
  public String getClientSecret ()
  {
    return this.clientSecret_;
  }

  /**
   * Get the baseuri for the Gatekeeper service.
   *
   * @return
   */
  public String getBaseUri ()
  {
    return this.baseUri_;
  }

  /**
   * Set the token used by the client.
   *
   * @param token
   */
  public void setToken (BearerToken token)
  {
    this.token_ = token;
  }

  /**
   * Get the token used by the client.
   *
   * @return
   */
  public BearerToken getToken ()
  {
    return this.token_;
  }

  /**
   * Test if the client has a token.
   *
   * @return
   */
  public boolean hasToken ()
  {
    return this.token_ != null;
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
                             final ResultListener <Boolean> result)
  {
    // First, get a client token. We need a client token to create the account
    // for the user.
    this.getClientToken (new ResultListener<BearerToken> ()
    {
      @Override
      public void onResponse (BearerToken token)
      {
        createAccountImpl (username, password, email, result);
      }

      @Override
      public void onError (int statusCode, String responseString)
      {
        result.onError (statusCode, responseString);
      }
    });
  }

  /**
   * Implementation for creating the account. This is called once the client has
   * obtained a client token. The client should then get a token for the new user.
   *
   * @param username
   * @param password
   * @param email
   * @param result
   */
  private void createAccountImpl (String username, String password, String email, final ResultListener <Boolean> result)
  {
    String url = this.getCompleteUrl ("/accounts");

    GatekeeperRequest <Boolean> request =
        this.makeRequest (Request.Method.POST, url, Boolean.class,
            new Response.Listener<Boolean> () {
              @Override
              public void onResponse (Boolean response)
              {
                result.onResponse (response);
              }
            },
            new Response.ErrorListener ()
            {
              @Override
              public void onErrorResponse (VolleyError error)
              {
                NetworkResponse response = error.networkResponse;

                if (response != null)
                  result.onError (response.statusCode, new String (response.data));
              }
            });

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
   * @param result
   */
  public void getUserToken (String username, String password, ResultListener <BearerToken> result)
  {
    HashMap <String, String> params = new HashMap<> ();

    params.put ("grant_type", "password");
    params.put ("client_id", this.clientId_);
    params.put ("username", username);
    params.put ("password", password);

    this.getToken (params, result);
  }

  /**
   * Get an access token for the client. The \a clientSecret parameter is optional.
   *
   * @param result
   */
  public void getClientToken (ResultListener <BearerToken> result)
  {
    if (this.clientSecret_ == null)
      throw new IllegalStateException ("Must provide client secret to request token");

    HashMap <String, String> params = new HashMap <> ();
    params.put ("grant_type", "client_credentials");
    params.put ("client_id", this.clientId_);
    params.put ("client_secret", this.clientSecret_);

    this.getToken (params, result);
  }

  /**
   * Refresh the current access token.
   *
   * @param result
   */
  public void refreshToken (ResultListener <BearerToken> result)
  {
    if (!this.token_.canRefresh ())
      throw new IllegalStateException ("Current token cannot be refreshed");

    HashMap <String, String> params = new HashMap <> ();
    params.put ("grant_type", "refresh_token");
    params.put ("client_id", this.clientId_);
    params.put ("refresh_token", this.token_.getRefreshToken ());

    this.getToken (params, result);
  }

  /**
   * Helper method that gets the token using the provided body params.
   *
   * @param params
   */
  private void getToken (final Map<String, String> params, final ResultListener <BearerToken> result)
  {
    final String url = this.getCompleteUrl ("/oauth2/token");


    GatekeeperRequest <Token> request =
        this.makeRequest (Request.Method.POST, url, Token.class,
            new Response.Listener<Token> ()
            {
              @Override
              public void onResponse (Token response)
              {
                response.accept (new TokenVisitor ()
                {
                  @Override
                  public void visitBearerToken (BearerToken token)
                  {
                    setToken (token);
                    result.onResponse (token);
                  }
                });
              }
            },
            new Response.ErrorListener () {
              @Override
              public void onErrorResponse (VolleyError error)
              {

              }
            });

    request.addParams (params);

    this.requestQueue_.add (request);
  }

  /**
   * Logout the current user.
   *
   * @param result
   * @param result
   */
  public void logout (final ResultListener <Boolean> result)
  {
    String url = this.getCompleteUrl ("/oauth2/logout");

    GatekeeperRequest <Boolean> request =
        this.makeRequest (Request.Method.GET, url, Boolean.class,
            new Response.Listener <Boolean> () {
              @Override
              public void onResponse (Boolean response)
              {
                result.onResponse (response);
              }
            },
            new Response.ErrorListener () {
              @Override
              public void onErrorResponse (VolleyError error)
              {
                NetworkResponse response = error.networkResponse;

                if (response != null)
                  result.onError (response.statusCode, new String (response.data));
              }
            });

    this.requestQueue_.add (request);
  }

  /**
   * Make a new GatekeeperRequest object.
   *
   * @param method
   * @param path
   * @param clazz
   * @param responseListener
   * @param errorListener
   * @param <T>
   * @return
   */
  public <T> GatekeeperRequest <T> makeRequest (int method,
                                                String path,
                                                Class <T> clazz,
                                                Response.Listener <T> responseListener,
                                                Response.ErrorListener errorListener)
  {
    GatekeeperRequest <T> request =
        new GatekeeperRequest<> (
            method,
            this.getCompleteUrl (path),
            clazz,
            this.token_,
            responseListener,
            errorListener);

    return request;
  }

  /**
   * Add a GatekeeperRequest object to the queue.
   *
   * @param request
   */
  public void addRequest (GatekeeperRequest <?> request)
  {
    this.requestQueue_.add (request);
  }

  /**
   * Get the complete URL for a path.
   *
   * @param relativePath
   * @return
   */
  private String getCompleteUrl (String relativePath)
  {
    return this.baseUri_ + relativePath;
  }
}
