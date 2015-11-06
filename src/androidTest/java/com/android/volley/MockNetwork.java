package com.android.volley;

import java.util.HashSet;

public class MockNetwork implements Network
{
  private HashSet<RequestMatcher> matchers_ = new HashSet<> ();

  public interface RequestMatcher
  {
    boolean matches (Request <?> request);
    NetworkResponse getNetworkResponse (Request <?> request) throws VolleyError;
  }

  public void addMatcher (RequestMatcher matcher)
  {
    this.matchers_.add (matcher);
  }

  public void clearMatchers ()
  {
    this.matchers_.clear ();
  }

  public void reset ()
  {
    this.clearMatchers ();
  }

  @Override
  public NetworkResponse performRequest (Request<?> request) throws VolleyError
  {
    for (RequestMatcher matcher : this.matchers_)
    {
      if (matcher.matches (request))
        return matcher.getNetworkResponse (request);
    }

    throw new VolleyError ("Unexpected request");
  }
}
