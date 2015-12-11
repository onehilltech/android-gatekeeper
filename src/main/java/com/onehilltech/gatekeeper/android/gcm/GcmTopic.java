package com.onehilltech.gatekeeper.android.gcm;

/**
 * Created by hilljh on 12/10/15.
 */
public class GcmTopic
{
  private static final String TOPIC_PREFIX = "/topics/";

  private final String topicName_;

  private final String topicUri_;

  public static boolean isTopic (String topic)
  {
    return topic.startsWith (TOPIC_PREFIX);
  }

  public static GcmTopic parseTopic (String topic)
  {
    String topicName = topic.substring (TOPIC_PREFIX.length ());

    if (topicName.isEmpty ())
      throw new IllegalArgumentException ("Invalid topic string");

    return new GcmTopic (topicName);
  }

  public GcmTopic (String topicName)
  {
    this.topicName_ = topicName;
    this.topicUri_ = TOPIC_PREFIX + topicName;
  }

  public String getTopicName ()
  {
    return this.topicName_;
  }

  @Override
  public String toString ()
  {
    return this.topicName_;
  }

  public String getTopicUri ()
  {
    return this.topicUri_;
  }

  @Override
  public int hashCode ()
  {
    return this.topicUri_.hashCode ();
  }

  @Override
  public boolean equals (Object obj)
  {
    if (obj instanceof GcmTopic)
    {
      GcmTopic topic = (GcmTopic)obj;
      return this.topicName_.equals (topic.topicName_);
    }
    else if (obj instanceof String)
    {
      String topicName = (String)obj;
      return this.topicUri_.equals (topicName);
    }
    else
    {
      return false;
    }
  }
}
