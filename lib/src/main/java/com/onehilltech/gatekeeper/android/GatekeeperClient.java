package com.onehilltech.gatekeeper.android;

import android.content.Context;
import android.content.pm.PackageManager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
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
import com.onehilltech.metadata.ManifestMetadata;
import com.onehilltech.metadata.MetadataProperty;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * Client interface for communicating with a Gatekeeper service.
 */
public class GatekeeperClient
{
  /**
   * Gatekeeper client configuration. The configuration can be initialized manually
   * or loaded from the meta-data section in AndroidManifest.xml.
   */
  public static final class Configuration
  {
    private static final String CLIENT_ID = "com.onehilltech.gatekeeper.android.client_id";
    private static final String CLIENT_SECRET = "com.onehilltech.gatekeeper.android.client_secret";
    private static final String BASE_URI = "com.onehilltech.gatekeeper.android.baseuri";

    @MetadataProperty(name=CLIENT_ID, fromResource=true)
    public String clientId;

    @MetadataProperty(name=CLIENT_SECRET, fromResource=true)
    public String clientSecret;

    @MetadataProperty(name=BASE_URI, fromResource=true)
    public String baseUri;

    public static Configuration loadFromMetadata (Context context)
        throws PackageManager.NameNotFoundException, InvocationTargetException, IllegalAccessException, ClassNotFoundException
    {
      Configuration configuration = new Configuration ();
      ManifestMetadata.get (context).initFromMetadata (configuration);

      return configuration;
    }
  }

  /**
   * @class Builder
   *
   * Builder for creating GatekeeperClient objects.
   */
  public static final class Builder
  {
    private OkHttpClient httpClient_;

    private Configuration config_;

    private Context context_;

    public Builder (Context context)
    {
      this.context_ = context;
    }

    public Builder setClient (OkHttpClient httpClient)
    {
      this.httpClient_ = httpClient;
      return this;
    }

    public Builder setConfiguration (Configuration config)
    {
      this.config_ = config;
      return this;
    }

    public GatekeeperClient build ()
    {
      try
      {
        Configuration config = this.config_;

        if (config == null)
          config = Configuration.loadFromMetadata (this.context_);

        OkHttpClient httpClient = this.httpClient_;

        if (httpClient == null)
          httpClient = new OkHttpClient.Builder ().build ();

        return new GatekeeperClient (config, httpClient);
      }
      catch (PackageManager.NameNotFoundException | IllegalAccessException | ClassNotFoundException | InvocationTargetException e)
      {
        throw new IllegalStateException ("Failed to load default configuration", e);
      }
    }
  }

  public static final int VERSION = 1;

  private final Retrofit retrofit_;

  private final OkHttpClient httpClient_;

  private final Service service_;

  private Gson gson_;

  /// Configuration for the client.
  private Configuration config_;

  private ClientToken clientToken_;

  private ResourceEndpoint <JsonAccount> accounts_;

  static
  {
    Resource.registerType ("account", new TypeToken<JsonAccount> () {}.getType ());
    Resource.registerType ("accounts", new TypeToken<List<JsonAccount>> () {}.getType ());
  }

  /**
   * Interceptor that adds the Authorization header to a request.
   */
  private final Interceptor authHeaderInterceptor_ = new Interceptor ()
  {
    @Override
    public Response intercept (Chain chain) throws IOException
    {
      String authorization = "Bearer " + clientToken_.accessToken;

      okhttp3.Request original = chain.request ();
      okhttp3.Request request =
          original.newBuilder ()
                  .header ("User-Agent", "FundAll Android")
                  .header ("Authorization", authorization)
                  .method (original.method (), original.body ())
                  .build ();

      return chain.proceed (request);
    }
  };
  /**
   * Initializing constructor.
   *
   * @param config
   * @param httpClient
   */
  GatekeeperClient (Configuration config, OkHttpClient httpClient)
  {
    this.config_ = config;
    this.httpClient_ = httpClient;

    // Initialize the type factories for Gson.
    RuntimeTypeAdapterFactory <JsonGrant> grantTypes =
        RuntimeTypeAdapterFactory.of (JsonGrant.class, "grant_type")
                                 .registerSubtype (JsonClientCredentials.class, "client_credentials")
                                 .registerSubtype (JsonPassword.class, "password")
                                 .registerSubtype (JsonRefreshToken.class, "refresh_token");

    // Initialize the Retrofit.
    ResourceMarshaller resourceMarshaller = new ResourceMarshaller ();

    this.gson_ =
        new GsonBuilder ()
            .registerTypeAdapter (Resource.class, resourceMarshaller)
            .registerTypeAdapterFactory (grantTypes)
            .create ();

    resourceMarshaller.setGson (this.gson_);

    this.retrofit_ =
        new Retrofit.Builder ()
            .baseUrl (this.getBaseUrlWithVersion ())
            .addConverterFactory (GsonConverterFactory.create (this.gson_))
            .client (this.httpClient_)
            .build ();

    // Create the remoting endpoints.
    this.service_ = this.retrofit_.create (Service.class);

    // Initialize the authenticated endpoints.
    this.initAuthEndpoints ();
  }

  private void initAuthEndpoints ()
  {
    OkHttpClient authHttpClient =
        this.httpClient_.newBuilder ()
                  .addInterceptor (this.authHeaderInterceptor_)
                  .build ();

    Retrofit authRetrofit =
        new Retrofit.Builder ()
            .baseUrl (this.getBaseUrlWithVersion ())
            .addConverterFactory (GsonConverterFactory.create (this.gson_))
            .client (authHttpClient)
            .build ();

    this.accounts_ = ResourceEndpoint.get (authRetrofit, "account", "accounts");
  }

  /**
   * Get the http client for the client.
   *
   * @return        OkHttpClient object
   */
  public OkHttpClient getHttpClient ()
  {
    return this.httpClient_;
  }

  /**
   * Get the Gson object used by the client.
   *
   * @return         Gson object
   */
  public Gson getGson ()
  {
    return this.gson_;
  }

  /**
   * Get the client id.
   *
   * @return      Client id string
   * */
  public String getClientId ()
  {
    return this.config_.clientId;
  }

  /**
   * Compute the complete URL relative to the versioned Gatekeeper URL.
   *
   * @param relativePath      Relative path
   * @return                  URL object
   */
  public URL computeCompleteUrl (String relativePath)
    throws MalformedURLException
  {
    return new URL (this.getBaseUrlWithVersion () + relativePath);
  }

  public String getBaseUrl ()
  {
    return this.config_.baseUri;
  }

  public String getBaseUrlWithVersion ()
  {
    return this.getBaseUrl () + "v" + VERSION + "/";
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
    grant.clientId = this.config_.clientId;
    grant.username = username;
    grant.password = password;

    return this.getToken (grant);
  }

  /**
   * Get an access token for this client.
   *
   * @return
   */
  public Call <JsonBearerToken> getClientToken ()
  {
    JsonClientCredentials credentials = new JsonClientCredentials ();
    credentials.clientId = this.config_.clientId;
    credentials.clientSecret = this.config_.clientSecret;

    return this.getToken (credentials);
  }

  /**
   * Refresh an existing token.
   *
   * @param refreshToken        Refresh token
   * @return
   */
  public Call <JsonBearerToken> refreshToken (String refreshToken)
  {
    JsonRefreshToken grant = new JsonRefreshToken ();
    grant.clientId = this.config_.clientId;
    grant.refreshToken = refreshToken;

    return this.getToken (grant);
  }

  /**
   * Create a new account.
   *
   * @param username
   * @param password
   * @param email
   * @param callback
   */
  public void createAccount (final String username,
                             final String password,
                             final String email,
                             final Callback<Resource> callback)
  {
    this.getClientToken ().enqueue (new Callback<JsonBearerToken> ()
    {
      @Override
      public void onResponse (Call<JsonBearerToken> call, retrofit2.Response<JsonBearerToken> response)
      {
        // Save the client token. We are either going to replace the current one, or
        // insert a new one into the database.
        clientToken_ = ClientToken.fromToken (config_.clientId, response.body ());
        clientToken_.save ();

        // Make a call to get the
        JsonAccount account = new JsonAccount ();
        account.username = username;
        account.password = password;
        account.email = email;

        accounts_.create (account).enqueue (callback);
      }

      @Override
      public void onFailure (Call<JsonBearerToken> call, Throwable t)
      {
        callback.onFailure (null, t) ;
      }
    });
  }

  /**
   * Helper method for requesting an access token.
   *
   * @param grantType     JsonGrant object
   * @return
   */
  private Call <JsonBearerToken> getToken (JsonGrant grantType)
  {
    return this.service_.getBearerToken (grantType);
  }

  private interface Service
  {
    @POST("oauth2/token")
    Call<JsonBearerToken> getBearerToken (@Body JsonGrant grant);
  }
}
