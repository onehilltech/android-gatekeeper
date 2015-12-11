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
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.onehilltech.gatekeeper.android.GatekeeperClient;
import com.onehilltech.gatekeeper.android.JsonRequest;
import com.onehilltech.gatekeeper.android.ResponseListener;
import com.onehilltech.gatekeeper.android.SingleUserSessionClient;
import com.onehilltech.metadata.ManifestMetadata;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 * @class GcmRegistrationIntentService
 */
public class GcmRegistrationIntentService extends IntentService
{
  private static final String TAG = "GcmRegIntentService";

  private static final String EXTRA_AUTHORIZED_ENTITY = "extra_authorized_entity";
  private static final String EXTRA_EXTRAS = "extra_extras";

  public static final String METADATA_AUTHORIZED_ENTITY = "com.onehilltech.gatekeeper.android.authorized_entity";


  public GcmRegistrationIntentService ()
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
    Intent intent = new Intent (context, GcmRegistrationIntentService.class);
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
      // Get the GCM configuration, and register the token with the service.
      InstanceID instanceID = InstanceID.getInstance (this);
      final String gcmToken = instanceID.getToken (authorizedEntity, GoogleCloudMessaging.INSTANCE_ID_SCOPE, extras);

      this.registerTokenWithServer (gcmToken, new ResponseListener<Boolean> () {
        @Override
        public void onResponse (Boolean response)
        {
          Intent intent = new Intent (Intents.REGISTRATION_COMPLETE);
          intent.putExtra (Intents.EXTRA_TOKEN, gcmToken);

          // Broadcast the Gcm token to all that is listening. This is mainly the
          // GcmClient object.
          LocalBroadcastManager.getInstance (GcmRegistrationIntentService.this)
                               .sendBroadcast (intent);
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
    // Since the GatekeeperClient cannot be used across activity/service boundaries,
    // we need to initialize a new client. The initialization process must use the
    // properties defined in the metadata to initialize the client. This is because
    // there is no way to pass the client id, client secret, and base uri of the
    // service to this object.
    SingleUserSessionClient.initialize (this, new SingleUserSessionClient.OnInitializedListener ()
    {
      @Override
      public void onInitialized (SingleUserSessionClient sessionClient)
      {
        GatekeeperClient gatekeeperClient = sessionClient.getClient ();
        String url = gatekeeperClient.getCompleteUrl ("/me/notifications");

        JsonRequest<Boolean> request =
            sessionClient.makeJsonRequest (Request.Method.POST,
                                           url,
                                           new TypeReference<Boolean> () {},
                                           listener);

        class PostData
        {
          public PostData (String network, String token)
          {
            this.network = network;
            this.token = token;
          }

          public String network;
          public String token;
        }

        PostData data = new PostData ("gcm", token);
        request.setData (data);

        gatekeeperClient.addRequest (request);
      }

      @Override
      public void onError (Throwable t)
      {
        Log.e (TAG, "Gatekeeper initialization error; cannot update Google Cloud Messaging token");

      }
    });
  }
}
