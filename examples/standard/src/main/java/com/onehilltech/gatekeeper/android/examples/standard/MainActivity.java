package com.onehilltech.gatekeeper.android.examples.standard;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.onehilltech.gatekeeper.android.GatekeeperSessionClient;
import com.onehilltech.gatekeeper.android.GatekeeperSignInActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity
  implements GatekeeperSessionClient.Listener
{
  private Button btnSignOut_;
  private GatekeeperSessionClient session_;

  private static final String TAG = "MainActivity";

  @Override
  protected void onCreate (@Nullable Bundle savedInstanceState)
  {
    super.onCreate (savedInstanceState);

    this.setContentView (R.layout.activity_main);

    this.session_ = new GatekeeperSessionClient.Builder (this).build ();
    this.session_.setListener (this);

    this.onViewCreated ();
  }

  @Override
  protected void onStart ()
  {
    super.onStart ();

    // Make sure the user it logged in.
    this.session_.ensureSignedIn (this, GatekeeperSignInActivity.class);
  }

  private void onViewCreated ()
  {
    this.btnSignOut_ = (Button)this.findViewById (R.id.btn_signout);
    this.btnSignOut_.setEnabled (this.session_.isSignedIn ());
    this.btnSignOut_.setOnClickListener (new View.OnClickListener ()
    {
      @Override
      public void onClick (View v)
      {
        onLogoutClicked ();
      }
    });
  }

  private void onLogoutClicked ()
  {
    this.session_.signOut (new Callback<Boolean> ()
    {
      @Override
      public void onResponse (Call<Boolean> call, Response<Boolean> response)
      {
        if (response.isSuccessful () && response.body ())
          session_.ensureSignedIn (MainActivity.this, GatekeeperSignInActivity.class);
      }

      @Override
      public void onFailure (Call<Boolean> call, Throwable t)
      {

      }
    });
  }

  @Override
  public void onSignedIn (GatekeeperSessionClient client)
  {
    this.btnSignOut_.setEnabled (true);
  }

  @Override
  public void onSignedOut (GatekeeperSessionClient client)
  {
    this.btnSignOut_.setEnabled (false);
  }
}
