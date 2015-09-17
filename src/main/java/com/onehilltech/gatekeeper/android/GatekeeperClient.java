package com.onehilltech.gatekeeper.android;

import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;
import org.json.JSONException;
import org.json.JSONObject;

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
  private final AsyncHttpClient client_ = new AsyncHttpClient ();

  /// Authorization token for the current user.
  private BearerToken token_;

  public interface ResultListener <T>
  {
    void onSuccess (int statusCode, T result);

    void onFailure (int statusCode, String responseString, Throwable throwable);
  }

  /**
   * @class TokenResultListener
   *
   * Internal class that handles processing the results of a token request. If
   * the received token is valid, it will replace the existing one.
   */
  private class TokenResultListener extends JsonHttpResponseHandler
  {
    private final ResultListener <BearerToken> result_;

    public TokenResultListener (ResultListener <BearerToken> result)
    {
      this.result_ = result;
    }

    @Override
    public void onSuccess (int statusCode, Header[] headers, JSONObject response)
    {
      try
      {
        // Store the token, and return it via the result listener.
        BearerToken token = BearerToken.fromJSONObject (response);
        setToken (token);

        this.result_.onSuccess (statusCode, token);
      }
      catch (JSONException e)
      {
        Log.e (TAG, e.getMessage (), e);
      }
    }

    @Override
    public void onFailure (int statusCode, Header[] headers, String responseString, Throwable throwable)
    {
      this.result_.onFailure (statusCode, responseString, throwable);
    }
  }

  /**
   * Initializing constructor.
   *
   * @param clientId
   * @param baseUri
   */
  public GatekeeperClient (String baseUri, String clientId, String clientSecret)
  {
    this.baseUri_ = baseUri;
    this.clientId_ = clientId;
    this.clientSecret_ = clientSecret;
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
    this.client_.addHeader ("Authorization", "Bearer " + token.getAccessToken ());
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
      public void onSuccess (int statusCode, BearerToken token)
      {
        createAccountImpl (username, password, email, result);
      }

      @Override
      public void onFailure (int statusCode, String responseString, Throwable throwable)
      {
        result.onFailure (statusCode, responseString, throwable);
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

    RequestParams params = new RequestParams ();
    params.put ("client_id", this.clientId_);
    params.put ("username", username);
    params.put ("password", password);
    params.put ("email", email);

    this.client_.post (url, params, new JsonHttpResponseHandler () {
      @Override
      public void onFailure (int statusCode, Header[] headers, String responseString, Throwable throwable)
      {
        result.onFailure (statusCode, responseString, throwable);
      }

      @Override
      public void onSuccess (int statusCode, Header[] headers, String responseString)
      {
        result.onSuccess (statusCode, Boolean.parseBoolean (responseString));
      }
    });
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
    String url = this.getCompleteUrl ("/oauth2/token");

    RequestParams params = new RequestParams ();
    params.put ("grant_type", "password");
    params.put ("client_id", this.clientId_);
    params.put ("username", username);
    params.put ("password", password);

    this.client_.post (url, params, new TokenResultListener (result));
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

    String url = this.getCompleteUrl ("/oauth2/token");

    RequestParams params = new RequestParams ();
    params.put ("grant_type", "client_credentials");
    params.put ("client_id", this.clientId_);
    params.put ("client_secret", this.clientSecret_);

    this.client_.post (url, params, new TokenResultListener (result));
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

    String url = this.getCompleteUrl ("/oauth2/token");

    RequestParams params = new RequestParams ();
    params.put ("grant_type", "refresh_token");
    params.put ("client_id", this.clientId_);
    params.put ("refresh_token", this.token_.getRefreshToken ());

    this.client_.post (url, params, new TokenResultListener (result));
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

    this.client_.get (url, new JsonHttpResponseHandler () {
      @Override
      public void onFailure (int statusCode, Header[] headers, String responseString, Throwable throwable)
      {
        result.onFailure (statusCode, responseString, throwable);
      }

      @Override
      public void onSuccess (int statusCode, Header[] headers, String responseString)
      {
        result.onSuccess (statusCode, Boolean.parseBoolean (responseString));
      }
    });
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
