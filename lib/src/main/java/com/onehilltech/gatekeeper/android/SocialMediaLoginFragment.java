package com.onehilltech.gatekeeper.android;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.onehilltech.gatekeeper.android.utils.BaseLoginFragment;

public class SocialMediaLoginFragment extends BaseLoginFragment
{
  private LoginButton fbLoginButton_;
  private CallbackManager fbCallbackManager_;

  private Button signUpUsingEmail_;

  private TextView logInUsingEmail_;

  private final FacebookCallback <LoginResult> fbLoginResult_ = new FacebookCallback<LoginResult> ()
  {
    @Override
    public void onSuccess (LoginResult loginResult)
    {

    }

    @Override
    public void onCancel ()
    {

    }

    @Override
    public void onError (FacebookException error)
    {

    }
  };

  private static final String [] FACEBOOK_DEFAULT_PERMISSIONS = {
    "email"
  };

  public static SocialMediaLoginFragment newInstance ()
  {
    return new SocialMediaLoginFragment ();
  }

  @Override
  public void onAttach (Context context)
  {
    super.onAttach (context);

    FacebookSdk.sdkInitialize (context.getApplicationContext ());
    this.fbCallbackManager_ = CallbackManager.Factory.create ();
  }

  @Nullable
  @Override
  public View onCreateView (LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
  {
    return inflater.inflate (R.layout.fragment_login_with_socialmedia, container, false);
  }

  @Override
  public void onViewCreated (View view, @Nullable Bundle savedInstanceState)
  {
    super.onViewCreated (view, savedInstanceState);

    // Setup Facebook login support.
    this.fbLoginButton_ = (LoginButton)view.findViewById (R.id.fb_login_button);
    this.fbLoginButton_.setReadPermissions (FACEBOOK_DEFAULT_PERMISSIONS);
    this.fbLoginButton_.setFragment (this);
    this.fbLoginButton_.registerCallback (this.fbCallbackManager_, this.fbLoginResult_);

    this.signUpUsingEmail_ = (Button)view.findViewById (R.id.sign_up_using_email);
    this.logInUsingEmail_ = (TextView)view.findViewById (R.id.log_in_using_email);
  }

  @Override
  public void onActivityResult (int requestCode, int resultCode, Intent data)
  {
    super.onActivityResult (requestCode, resultCode, data);
    this.fbCallbackManager_.onActivityResult (requestCode, resultCode, data);
  }
}
