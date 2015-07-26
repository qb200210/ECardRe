package com.micklestudios.knowell;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.micklestudios.knowell.utils.AppGlobals;
import com.micklestudios.knowell.utils.TypefaceUtil;
import com.parse.Parse;
import com.parse.ParseACL;
import com.parse.ParseException;
import com.parse.ParsePush;
import com.parse.ParseUser;
import com.parse.SaveCallback;

public class MyApplication extends Application {

  private static Context context;

  @Override
  public void onCreate() {
    super.onCreate();
    context = this;
    // Set global default font to be Roboto
    TypefaceUtil.overrideFont(getApplicationContext(), "SERIF", "fonts/Roboto-Regular.ttf");
    // Must extend Application, otherwise get errors when opening app again
    // saying enableOfflineStore() called multiple times
    Parse.enableLocalDatastore(this);
    // ParseUser.enableAutomaticUser();
    ParseACL defaultACL = new ParseACL();
    defaultACL.setPublicReadAccess(true);
    defaultACL.setPublicWriteAccess(false);
    ParseACL.setDefaultACL(defaultACL, true);
    Parse.initialize(this, getString(R.string.parse_app_id),
      getString(R.string.parse_client_key));
    ParseUser.enableRevocableSessionInBackground();

    // check sharedpreferences
    SharedPreferences sharedPref = getSharedPreferences(
      AppGlobals.MY_PREFS_NAME, MODE_PRIVATE);
    boolean notificationsEnabled = sharedPref.getBoolean("KnoWellPushToggle",
      true);
    if (notificationsEnabled) {
      ParsePush.subscribeInBackground(AppGlobals.PUSH_CHANNEL_NAME,
        new SaveCallback() {
          @Override
          public void done(ParseException e) {
            if (e == null) {
              Log.d("com.parse.push",
                "successfully subscribed to the broadcast channel.");
            } else {
              Log.e("com.parse.push", "failed to subscribe for push", e);
            }
          }
        });
    }
  }

  public static Context getContext() {
    return context;
  }

}
