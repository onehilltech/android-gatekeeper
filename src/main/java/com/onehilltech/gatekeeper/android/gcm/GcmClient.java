package com.onehilltech.gatekeeper.android.gcm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import com.google.android.gms.gcm.GcmPubSub;
import com.google.android.gms.iid.InstanceID;
import com.raizlabs.android.dbflow.annotation.NotNull;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GcmClient implements Closeable
{
  public interface OnRegistrationCompleteListener
  {
    void onRegistrationComplete (GcmClient gcmClient);
  }

  public interface OnSubscriptionCompleteListener
  {
    void onSubscriptionCompleteListener (GcmClient gcmClient, GcmTopic topic);
    void onSubscriptionError (GcmClient gcmClient, Throwable t);
  }

  private static final String METADATA_AUTHORIZED_ENTITY = "com.onehilltech.gatekeeper.android.authorized_entity";

  private static final String TAG = "GcmClient";

  private final GcmPubSub gcmPubSub_;

  private String gcmToken_;

  private final InstanceID instanceID_;

  private final LocalBroadcastManager localBroadcastManager_;

  private final OnRegistrationCompleteListener onRegistrationCompleteListener_;

  private final ExecutorService executor_;

  private BroadcastReceiver broadcastReceiver_ = new BroadcastReceiver ()
  {
    @Override
    public void onReceive (Context context, Intent intent)
    {
      if (intent.getAction ().equals (Intents.REGISTRATION_COMPLETE))
      {
        gcmToken_ = intent.getStringExtra (Intents.EXTRA_TOKEN);
        onRegistrationCompleteListener_.onRegistrationComplete (GcmClient.this);
      }
    }
  };

  /**
   * Initialize a GcmClient object.
   *
   * @param context
   * @param onRegistrationCompleteListener
   */
  public static void initialize (@NotNull Context context, @NotNull OnRegistrationCompleteListener onRegistrationCompleteListener)
      throws PackageManager.NameNotFoundException, ClassNotFoundException, InvocationTargetException, IllegalAccessException
  {
    new GcmClient (context, onRegistrationCompleteListener);
  }

  /**
   * Internal constructor used by the initialize () methods.
   *
   * @param context
   * @param onRegistrationCompleteListener
   */
  private GcmClient (final Context context, OnRegistrationCompleteListener onRegistrationCompleteListener)
    throws PackageManager.NameNotFoundException, ClassNotFoundException, InvocationTargetException, IllegalAccessException
  {
    this.instanceID_ = InstanceID.getInstance (context);
    this.gcmPubSub_ = GcmPubSub.getInstance (context);
    this.executor_ = Executors.newCachedThreadPool ();

    // Setup the receiver for local broadcasts.
    this.onRegistrationCompleteListener_ = onRegistrationCompleteListener;
    this.localBroadcastManager_ = LocalBroadcastManager.getInstance (context);
    this.localBroadcastManager_.registerReceiver (this.broadcastReceiver_, new IntentFilter (Intents.REGISTRATION_COMPLETE));

    try
    {
      context.startService (GcmRegistrationIntentService.newIntent (context));
    }
    catch (PackageManager.NameNotFoundException | InvocationTargetException | IllegalAccessException | ClassNotFoundException  e)
    {
      throw new RuntimeException ("Failed to start registration service", e);
    }
  }

  @Override
  public void close ()
  {
    this.localBroadcastManager_.unregisterReceiver (this.broadcastReceiver_);
  }

  /**
   * Subscribe to a topic. The GcmTopicMessageHandler will receive notifications when
   * messages on the topic is received.
   *
   * @param topic
   * @param handler
   * @param extras
   * @throws IOException
   */
  public void subscribeTo (@NotNull final GcmTopic topic,
                           @NotNull final GcmTopicMessageHandler handler,
                           final Bundle extras,
                           final OnSubscriptionCompleteListener listener)
  {
    this.executor_.execute (new Runnable () {
      @Override
      public void run ()
      {
        try
        {
          gcmPubSub_.subscribe (gcmToken_, topic.getTopicUri (), extras);
          MessageDispatcher.getInstance ().addHandler (topic, handler);

          if (listener != null)
            listener.onSubscriptionCompleteListener (GcmClient.this, topic);
        }
        catch (Exception e)
        {
          if (listener != null)
            listener.onSubscriptionError (GcmClient.this, e);
        }
      }
    });
  }
}
