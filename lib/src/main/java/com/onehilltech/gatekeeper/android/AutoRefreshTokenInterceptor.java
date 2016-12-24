package com.onehilltech.gatekeeper.android;

import com.onehilltech.gatekeeper.android.http.JsonBearerToken;
import com.onehilltech.gatekeeper.android.model.UserToken;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Call;

/**
 * @class AutoRefreshTokenInterceptor
 *
 * Intercept that auto-refreshes the access token if it has expired. The client
 * will not know the token has been refreshed. If the token cannot be refreshed,
 * then the client will receive the original failure response.
 */
public class AutoRefreshTokenInterceptor implements Interceptor
{
  private SingleUserSessionClient session_;

  public AutoRefreshTokenInterceptor ()
  {

  }

  public AutoRefreshTokenInterceptor (SingleUserSessionClient session)
  {
    this.session_ = session;
  }

  public void setSession (SingleUserSessionClient session)
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
    UserToken userToken = this.session_.getUserToken ();

    if (userToken == null)
      return origResponse;

    GatekeeperClient gatekeeper = this.session_.getGatekeeperClient ();

    Call<JsonBearerToken> refreshCall = gatekeeper.refreshToken (userToken.refreshToken);
    retrofit2.Response<JsonBearerToken> refreshResponse = refreshCall.execute ();

    if (!refreshResponse.isSuccessful ())
      return origResponse;

    // Save the new user token, and retry the original call again.
    // Update the user token in the client.
    JsonBearerToken token = refreshResponse.body ();
    this.session_.refreshToken (token);

    // Retry the call again using this callback object.
    return chain.proceed (origRequest);
  }
}
