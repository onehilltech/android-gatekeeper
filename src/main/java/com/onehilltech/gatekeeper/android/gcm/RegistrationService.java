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
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.android.volley.Request;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.onehilltech.gatekeeper.android.Gatekeeper;
import com.onehilltech.gatekeeper.android.GatekeeperClient;
import com.onehilltech.gatekeeper.android.ProtectedRequest;
import com.onehilltech.gatekeeper.android.ResponseListener;

import java.io.IOException;

/**
 * @class RegistrationService
 */
public class RegistrationService extends IntentService
{
  private static final String TAG = "RegistrationService";

  public static final String EXTRA_AUTHORIZED_ENTITY = "extra_authorized_entity";
  public static final String EXTRA_EXTRAS = "extra_extras";

  public RegistrationService ()
  {
    super (TAG);
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

      this.registerTokenWithServer (token, new ResponseListener<Boolean> ());
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
  private void registerTokenWithServer (String token, final ResponseListener <Boolean> listener)
  {
    GatekeeperClient client = Gatekeeper.getClient ();
    ProtectedRequest<Boolean> request = client.makeRequest (Request.Method.POST, "/me/notifications", listener);

    request
        .addParam ("network", "gcm")
        .addParam ("token", token);

    client.addRequest (request);
  }
}
