package com.onehilltech.gatekeeper.android;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CallbackProxy <T> implements Callback <T>
{
  private Callback <T> delegate_;

  public CallbackProxy (Callback <T> delegate)
  {
    this.delegate_ = delegate;
  }

  @Override
  public void onFailure (Call<T> call, Throwable t)
  {
    this.delegate_.onFailure (call, t);
  }

  @Override
  public void onResponse (Call<T> call, Response<T> response)
  {
    this.delegate_.onResponse (call, response);
  }
}
