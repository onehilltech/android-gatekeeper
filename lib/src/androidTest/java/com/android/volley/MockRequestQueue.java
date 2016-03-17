package com.android.volley;

import com.android.volley.toolbox.NoCache;

public class MockRequestQueue extends RequestQueue
{
  private final MockNetwork network_;

  public static final int DEFAULT_TRHEADPOOL_SIZE = 1;

  public static MockRequestQueue newInstance ()
  {
    return newInstance (new NoCache ());
  }

  public static MockRequestQueue newInstance (Cache cache)
  {
    MockNetwork mockNetwork = new MockNetwork ();
    return new MockRequestQueue (cache, mockNetwork);
  }

  private MockRequestQueue (Cache cache, MockNetwork mockNetwork)
  {
    super (cache, mockNetwork, DEFAULT_TRHEADPOOL_SIZE, new BasicResponseDelivery ());

    this.network_ = mockNetwork;
  }

  public MockNetwork getMockNetwork ()
  {
    return this.network_;
  }
}
