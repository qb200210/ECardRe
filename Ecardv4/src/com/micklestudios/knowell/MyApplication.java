package com.micklestudios.knowell;

import com.parse.Parse;
import com.parse.ParseACL;
import com.parse.PushService;
import com.micklestudios.knowell.R;

import android.app.Application;
import android.content.Context;

public class MyApplication extends Application {

  private static Context context;

  @Override
  public void onCreate() {
    super.onCreate();
    context = this;
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
  
  public static Context getContext(){
    return context;
  }
  
}
