package com.android.volley;

/**
 * Created by hilljh on 11/5/15.
 */
public class BasicResponseDelivery implements ResponseDelivery
{
  @Override
  public void postResponse (Request<?> request, Response<?> response) {
    postResponse (request, response, null);
  }

  @Override
  public void postResponse (Request<?> request, Response<?> response, Runnable runnable)
  {
    request.markDelivered ();
    request.addMarker ("post-response");

    this.execute (request, response);
  }

  @Override
  public void postError(Request<?> request, VolleyError error) {
    request.addMarker ("post-error");
    Response<?> response = Response.error(error);

    this.execute (request, response);
  }

  private void execute (Request request, Response response)
  {
    // If this request has canceled, finish it and don't deliver.
    if (request.isCanceled ())
    {
      request.finish ("canceled-at-delivery");
      return;
    }

    // Deliver a normal response or error, depending.
    if (response.isSuccess())
      request.deliverResponse (response.result);
     else
      request.deliverError (response.error);

    // If this is an intermediate response, add a marker, otherwise we're done
    // and the request can be finished.
    if (response.intermediate)
      request.addMarker("intermediate-response");
    else
      request.finish("done");
  }
}
