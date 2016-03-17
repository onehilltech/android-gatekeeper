package com.onehilltech.gatekeeper.android.gcm;

import android.os.Bundle;

public interface GcmTopicMessageHandler
{
  void onMessage (GcmTopic topic, Bundle data);
}
