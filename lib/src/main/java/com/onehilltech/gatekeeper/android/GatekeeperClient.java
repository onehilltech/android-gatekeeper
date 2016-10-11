package com.onehilltech.gatekeeper.android;

import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import com.onehilltech.gatekeeper.android.http.JsonAccount;
import com.onehilltech.gatekeeper.android.http.JsonBearerToken;
import com.onehilltech.gatekeeper.android.http.JsonClientCredentials;
import com.onehilltech.gatekeeper.android.http.JsonGrant;
import com.onehilltech.gatekeeper.android.http.JsonPassword;
import com.onehilltech.gatekeeper.android.http.JsonRefreshToken;
import com.onehilltech.gatekeeper.android.http.jsonapi.Resource;
import com.onehilltech.gatekeeper.android.http.jsonapi.ResourceEndpoint;
import com.onehilltech.gatekeeper.android.http.jsonapi.ResourceMarshaller;
import com.onehilltech.gatekeeper.android.model.ClientToken;
import com.onehilltech.gatekeeper.android.model.ClientToken_Table;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.config.*;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.structure.database.transaction.QueryTransaction;

import java.lang.reflect.InvocationTargetException;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * Client interface for communicating with a Gatekeeper service.
 */
public class GatekeeperClient
{
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

  private interface Service
  {
    @POST("oauth2/token")
    Call<JsonBearerToken> getBearerToken (@Body JsonGrant grant);
  }


  public static final int VERSION = 1;

  /// Logging tag.
  private static final String TAG = "GatekeeperClient";

  /// Base URI for the Gatekeeper service.
  private final String baseUri_;

  /// Authorization token for the client.
  private ClientToken clientToken_;

  private final Retrofit retrofit_;

  private final OkHttpClient httpClient_;

  private final Service service_;

  private final ResourceEndpoint <JsonAccount> accounts_;

  private Gson gson_;

  /**
   * Initialize a new GatekeeperClient using information from the metadata in
   * AndroidManifest.xml.
   *
   * @param context         Target context
   * @param onInitializedListener   Callback for initialization
   */
  public static void initialize (Context context, OkHttpClient httpClient, OnInitializedListener onInitializedListener)
  {
    try
    {
      Configuration configuration = Configuration.loadFromMetadata (context);
      initialize (configuration, httpClient, onInitializedListener);
    }
    catch (PackageManager.NameNotFoundException | IllegalAccessException | InvocationTargetException | ClassNotFoundException e)
    {
      onInitializedListener.onInitializeFailed (new RuntimeException ("Invalid or missing configuration", e));
    }
  }

  /**
   * Initialize a new GatekeeperClient object.
   *
   * @param config                  Client configuration
   * @param httpClient              Volley RequestQueue for requests
   * @param onInitializedListener   Callback for initialization.
   */
  public static void initialize (final Configuration config,
                                 final OkHttpClient httpClient,
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
              initialize (config, clientToken, httpClient, onInitializedListener);
            }
          }).execute ();
  }

  /**
   * Initialize a new GatekeeperClient object.
   *
   * @param config
   * @param clientToken
   * @param httpClient
   * @param onInitializedListener
   */
  private static void initialize (Configuration config,
                                  ClientToken clientToken,
                                  OkHttpClient httpClient,
                                  OnInitializedListener onInitializedListener)
  {
    if (clientToken != null)
      makeGatekeeperClient (config, clientToken, httpClient, onInitializedListener);
    else
      requestClientToken (config, httpClient, onInitializedListener);
  }

  /**
   * Request a new client token. Requesting a new client token will result a initializing
   * a new GatekeeperClient object.
   */
  private static void requestClientToken (final Configuration config,
                                          final OkHttpClient httpClient,
                                          final OnInitializedListener onInitializedListener)
  {
    // Create a client that does not have a token.
    final GatekeeperClient client = new GatekeeperClient (config.baseUri, null, httpClient);

    // Request a new token from the server for the client.
    JsonClientCredentials clientCredentials = new JsonClientCredentials ();
    clientCredentials.clientId = config.clientId;
    clientCredentials.clientSecret = config.clientSecret;

    Call <JsonBearerToken> call = client.getToken (clientCredentials);
    call.enqueue (new Callback<JsonBearerToken> ()
    {
      @Override
      public void onResponse (Call<JsonBearerToken> call, Response<JsonBearerToken> response)
      {
        // Store the response in the client, and save the token to the database.
        client.clientToken_ = ClientToken.fromToken (config.clientId, response.body ());
        client.clientToken_.save ();

        onInitializedListener.onInitialized (client);
      }

      @Override
      public void onFailure (Call<JsonBearerToken> call, Throwable t)
      {
        onInitializedListener.onInitializeFailed (t);
      }
    });
  }

  /**
   * Helper method to make the GatekeeperClient object, and call the onRegistrationComplete
   * callback with the client.
   *
   * @param config                    Client configuration
   * @param httpClient                Request queue for sending request to server
   * @param onInitializedListener     Callback for initialization complete
   */
  private static void makeGatekeeperClient (Configuration config,
                                            ClientToken clientToken,
                                            OkHttpClient httpClient,
                                            OnInitializedListener onInitializedListener)
  {
    GatekeeperClient client = new GatekeeperClient (config.baseUri, clientToken, httpClient);
    onInitializedListener.onInitialized (client);
  }

  /**
   * Initializing constructor.
   *
   * @param baseUri
   */
  GatekeeperClient (String baseUri, ClientToken clientToken, OkHttpClient httpClient)
  {
    this.baseUri_ = baseUri;
    this.httpClient_ = httpClient;
    this.clientToken_ = clientToken;

    // Initialize the Retrofit.
    ResourceMarshaller resourceMarshaller = new ResourceMarshaller ();

    RuntimeTypeAdapterFactory <JsonGrant> grantTypes =
        RuntimeTypeAdapterFactory.of (JsonGrant.class, "grant_type")
                                 .registerSubtype (JsonClientCredentials.class, "client_credentials")
                                 .registerSubtype (JsonPassword.class, "password")
                                 .registerSubtype (JsonRefreshToken.class, "refresh_token");

    this.gson_ =
        new GsonBuilder ()
            .registerTypeAdapter (Resource.class, resourceMarshaller)
            .registerTypeAdapterFactory (grantTypes)
            .create ();

    resourceMarshaller.setGson (this.gson_);

    this.retrofit_ = new Retrofit.Builder ()
        .baseUrl (this.getBaseUrlWithVersion ())
        .addConverterFactory (GsonConverterFactory.create (this.gson_))
        .client (this.httpClient_)
        .build ();

    this.accounts_ = ResourceEndpoint.get (this.retrofit_, "account", "accounts");
    this.service_ = this.retrofit_.create (Service.class);
  }

  public OkHttpClient getHttpClient ()
  {
    return this.httpClient_;
  }

  public Gson getGson ()
  {
    return this.gson_;
  }

  public String getClientId ()
  {
    return this.clientToken_.getClientId ();
  }

  public String getCompleteUrl (String relativePath)
  {
    return this.baseUri_ + "/v" + VERSION + relativePath;
  }

  public String getBaseUrl ()
  {
    return this.baseUri_;
  }

  public String getBaseUrlWithVersion ()
  {
    return this.baseUri_ + "v" + VERSION + "/";
  }

  /**
   * Get the client token.
   *
   * @return
   */
  public ClientToken getClientToken ()
  {
    return this.clientToken_;
  }

  /**
   * Create a new account on the Gatekeeper server. The result of this method is a Boolean
   * value that determines if the account was created. Once the account has been created,
   * the application should request a token on behalf of the newly created user.
   *
   * @param username        Username
   * @param password        Password
   */
  public Call <Resource> createAccount (String username, String password, String email)
  {
    JsonAccount account = new JsonAccount ();
    account.username = username;
    account.password = password;
    account.email = email;

    return this.accounts_.create (account);
  }

  /**
   * Get an access token for the user.
   *
   * @param username        Username
   * @param password        Password
   */
  public Call<JsonBearerToken> getUserToken (String username, String password)
  {
    JsonPassword grant = new JsonPassword ();
    grant.clientId = this.clientToken_.getClientId ();
    grant.username = username;
    grant.password = password;

    return this.getToken (grant);
  }

  public Call <JsonBearerToken> refreshToken (String refreshToken)
  {
    JsonRefreshToken grant = new JsonRefreshToken ();
    grant.clientId = this.clientToken_.getClientId ();
    grant.refreshToken = refreshToken;

    return this.getToken (grant);
  }

  private Call <JsonBearerToken> getToken (JsonGrant grantType)
  {
    return this.service_.getBearerToken (grantType);
  }
}
