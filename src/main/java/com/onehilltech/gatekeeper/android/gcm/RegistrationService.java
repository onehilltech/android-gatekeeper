/**
 * Copyright 2015 Google Inc. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.onehilltech.gatekeeper.android.gcm;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.onehilltech.gatekeeper.android.GatekeeperClient;
import com.onehilltech.gatekeeper.android.JsonRequest;
import com.onehilltech.gatekeeper.android.ResponseListener;
import com.onehilltech.metadata.ManifestMetadata;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 * @class RegistrationService
 */
public class RegistrationService extends IntentService
{
  private static final String TAG = "RegistrationService";

  private static final String EXTRA_AUTHORIZED_ENTITY = "extra_authorized_entity";
  private static final String EXTRA_EXTRAS = "extra_extras";

  public static final String METADATA_AUTHORIZED_ENTITY = "com.onehilltech.gatekeeper.android.authorized_entity";


  public RegistrationService ()
  {
    super (TAG);
  }

  public static Intent newIntent (Context context)
      throws InvocationTargetException, PackageManager.NameNotFoundException, IllegalAccessException, ClassNotFoundException
  {
    ManifestMetadata metadata = ManifestMetadata.get (context);
    String authorizedEntity = metadata.getValue (METADATA_AUTHORIZED_ENTITY, true, String.class);

    return newIntent (context, authorizedEntity);
  }

  public static Intent newIntent (Context context, String authorizedEntity)
  {
    Intent intent = new Intent (context, RegistrationService.class);
    intent.putExtra (EXTRA_AUTHORIZED_ENTITY, authorizedEntity);

    return intent;
  }

  @Override
  protected void onHandleIntent (Intent intent)
  {
    if (!intent.hasExtra (EXTRA_AUTHORIZED_ENTITY))
      throw new IllegalArgumentException ("Intent is missing " + EXTRA_AUTHORIZED_ENTITY);

    String authorizedEntity = intent.getStringExtra (EXTRA_AUTHORIZED_ENTITY);
    Bundle extras = intent.hasExtra (EXTRA_EXTRAS) ? intent.getBundleExtra (EXTRA_EXTRAS) : null;

    try
    {
      // Request the GCM token from this instance.
      InstanceID instanceID = InstanceID.getInstance (this);
      String token = instanceID.getToken (authorizedEntity, GoogleCloudMessaging.INSTANCE_ID_SCOPE, extras);

      this.registerTokenWithServer (token, new ResponseListener<Boolean> () {
        @Override
        public void onResponse (Boolean response)
        {
          if (response)
            Log.d (TAG, "Registered gcm token with server");
          else
            Log.w (TAG, "Failed to register gcm token with server");
        }

        @Override
        public void onErrorResponse (VolleyError error)
        {
          Log.e (TAG, error.getMessage ());
        }
      });
    }
    catch (IOException e)
    {
      Log.e (TAG, e.getMessage (), e);
    }
  }

  /**
   * Persist registration to third-party servers.
   * <p>
   * Modify this method to associate the user's GCM registration token with any server-side account
   * maintained by your application.
   *
   * @param token The new token.
   */
  private void registerTokenWithServer (final String token, final ResponseListener <Boolean> listener)
  {
    try
    {
      // Since the GatekeeperClient cannot be used across activity/service boundaries,
      // we need to initialize a new client. The initialization process must use the
      // properties defined in the metadata to initialize the client. This is becuase
      // there is no way to pass the client id, client secret, and base uri of the
      // service to this object.

      GatekeeperClient.initialize (this, new GatekeeperClient.OnInitialized () {
        @Override
        public void onInitialized (GatekeeperClient client)
        {
          String url = client.getCompleteUrl ("/me/notifications");

          JsonRequest<Boolean> request =
              client.makeJsonRequest (Request.Method.POST,
                                      url,
                                      new TypeReference<Boolean> () {},
                                      listener);

          class Data
          {
            public Data (String network, String token)
            {
              this.network = network;
              this.token = token;
            }

            public String network;
            public String token;
          }

          Data data = new Data ("gcm", token);
          request.setData (data);

          client.addRequest (request);
        }

        @Override
        public void onError (VolleyError error)
        {
          Log.e (TAG, "Gatekeeper initialization error; cannot update Google Cloud Messaging token");
        }
      });
    }
    catch (Exception e)
    {
      Log.e (TAG, e.getMessage (), e);
    }
  }
}
