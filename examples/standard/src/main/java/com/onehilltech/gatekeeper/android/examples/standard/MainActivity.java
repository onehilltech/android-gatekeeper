package com.onehilltech.gatekeeper.android.examples.standard;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.onehilltech.gatekeeper.android.LoginActivity;
import com.onehilltech.gatekeeper.android.ResponseListener;
import com.onehilltech.gatekeeper.android.SingleUserSessionClient;

public class MainActivity extends AppCompatActivity
  implements SingleUserSessionClient.OnInitializedListener
{
  private SingleUserSessionClient client_;
  private TextView errMsg_;
  private Button btnSignOut_;

  @Override
  protected void onCreate (@Nullable Bundle savedInstanceState)
  {
    super.onCreate (savedInstanceState);

    this.setContentView (R.layout.activity_main);
    this.onViewCreated ();

    SingleUserSessionClient.initialize (this, this);
  }

  private void onViewCreated ()
  {
    this.errMsg_ = (TextView) this.findViewById (R.id.error_message);

    this.btnSignOut_ = (Button)this.findViewById (R.id.btn_signout);
    this.btnSignOut_.setOnClickListener (new View.OnClickListener ()
    {
      @Override
      public void onClick (View v)
      {
        client_.logout (new ResponseListener<Boolean> ()
        {
          @Override
          public void onErrorResponse (VolleyError error)
          {

          }

          @Override
          public void onResponse (Boolean response)
          {
            if (response)
              onSignOut ();
          }
        });
      }
    });
  }

  @Override
  public void onInitialized (SingleUserSessionClient sessionClient)
  {
    this.client_ = sessionClient;
    this.btnSignOut_.setEnabled (this.client_.isLoggedIn ());
  }

  private void onSignOut ()
  {
    // Show the login activity.
    Intent intent = new Intent (this, LoginActivity.class);
    this.startActivity (intent);

    // Finish this activity.
    this.finish ();
  }

  @Override
  public void onInitializeFailed (Throwable t)
  {
    this.errMsg_.setText (t.getLocalizedMessage ());
  }
}
