package com.micklestudios.knowell.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import org.json.JSONException;
import org.json.JSONObject;

import com.micklestudios.knowell.ActivityBufferOpening;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

public class MyPushReceiver extends BroadcastReceiver {
	
	ParseUser currentUser;	
	private static final long CONVERSATIONS_TIMEOUT = 3000;	

	@Override
	public void onReceive(final Context context, Intent intent) {
		Log.i("pushReceiver", "Received push");
		// Toast.makeText(context,"Received push", Toast.LENGTH_SHORT).show();
		currentUser = ParseUser.getCurrentUser();
		// Bundle extras = intent.getExtras();
        // String message = extras != null ? extras.getString("com.parse.Data") : "";
        // JSONObject jObject = null;        
        // jObject = new JSONObject(message);
        // Toast.makeText(context,message, Toast.LENGTH_SHORT).show();
        
		// upon opening, pin online conversations to local
		SharedPreferences prefs = context.getSharedPreferences(ActivityBufferOpening.MY_PREFS_NAME, context.MODE_PRIVATE);
		SharedPreferences.Editor prefEditor = prefs.edit();
		final AsyncTasks.SyncDataTaskConversations syncConversations = new AsyncTasks.SyncDataTaskConversations(context, currentUser, prefs, prefEditor, true);
		syncConversations.execute();
		Handler handlerConversations = new Handler();
		handlerConversations.postDelayed(new Runnable() {

			@Override
			public void run() {
				if (syncConversations.getStatus() == AsyncTask.Status.RUNNING) {
					Toast.makeText(context, "Sync Conversations Timed Out", Toast.LENGTH_SHORT).show();
					syncConversations.cancel(true);
				}
			}
		}, CONVERSATIONS_TIMEOUT);
	}
	
}
