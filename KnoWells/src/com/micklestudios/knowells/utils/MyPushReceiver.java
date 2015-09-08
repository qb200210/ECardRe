package com.micklestudios.knowells.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import org.json.JSONException;
import org.json.JSONObject;

import com.micklestudios.knowells.ActivityBufferOpening;
import com.micklestudios.knowells.ActivityConversations;
import com.micklestudios.knowells.ActivityMain;
import com.micklestudios.knowells.R;
import com.micklestudios.knowells.infrastructure.ConversationsListAdapter;
import com.nhaarman.listviewanimations.appearance.simple.AlphaInAnimationAdapter;
import com.parse.FindCallback;
import com.parse.ParseAnalytics;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParsePushBroadcastReceiver;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

public class MyPushReceiver extends ParsePushBroadcastReceiver {

  ParseUser currentUser;
  private Context context;
  private static final long CONVERSATIONS_TIMEOUT = 3000;

  @Override
  public void onPushReceive(final Context context, Intent intent) {
    this.context = context;
    super.onPushReceive(context, intent);
    Log.i("pushReceiver", "Received push");
    currentUser = ParseUser.getCurrentUser();
    // Bundle extras = intent.getExtras();
    // String message = extras != null ? extras.getString("com.parse.Data") :
    // "";
    // JSONObject jObject = null;
    // jObject = new JSONObject(message);
    // Toast.makeText(context,message, Toast.LENGTH_SHORT).show();

    // upon opening, pin online conversations to local
    SharedPreferences prefs = context.getSharedPreferences(
      AppGlobals.MY_PREFS_NAME, context.MODE_PRIVATE);
    SharedPreferences.Editor prefEditor = prefs.edit();
    final AsyncTasks.SyncDataTaskConversations syncConversations = new AsyncTasks.SyncDataTaskConversations(
      context, currentUser, prefs, prefEditor, true);
    syncConversations.execute();
    Handler handlerConversations = new Handler();
    handlerConversations.postDelayed(new Runnable() {

      @Override
      public void run() {
        if (syncConversations.getStatus() == AsyncTask.Status.RUNNING) {
          Toast.makeText(context, "Sync Conversations Timed Out",
            Toast.LENGTH_SHORT).show();
          syncConversations.cancel(true);
        }
      }
    }, CONVERSATIONS_TIMEOUT);
    
  }

  @Override
  public void onPushOpen(Context context, Intent intent) {

    // To track "App Opens"
    ParseAnalytics.trackAppOpenedInBackground(intent);

    // Here is data you sent
    Log.i("push", intent.getExtras().getString("com.parse.Data"));
    
    Intent i = new Intent(context, ActivityConversations.class);
    i.putExtra("refreshList", true);
    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    context.startActivity(i);
  }

}
