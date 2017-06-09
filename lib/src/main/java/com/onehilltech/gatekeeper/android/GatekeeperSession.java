package com.onehilltech.gatekeeper.android;

import android.content.Context;
import android.content.SharedPreferences;

class GatekeeperSession
{
  private static final String PREFS_FILE = "gatekeeper_session.info";

  private static final String PREF_USERNAME = "username";
  private static final String PREF_USER_ID = "user_id";

  private final SharedPreferences prefs_;

  /**
   * Edit the current session. This can only be used by classes from this
   * package.
   */
  static class Editor
  {
    private final SharedPreferences.Editor editor_;

    private Editor (SharedPreferences.Editor editor)
    {
      this.editor_ = editor;
    }

    Editor setUsername (String username)
    {
      this.editor_.putString (PREF_USERNAME, username);
      return this;
    }

    Editor setUserId (String userId)
    {
      this.editor_.putString (PREF_USER_ID, userId);
      return this;
    }

    boolean commit ()
    {
      return this.editor_.commit ();
    }

    void apply ()
    {
      this.editor_.apply ();
    }

    void clear ()
    {
      this.editor_.clear ();
    }

    void delete ()
    {
      this.editor_.remove (PREF_USERNAME);
      this.editor_.remove (PREF_USER_ID);

      this.editor_.commit ();
    }
  }

  /**
   * Get the current GatekeeperSession details.
   *
   * @param context
   * @return
   */
  public static GatekeeperSession get (Context context)
  {
    return new GatekeeperSession (context.getSharedPreferences (PREFS_FILE, Context.MODE_PRIVATE));
  }

  public String getUsername ()
  {
    return this.prefs_.getString (PREF_USERNAME, null);
  }

  public String getUserId ()
  {
    return this.prefs_.getString (PREF_USER_ID, null);
  }

  private GatekeeperSession (SharedPreferences prefs)
  {
    this.prefs_ = prefs;
  }

  /**
   * Edit the preferences. This can only be used by classes in this package to
   * prevent malicious intent.
   *
   * @return
   */
  Editor edit ()
  {
    return new Editor (this.prefs_.edit ());
  }
}
