package com.warpspace.ecardv4;

import com.parse.Parse;
import com.parse.ParseACL;
import com.parse.PushService;
import com.warpspace.ecardv4.R;

import android.app.Application;

public class MyApplication extends Application {

  @Override
  public void onCreate() {
    super.onCreate();

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
    // Specify an Activity to handle all pushes by default.
    PushService.setDefaultPushCallback(this, ActivityConversations.class);

  }
}
