package com.onehilltech.gatekeeper.android.utils;

import android.widget.TextView;

public class InputError
{
  private TextView focusView_;

  public InputError ()
  {

  }

  public void addError (TextView view, String errorMessage)
  {
    view.setError (errorMessage);

    if (this.focusView_ == null)
      this.focusView_ = view;
  }

  public boolean hasError ()
  {
    return this.focusView_ != null;
  }

  public void requestFocus ()
  {
    this.focusView_.requestFocus ();
  }
}
