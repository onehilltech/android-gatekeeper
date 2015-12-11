package com.onehilltech.gatekeeper.android.gcm;

import android.os.Bundle;

import com.google.android.gms.gcm.GcmPubSub;

import java.util.HashMap;

final class MessageDispatcher
{
  private static MessageDispatcher instance_;

  private GcmPubSub gcmPubSub_;

  private String gcmToken_;

  private final HashMap <GcmTopic, GcmTopicMessageHandler> messageHandlers_ = new HashMap<> ();

  public static MessageDispatcher getInstance ()
  {
    if (instance_ != null)
      return instance_;

    instance_ = new MessageDispatcher ();
    return instance_;
  }

  private MessageDispatcher ()
  {
  }

  /**
   * Dispatch a received message to a GcmTopicMessageHandler.
   *
   * @param from
   * @param data
   */
  public void dispatchMessage (String from, Bundle data)
  {
    if (GcmTopic.isTopic (from))
    {
      GcmTopic topic = GcmTopic.parseTopic (from);
      GcmTopicMessageHandler handler = this.messageHandlers_.get (topic);

      if (handler != null)
        handler.onMessage (topic, data);
    }
  }

  public void addHandler (GcmTopic topic, GcmTopicMessageHandler handler)
  {
    this.messageHandlers_.put (topic, handler);
  }
}
