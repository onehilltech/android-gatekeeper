package com.onehilltech.gatekeeper.android;

import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;
import org.robolectric.shadows.httpclient.FakeHttpLayer;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.Map;

public abstract class EncodedPostBodyMatcher
    implements FakeHttpLayer.RequestMatcherBuilder.PostBodyMatcher
{
  @Override
  public final boolean matches (HttpEntity actualPostBody) throws IOException
  {
    String strData = EntityUtils.toString (actualPostBody);
    String decodedData = URLDecoder.decode (strData, "UTF-8");
    Map<String, String> postData = HttpBody.parseString (decodedData);

    return this.matches (postData);
  }

  public abstract boolean matches (Map <String, String> actualPostBody)
      throws IOException;
}
