package com.onehilltech.gatekeeper.android;

import android.content.Context;
import android.content.pm.PackageManager;

import com.onehilltech.metadata.ManifestMetadata;
import com.onehilltech.metadata.MetadataProperty;

import java.lang.reflect.InvocationTargetException;

/**
 * Gatekeeper client configuration. The configuration can be initialized manually
 * or loaded from the meta-data section in AndroidManifest.xml.
 */
public class Configuration
{
  public static final String CLIENT_ID = "com.onehilltech.gatekeeper.android.client_id";
  public static final String CLIENT_SECRET = "com.onehilltech.gatekeeper.android.client_secret";
  public static final String BASE_URI = "com.onehilltech.gatekeeper.android.baseuri";

  @MetadataProperty(name=CLIENT_ID, fromResource=true)
  public String clientId;

  @MetadataProperty(name=CLIENT_SECRET, fromResource=true)
  public String clientSecret;

  @MetadataProperty(name=BASE_URI, fromResource=true)
  public String baseUri;

  /**
   * Load the configuration from meta-data in AndroidManifest.xml
   *
   * @param context
   * @return
   * @throws PackageManager.NameNotFoundException
   * @throws IllegalAccessException
   * @throws ClassNotFoundException
   * @throws InvocationTargetException
   */
  public static Configuration loadFromMetadata (Context context)
      throws PackageManager.NameNotFoundException,
      IllegalAccessException,
      ClassNotFoundException,
      InvocationTargetException
  {
    Configuration config = new Configuration ();
    ManifestMetadata.get (context).initFromMetadata (config);

    return config;
  }

  public String getCompleteUrl (String path)
  {
    return this.baseUri + path;
  }
}
