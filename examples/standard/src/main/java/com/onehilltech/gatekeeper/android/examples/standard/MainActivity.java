package com.onehilltech.gatekeeper.android.examples.standard;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.onehilltech.gatekeeper.android.SingleUserLoginActivity;
import com.onehilltech.gatekeeper.android.SingleUserSessionClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity
  implements SingleUserSessionClient.Listener
{
  private Button btnSignOut_;
  private SingleUserSessionClient sessionClient_;

  private static final String TAG = "MainActivity";

  @Override
  protected void onCreate (@Nullable Bundle savedInstanceState)
  {
    super.onCreate (savedInstanceState);

    this.setContentView (R.layout.activity_main);

    this.sessionClient_ = new SingleUserSessionClient.Builder (this).build ();
    this.sessionClient_.setListener (this);

    this.onViewCreated ();
  }

  @Override
  protected void onStart ()
  {
    super.onStart ();

    // Make sure the user it logged in.
    this.sessionClient_.checkLoggedIn (this, SingleUserLoginActivity.class);
  }

  private void onViewCreated ()
  {
    this.btnSignOut_ = (Button)this.findViewById (R.id.btn_signout);
    this.btnSignOut_.setOnClickListener (new View.OnClickListener ()
    {
      @Override
      public void onClick (View v)
      {
        onLogoutClicked ();
      }
    });

    this.btnSignOut_.setEnabled (this.sessionClient_.isLoggedIn ());
  }

  private void onLogoutClicked ()
  {
    this.sessionClient_.logout (new Callback<Boolean> ()
    {
      @Override
      public void onResponse (Call<Boolean> call, Response<Boolean> response)
      {
        if (response.isSuccessful () && response.body ())
          finish ();
      }

      @Override
      public void onFailure (Call<Boolean> call, Throwable t)
      {

      }
    });
  }

  @Override
  public void onLogin (SingleUserSessionClient client)
  {
    this.btnSignOut_.setEnabled (true);
  }

  @Override
  public void onLogout (SingleUserSessionClient client)
  {
    this.btnSignOut_.setEnabled (false);
  }
}
