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
    GatekeeperMetadata gatekeeperMetadata = new GatekeeperMetadata ();
    ManifestMetadata.get (InstrumentationRegistry.getTargetContext ()).initFromMetadata (gatekeeperMetadata);

    Assert.assertEquals ("new_account", gatekeeperMetadata.newAccountActivity);
    Assert.assertEquals ("redirect", gatekeeperMetadata.loginSuccessRedirectActivity);
  }
}
