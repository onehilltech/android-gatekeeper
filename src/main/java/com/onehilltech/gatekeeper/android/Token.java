package com.onehilltech.gatekeeper.android;

import java.util.Date;

/**
 * Created by hillj on 9/16/15.
 */
public interface Token
{
  boolean hasExpired ();
  Date getExpirationDate ();
}
