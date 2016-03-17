/**
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.onehilltech.gatekeeper.android.gcm;

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.iid.InstanceIDListenerService;

/**
 * Utility service that handles GCM refreshing the token. This service uses the authorized
 * entity defined the metadata section of AndroidManifest.xml to register the token with
 * the Gatekeeper service.
 *
 * The value of the authorized entity must be defined as a string resource. It cannot be
 * defined directly as a raw value in AndroidManifest.xml. Doing so will result in a handled
 * exception, but the token will not be registered.
 *
 * Using this service is optional. You can manually handle token refreshes using your own
 * service. In order to refresh the token with the Gatekeeper service, you must manually
 * start the GcmRegistrationIntentService intent.
 */
public class GcmInstanceIDService extends InstanceIDListenerService
{
  private static final String TAG = "GcmInstanceIDService";

  @Override
  public void onTokenRefresh ()
  {
    Log.d (TAG, "Refreshing Google Cloud Messaging token");

    try
    {
      Intent intent = GcmRegistrationIntentService.newIntent (this);
      this.startService (intent);
    }
    catch (Exception ex)
    {
      Log.e (TAG, ex.getMessage (), ex);
    }
  }
}
