package com.onehilltech.gatekeeper.android;

import android.support.test.InstrumentationRegistry;

import com.onehilltech.metadata.ManifestMetadata;

import junit.framework.Assert;

import org.junit.Test;

public class LoginMetadataTest
{
  @Test
  public void testDefault () throws Exception
  {
    LoginMetadata loginMetadata = new LoginMetadata ();
    ManifestMetadata.get (InstrumentationRegistry.getTargetContext ()).initFromMetadata (loginMetadata);

    Assert.assertEquals ("new_account", loginMetadata.newAccountActivity);
    Assert.assertEquals ("redirect", loginMetadata.loginSuccessRedirectActivity);
  }
}
