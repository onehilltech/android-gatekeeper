package com.onehilltech.gatekeeper.android.utils;

import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;

import java.lang.ref.WeakReference;
import java.util.HashMap;

/**
 * Utility class getting user-friendly error messages.
 */
public class ErrorMessageUtil
{
  /**
   * Internal interface for translating VolleyError objects to
   * error messages.
   */
  private interface VolleyErrorHandler
  {
    String getErrorMessage (VolleyError error);
  }

  /// Singleton instance as a weak reference.
  private static WeakReference <ErrorMessageUtil> instance_;

  /// Mapping of VolleyError types to message generators.
  private final HashMap <Class <? extends VolleyError>, VolleyErrorHandler> volleyHandlers_ = new HashMap<> ();

  /**
   * Get the singleton instance.
   *
   * @return
   */
  public static ErrorMessageUtil instance ()
  {
    if (instance_ != null && instance_.get () != null)
      return instance_.get ();

    ErrorMessageUtil util = new ErrorMessageUtil ();
    instance_ = new WeakReference<> (util);

    return util;
  }

  /**
   * Default constructor
   */
  private ErrorMessageUtil ()
  {
    volleyHandlers_.put (TimeoutError.class, new VolleyErrorHandler ()
    {
      @Override
      public String getErrorMessage (VolleyError error)
      {
        return "Network request timed out";
      }
    });

    volleyHandlers_.put (NoConnectionError.class, new VolleyErrorHandler ()
    {
      @Override
      public String getErrorMessage (VolleyError error)
      {
        return "Cannot connect to server; check network connection exists";
      }
    });
  }

  /**
   * Get the error message from an exception.
   *
   * @param error
   * @return
   */
  public String getErrorMessage (VolleyError error)
  {
    VolleyErrorHandler errorHandler = this.volleyHandlers_.get (error);

    if (errorHandler != null)
      return errorHandler.getErrorMessage (error);

    if (error.networkResponse != null)
      return getNetworkResponse (error.networkResponse);

    return error.getLocalizedMessage ();
  }

  /**
   * Get the network response.
   *
   * @param response
   * @return
   */
  private static String getNetworkResponse (NetworkResponse response)
  {
    return new String (response.data);
  }
}
