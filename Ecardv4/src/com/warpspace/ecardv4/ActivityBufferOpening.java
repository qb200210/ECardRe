package com.warpspace.ecardv4;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;

import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseACL;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.warpspace.ecardv4.R;
import com.warpspace.ecardv4.utils.ECardSQLHelper;
import com.warpspace.ecardv4.utils.ECardUtils;
import com.warpspace.ecardv4.utils.OfflineData;
import com.warpspace.ecardv4.utils.AsyncTasks;
import com.warpspace.ecardv4.utils.AsyncTasks.SyncDataTaskSelfCopy;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Window;
import android.widget.Toast;

public class ActivityBufferOpening extends Activity {

	private static final long CREATE_SELF_COPY_TIMEOUT = 2000;
	private static final long CACHEIDS_TIMEOUT = 10000;
	private static final long NOTES_TIMEOUT = 10000;
	private static final long CONVERSATIONS_TIMEOUT = 3000;	
	ParseUser currentUser;
	// flag to see if there is portrait cached offline that cannot be converted to ParseFile yet.
	boolean imgFromTmpData = false;

	private boolean timeoutFlag = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
		if (getActionBar() != null) {
			getActionBar().hide();
		}
		setContentView(R.layout.activity_buffer_opening);
		currentUser = ParseUser.getCurrentUser();
		
		if(ECardUtils.isNetworkAvailable(this)){
			// Below is for the sake of push notification
			ParseInstallation.getCurrentInstallation().put("ecardId", currentUser.get("ecardId").toString());
			ParseInstallation.getCurrentInstallation().saveInBackground(new SaveCallback() {
	
				@Override
				public void done(ParseException arg0) {
				}
			});
	
			// syncing data within given timeout		
			syncAllDataUponOpening();
		}
		
		// if tmpImgByteArray not null, need to convert regardless of network
		checkPortrait();
		// only display the splash screen for an amount of time
		// should I have it depend on the completion of self-copy sync instead?
		timerToJump();
	}
	
	private void syncAllDataUponOpening() {
		// Create/refresh local copy every time app opens
		final AsyncTasks.SyncDataTaskSelfCopy createSelfCopy = new AsyncTasks.SyncDataTaskSelfCopy(this, currentUser);
		createSelfCopy.execute();
		Handler handler = new Handler();
		handler.postDelayed(new Runnable() {

			@Override
			public void run() {
				if (createSelfCopy.getStatus() == AsyncTask.Status.RUNNING) {
					Toast.makeText(getApplicationContext(), "Self Copy Timed Out", Toast.LENGTH_SHORT).show();
					timeoutFlag = true;
					createSelfCopy.cancel(true);
				}
			}
		}, CREATE_SELF_COPY_TIMEOUT);
		
		// upon opening, pin online conversations to local
		final AsyncTasks.SyncDataTaskConversations syncConversations = new AsyncTasks.SyncDataTaskConversations(this, currentUser);
		syncConversations.execute();
		Handler handlerConversations = new Handler();
		handlerConversations.postDelayed(new Runnable() {

			@Override
			public void run() {
				if (syncConversations.getStatus() == AsyncTask.Status.RUNNING) {
					Toast.makeText(getApplicationContext(), "Sync Conversations Timed Out", Toast.LENGTH_SHORT).show();
					timeoutFlag = true;
					syncConversations.cancel(true);
				}
			}
		}, CONVERSATIONS_TIMEOUT);
		
		// upon opening, pin online notes to local
		final AsyncTasks.SyncDataTaskNotes syncNotes = new AsyncTasks.SyncDataTaskNotes(this, currentUser);
		syncNotes.execute();
		Handler handlerNotes = new Handler();
		handlerNotes.postDelayed(new Runnable() {

			@Override
			public void run() {
				if (syncNotes.getStatus() == AsyncTask.Status.RUNNING) {
					Toast.makeText(getApplicationContext(), "Sync Notes Timed Out", Toast.LENGTH_SHORT).show();
					timeoutFlag = true;
					syncNotes.cancel(true);
				}
			}
		}, NOTES_TIMEOUT);
		
		// check ecardIds that were scanned/cached offline		
		final AsyncTasks.SyncDataTaskCachedIds syncCachedIds = new AsyncTasks.SyncDataTaskCachedIds(this, currentUser);
		syncCachedIds.execute();
		Handler handlerCachedIds = new Handler();
		handlerCachedIds.postDelayed(new Runnable() {

			@Override
			public void run() {
				if (syncCachedIds.getStatus() == AsyncTask.Status.RUNNING) {
					Toast.makeText(getApplicationContext(), "Sync CachedIds Timed Out", Toast.LENGTH_SHORT).show();
					timeoutFlag = true;
					syncCachedIds.cancel(true);
				}
			}
		}, CACHEIDS_TIMEOUT);
	}

	private void checkPortrait() {
		ParseQuery<ParseObject> query = ParseQuery.getQuery("ECardInfo");
		query.fromLocalDatastore();
		query.getInBackground(currentUser.get("ecardId").toString(), new GetCallback<ParseObject>() {

			@Override
			public void done(ParseObject object, ParseException e) {
				if (e == null && object != null) {
					byte[] tmpImgData = (byte[]) object.get("tmpImgByteArray");
					if (tmpImgData != null) {
						imgFromTmpData = true;
					}
				}
			}

		});

	}

	public void timerToJump() {
		int timeout = 1000;
		// make the activity visible for 3 seconds before transitioning
		// This is important when first time sign up or login on this device
		// allows time to create local self copy

		Timer timer = new Timer();
		timer.schedule(new TimerTask() {

			@Override
			public void run() {
				Intent intent = new Intent(getBaseContext(), ActivityMain.class);
				intent.putExtra("imgFromTmpData", imgFromTmpData);
				startActivity(intent);
			}
		}, timeout);

		int timeout1 = timeout + 500;
		timer.schedule(new TimerTask() {

			@Override
			public void run() {
				finish();
				// have to delayed finishing, so desktop don't show
			}
		}, timeout1);
	}

}
