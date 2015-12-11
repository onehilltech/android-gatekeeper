package com.onehilltech.gatekeeper.android.gcm;

import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;

public final class GcmMessageService extends GcmListenerService
{
  private static final String TAG = "GcmMessageService";

  @Override
  public void onMessageReceived (String from, Bundle data)
  {
    Log.d (TAG, "onMessageReceived () called");
    MessageDispatcher.getInstance ().dispatchMessage (from, data);
  }

  @Override
  public void onDeletedMessages ()
  {

  }

  @Override
  public void onMessageSent (String msgId)
  {

  }

  @Override
  public void onSendError (String msgId, String error)
  {

  }
}
