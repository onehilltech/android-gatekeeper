package com.onehilltech.gatekeeper.android;

import android.widget.TextView;

/**
 * Created by hilljh on 11/19/15.
 */
class InputError
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
