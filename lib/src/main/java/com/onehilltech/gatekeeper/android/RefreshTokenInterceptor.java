package com.onehilltech.gatekeeper.android;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * @class RefreshTokenInterceptor
 *
 * Intercept that auto-refreshes the access token if it has expired. The client
 * will not know the token has been refreshed. If the token cannot be refreshed,
 * then the client will receive the original failure response.
 */
public class RefreshTokenInterceptor implements Interceptor
{
  private GatekeeperSessionClient session_;

  public RefreshTokenInterceptor ()
  {

  }

  public void setSession (GatekeeperSessionClient session)
  {
    this.session_ = session;
  }

  @Override
  public Response intercept (Chain chain) throws IOException
  {
    // Proceed with the original request. Check the status code for the response.
    // If the status code is 401, then we need to refresh the token. Otherwise,
    // we return control to the next interceptor.

    Request origRequest = chain.request ();
    Response origResponse = chain.proceed (origRequest);

    int statusCode = origResponse.code ();

    if (statusCode != 401)
      return origResponse;

    // Let's try to update the original token. If the response is not successful,
    // the return the original response.
    if (!this.session_.refreshToken ())
      return origResponse;

    // Retry the call again using this callback object.
    return chain.proceed (origRequest);
  }
}
