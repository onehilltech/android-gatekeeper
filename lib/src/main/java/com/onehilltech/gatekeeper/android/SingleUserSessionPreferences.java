package com.onehilltech.gatekeeper.android;

import android.content.Context;
import android.content.SharedPreferences;

class SingleUserSessionPreferences
{
  private static final String PREFS_FILE = "gatekeeper_single_user_session.prefs";
  private static final String PREF_USERNAME = "username";

  private final SharedPreferences prefs_;

  public static class Editor
  {
    private final SharedPreferences.Editor editor_;

    private Editor (SharedPreferences.Editor editor)
    {
      this.editor_ = editor;
    }

    public Editor setUsername (String userId)
    {
      this.editor_.putString (PREF_USERNAME, userId);
      return this;
    }

    public boolean commit ()
    {
      return this.editor_.commit ();
    }

    public void apply ()
    {
      this.editor_.apply ();
    }

    public void clear ()
    {
      this.editor_.clear ();
    }

    public void delete ()
    {
      this.editor_.remove (PREF_USERNAME);
      this.editor_.commit ();
    }
  }

  public static SingleUserSessionPreferences open (Context context)
  {
    return new SingleUserSessionPreferences (context.getSharedPreferences (PREFS_FILE, Context.MODE_PRIVATE));
  }

  public String getUsername ()
  {
    return this.prefs_.getString (PREF_USERNAME, null);
  }

  private SingleUserSessionPreferences (SharedPreferences prefs)
  {
    this.prefs_ = prefs;
  }

  /**
   * Edit the preferences. This can only be used by classes in this package to
   * prevent malicious intent.
   *
   * @return
   */
  public Editor edit ()
  {
    return new Editor (this.prefs_.edit ());
  }
}
