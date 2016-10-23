package com.onehilltech.gatekeeper.android;

import com.onehilltech.gatekeeper.android.http.JsonBearerToken;
import com.onehilltech.gatekeeper.android.model.UserToken;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public abstract class RefreshTokenCallback <T> implements Callback <T>
{
  private static final String HEADER_WWW_AUTHENTICATE = "WWW-Authenticate";

  private SingleUserSessionClient sessionClient_;

  private boolean retried_ = false;

  public RefreshTokenCallback (SingleUserSessionClient client)
  {
    this.sessionClient_ = client;
  }

  @Override
  public final void onResponse (final Call<T> call, Response<T> response)
  {
    int statusCode = response.code ();

    if (statusCode == 401 && !this.retried_)
      this.refreshToken (call, response);
    else
      this.onHandleResponse (call, response);
  }

  private void refreshToken (final Call <T> origCall, final Response <T> origResp)
  {
    // refresh the token.
    UserToken userToken = this.sessionClient_.getUserToken ();

    // Mark the call as retried.
    this.retried_ = true;

    GatekeeperClient gatekeeper = this.sessionClient_.getGatekeeperClient ();
    Call <JsonBearerToken> refreshCall = gatekeeper.refreshToken (userToken.refreshToken);
    refreshCall.enqueue (new Impl (origCall, origResp));
  }

  public abstract void onHandleResponse (final Call<T> call, Response<T> response);

  class Impl implements Callback <JsonBearerToken>
  {
    private Call <T> origCall_;
    private Response <T> origResp_;

    Impl (Call <T> origCall, Response <T> origResp)
    {
      this.origCall_ = origCall;
      this.origResp_ = origResp;
    }

    @Override
    public void onFailure (Call<JsonBearerToken> call, Throwable t)
    {
      RefreshTokenCallback.this.onFailure (this.origCall_, t);
    }

    @Override
    public void onResponse (Call<JsonBearerToken> call, Response<JsonBearerToken> response)
    {
      if (response.isSuccessful ())
      {
        // Update the user token in the client.
        JsonBearerToken token = response.body ();
        sessionClient_.refreshToken (token);

        // Retry the call again using this callback object.
        Call <T> retryCall = this.origCall_.clone ();
        retryCall.enqueue (RefreshTokenCallback.this);
      }
      else
      {
        RefreshTokenCallback.this.onHandleResponse (this.origCall_, this.origResp_);
      }
    }
  }
}
