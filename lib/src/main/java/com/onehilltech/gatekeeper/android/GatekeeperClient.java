package com.onehilltech.gatekeeper.android;

import android.content.Context;
import android.content.pm.PackageManager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import com.onehilltech.backbone.data.ResourceSerializer;
import com.onehilltech.backbone.http.HttpError;
import com.onehilltech.backbone.http.Resource;
import com.onehilltech.gatekeeper.android.http.JsonBearerToken;
import com.onehilltech.gatekeeper.android.http.JsonClientCredentials;
import com.onehilltech.gatekeeper.android.http.JsonGrant;
import com.onehilltech.gatekeeper.android.http.JsonPassword;
import com.onehilltech.gatekeeper.android.http.JsonRefreshToken;
import com.onehilltech.metadata.ManifestMetadata;
import com.onehilltech.metadata.MetadataProperty;
import com.onehilltech.promises.Promise;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Converter;
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

    /**
     * Load the configuration from metadata.
     *
     * @param context
     * @return
     * @throws PackageManager.NameNotFoundException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     */
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

        return new GatekeeperClient (this.context_, config, httpClient);
      }
      catch (PackageManager.NameNotFoundException | IllegalAccessException | ClassNotFoundException | InvocationTargetException e)
      {
        throw new IllegalStateException ("Failed to load default configuration", e);
      }
    }
  }

  public static final int VERSION = 1;

  private final Context context_;

  private final Retrofit retrofit_;

  private final OkHttpClient httpClient_;

  private final Service service_;

  private Gson gson_;

  /// Configuration for the client.
  private Configuration config_;

  private final Converter<ResponseBody, Resource> resourceConverter_;

  /**
   * Initializing constructor.
   *
   * @param config
   * @param httpClient
   */
  GatekeeperClient (Context context, Configuration config, OkHttpClient httpClient)
  {
    this.context_ = context;
    this.config_ = config;
    this.httpClient_ = httpClient;

    // Initialize the type factories for Gson.
    RuntimeTypeAdapterFactory <JsonGrant> grantTypes =
        RuntimeTypeAdapterFactory.of (JsonGrant.class, "grant_type")
                                 .registerSubtype (JsonClientCredentials.class, "client_credentials")
                                 .registerSubtype (JsonPassword.class, "password")
                                 .registerSubtype (JsonRefreshToken.class, "refresh_token");

    // Initialize the Retrofit.
    ResourceSerializer serializer = new ResourceSerializer.Builder ().build ();

    this.gson_ =
        new GsonBuilder ()
            .registerTypeAdapter (Resource.class, serializer)
            .registerTypeAdapterFactory (grantTypes)
            .create ();

    serializer.setGson (this.gson_);

    this.retrofit_ =
        new Retrofit.Builder ()
            .baseUrl (this.getBaseUrlWithVersion ())
            .addConverterFactory (GsonConverterFactory.create (this.gson_))
            .client (this.httpClient_)
            .build ();

    this.resourceConverter_ = this.retrofit_.responseBodyConverter (Resource.class, new Annotation[0]);

    // Create the remoting endpoints.
    this.service_ = this.retrofit_.create (Service.class);
  }

  /**
   * Create a new Retrofit object for this GatekeeperClient.
   *
   * @return      Retrofit object
   */
  public Retrofit newRetrofit ()
  {
    return new Retrofit.Builder ().baseUrl (this.getBaseUrlWithVersion ())
                                  .addConverterFactory (GsonConverterFactory.create (this.gson_))
                                  .client (this.httpClient_)
                                  .build ();
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
  public Promise<JsonBearerToken> getUserToken (String username, String password)
  {
    JsonPassword grant = new JsonPassword ();
    grant.username = username;
    grant.password = password;

    return this.getToken (grant);
  }

  /**
   * Get an access token for this client.
   */
  public Promise <JsonBearerToken> getClientToken ()
  {
    JsonClientCredentials credentials = new JsonClientCredentials ();
    return this.getToken (credentials);
  }

  /**
   * Refresh an existing token.
   *
   * @param refreshToken        Refresh token
   */
  public Promise<JsonBearerToken> refreshToken (String refreshToken)
  {
    JsonRefreshToken grant = new JsonRefreshToken ();
    grant.refreshToken = refreshToken;

    return this.getToken (grant);
  }

  Call <JsonBearerToken> refreshTokenSync (String refreshToken)
  {
    JsonRefreshToken grant = new JsonRefreshToken ();
    grant.refreshToken = refreshToken;
    grant.clientId = this.config_.clientId;
    grant.clientSecret = this.config_.clientSecret;
    grant.packageName = this.context_.getPackageName ();

    return this.service_.getBearerToken (grant);
  }

  /**
   * Helper method for requesting an access token.
   *
   * @param grantType     JsonGrant object
   */
  private Promise<JsonBearerToken> getToken (JsonGrant grantType)
  {
    return new Promise<> ((settlement) -> {
      grantType.clientId = this.config_.clientId;
      grantType.clientSecret = this.config_.clientSecret;
      grantType.packageName = this.context_.getPackageName ();

      this.service_.getBearerToken (grantType).enqueue (new Callback<JsonBearerToken> ()
      {
        @Override
        public void onResponse (Call<JsonBearerToken> call, Response<JsonBearerToken> response)
        {
          if (response.isSuccessful ())
          {
            settlement.resolve (response.body ());
          }
          else
          {
            try
            {
              HttpError error = getError (response.errorBody ());
              error.setStatusCode (response.code ());

              settlement.reject (error);
            }
            catch (IOException e)
            {
              settlement.reject (e);
            }
          }
        }

        @Override
        public void onFailure (Call<JsonBearerToken> call, Throwable t)
        {
          settlement.reject (t);
        }
      });
    });
  }

  /**
   * Get the HttpError from the ResponseBody.
   *
   * @param errorBody
   * @return
   * @throws IOException
   */
  public HttpError getError (ResponseBody errorBody)
      throws IOException
  {
    Resource resource = this.resourceConverter_.convert (errorBody);
    return resource.get ("errors");
  }

  private interface Service
  {
    @POST("oauth2/token")
    Call<JsonBearerToken> getBearerToken (@Body JsonGrant grant);
  }
}
