package com.onehilltech.gatekeeper.android;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by hillj on 9/17/15.
 */
public class HttpBody
{
  public static Map<String, String> parseString (String bodyStr)
  {
    HashMap <String, String> entries = new HashMap <String, String> ();
    String [] keyValues = bodyStr.split ("&");

    for (String keyValue : keyValues)
    {
      String [] pair = keyValue.split ("=");
      entries.put (pair[0], pair[1]);
    }

    return entries;
  }
}
