package com.onehilltech.gatekeeper.android.examples.standard;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.TextView;

import com.onehilltech.backbone.data.HttpError;
import com.onehilltech.gatekeeper.android.GatekeeperSessionClient;
import com.onehilltech.gatekeeper.android.GatekeeperSignInActivity;

import static com.onehilltech.promises.Promise.resolved;

public class MainActivity extends AppCompatActivity
  implements GatekeeperSessionClient.Listener
{
  private Button btnSignOut_;

  private TextView whoami_;

  GatekeeperSessionClient sessionClient_;

  @Override
  protected void onCreate (@Nullable Bundle savedInstanceState)
  {
    super.onCreate (savedInstanceState);
    this.setContentView (R.layout.activity_main);

    this.sessionClient_ = GatekeeperSessionClient.getInstance (this);
    this.sessionClient_.addListener (this);

    this.onViewCreated ();
  }

  @Override
  protected void onDestroy ()
  {
    super.onDestroy ();

    this.sessionClient_.removeListener (this);
  }

  @Override
  protected void onStart ()
  {
    super.onStart ();

    this.sessionClient_.ensureSignedIn (this, GatekeeperSignInActivity.class);
  }

  private void onViewCreated ()
  {
    this.btnSignOut_ = (Button)this.findViewById (R.id.btn_signout);
    this.whoami_ = (TextView)this.findViewById (R.id.whoami);
    this.btnSignOut_.setEnabled (this.sessionClient_.isSignedIn ());

    this.btnSignOut_.setOnClickListener (
        v -> this.sessionClient_.signOut ()
                                .then (resolved (value -> {
                                  this.sessionClient_.ensureSignedIn (this, GatekeeperSignInActivity.class);
                                })));

    if (this.sessionClient_.isSignedIn ())
      this.whoami_.setText (this.sessionClient_.getSession ().getUsername ());
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

  @Override
  public void onReauthenticate (GatekeeperSessionClient client, HttpError error)
  {
    Intent intent =
        new GatekeeperSignInActivity.Builder (this)
            .setErrorMessage (error.getMessage ())
            .build ();

    this.sessionClient_.ensureSignedIn (this, intent);
  }
}
