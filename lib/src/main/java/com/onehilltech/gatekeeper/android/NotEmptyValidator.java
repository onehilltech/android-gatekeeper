package com.onehilltech.gatekeeper.android;

import android.support.annotation.NonNull;

import com.rengwuxian.materialedittext.validation.METValidator;

class NotEmptyValidator extends METValidator
{
  public NotEmptyValidator ()
  {
    this ("Cannot be an empty string");
  }

  public NotEmptyValidator (@NonNull String errorMessage)
  {
    super (errorMessage);
  }

  @Override
  public boolean isValid (@NonNull CharSequence text, boolean isEmpty)
  {
    return text.toString ().trim ().length () != 0;
  }
}
