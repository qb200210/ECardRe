package com.micklestudios.knowell;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import com.f2prateek.progressbutton.ProgressButton;
import com.micklestudios.knowell.infrastructure.UserInfo;
import com.micklestudios.knowell.utils.AsyncTasks;
import com.micklestudios.knowell.utils.ECardUtils;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.micklestudios.knowell.R;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Window;
import android.widget.Toast;

public class ActivityBufferOpening extends Activity {

	private static final long CREATE_SELF_COPY_TIMEOUT = 30000;
	private static final long CACHEIDS_TIMEOUT = 30000;
	private static final long NOTES_TIMEOUT = 30000;
	private static final long CONVERSATIONS_TIMEOUT = 30000;
	Integer WEIGHT_SELF = 20;
	Integer WEIGHT_NOTES = 20;
	Integer WEIGHT_CONV = 20;
	Integer WEIGHT_CACHEDIDS = 20;
	Integer totalProgress = 0;
	public static final String MY_PREFS_NAME = "KnoWellSyncParams";
	ParseUser currentUser;
	// flag to see if there is portrait cached offline that cannot be converted
	// to ParseFile yet.
	boolean imgFromTmpData = false;

	private boolean timeoutFlagSelf = false;
	private boolean timeoutFlagConv = false;
	private boolean timeoutFlagNotes = false;
	private boolean timeoutFlagCachedIds = false;
	private ProgressButton progressButton1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
		if (getActionBar() != null) {
			getActionBar().hide();
		}
		setContentView(R.layout.activity_buffer_opening);
		currentUser = ParseUser.getCurrentUser();
		progressButton1 = (ProgressButton) findViewById(R.id.pin_progress_1);
	    progressButton1.setProgress(totalProgress);

		if (ECardUtils.isNetworkAvailable(this)) {
			// Below is for the sake of push notification
			ParseInstallation.getCurrentInstallation().put("ecardId",
					currentUser.get("ecardId").toString());
			ParseInstallation.getCurrentInstallation().saveInBackground(
					new SaveCallback() {

						@Override
						public void done(ParseException arg0) {
						}
					});

			// check sharedpreferences
			SharedPreferences prefs = getSharedPreferences(MY_PREFS_NAME,
					MODE_PRIVATE);
			SharedPreferences.Editor prefEditor = prefs.edit();

			// syncing data within given timeout
			// when self sync done, transition
			syncAllDataUponOpening(prefs, prefEditor);
		} else {
			// no network, jump into ActivityMain
			
			Timer timer = new Timer();
			timer.schedule(new TimerTask() {

				@Override
				public void run() {
					// if no network, generate userInfo objects directly from localDataStore
					getContacts();
					totalProgress = 100;
					Message myMessage = new Message();
					myMessage.obj = totalProgress;
					handlerJump.sendMessage(myMessage);
				}
				
			}, 500);
		}

		// if tmpImgByteArray not null, need to convert to img file regardless
		// of network
		checkPortrait();
		// only display the splash screen for an amount of time
		// should I have it depend on the completion of self-copy sync instead?

	}
	
	  private void getContacts() {
		  	ActivitySearch.allUsers = new ArrayList<UserInfo>();
		    /* A map of all the ECardNote objects to the noteID */
		    final HashMap<String, ParseObject> noteIdToNoteObjectMap = new HashMap<String, ParseObject>();

		    ParseQuery<ParseObject> queryNotes = ParseQuery.getQuery("ECardNote");
		    queryNotes.fromLocalDatastore();
		    queryNotes.whereEqualTo("userId", currentUser.getObjectId());

		    queryNotes.findInBackground(new FindCallback<ParseObject>() {
		      @Override
		      public void done(List<ParseObject> objectsNoteList,
		        ParseException noteException) {
		        if (noteException == null) {
		          if (objectsNoteList.size() != 0) {
		            // Got a list of all the notes. Now collect all the noteIDs.
		            for (Iterator<ParseObject> iter = objectsNoteList.iterator(); iter
		              .hasNext();) {
		              ParseObject objectNote = iter.next();
		              String infoObjectId = (String) objectNote.get("ecardId");

		              // Add these values to the map.
		              noteIdToNoteObjectMap.put(infoObjectId, objectNote);
		            }

		            /*
		             * Now, query the ECardInfoTable to get all the ECardInfo for the
		             * notes collected here.
		             */
		            ParseQuery<ParseObject> queryInfo = ParseQuery
		              .getQuery("ECardInfo");
		            queryInfo.fromLocalDatastore();
		            queryInfo.whereContainedIn("objectId",
		              noteIdToNoteObjectMap.keySet());

		            queryInfo.findInBackground(new FindCallback<ParseObject>() {

		              @Override
		              public void done(List<ParseObject> objectInfoList,
		                ParseException infoException) {
		                // Now we have a list of ECardInfo objects. Populate the
		                // userInfo list.
		                if (infoException == null) {
		                  // Iterate over the list.
		                  for (Iterator<ParseObject> iter = objectInfoList.iterator(); iter
		                    .hasNext();) {
		                    ParseObject objectInfo = iter.next();
		                    UserInfo contact = new UserInfo(objectInfo);
		                    if (contact != null) {
		                      // Contact has been created. Populate the "createdAt" from
		                      // the note object.
		                      String infoObjectId = (String) objectInfo.getObjectId();
		                      ParseObject objectNote = noteIdToNoteObjectMap
		                        .get(infoObjectId);
		                      contact.setCreatedAt(objectNote.getCreatedAt());

		                      ActivitySearch.allUsers.add(contact);

		                    }
		                  }

		                }
		              }
		            });
		          }
		        } else {
		          Toast.makeText(getBaseContext(), "General parse error!",
		            Toast.LENGTH_SHORT).show();
		        }
		      }
		    });
		  }

	private void syncAllDataUponOpening(SharedPreferences prefs,
			SharedPreferences.Editor prefEditor) {
		// Create/refresh local copy every time app opens
		final AsyncTasks.SyncDataTaskSelfCopy createSelfCopy = new AsyncTasks.SyncDataTaskSelfCopy(
				this, currentUser, prefs, prefEditor);
		createSelfCopy.execute();
		Handler handler = new Handler();
		handler.postDelayed(new Runnable() {

			@Override
			public void run() {
				if (createSelfCopy.getStatus() == AsyncTask.Status.RUNNING) {
					Toast.makeText(getApplicationContext(),
							"Self Copy Timed Out", Toast.LENGTH_SHORT).show();
					timeoutFlagSelf = true;
					createSelfCopy.cancel(true);
				}
			}
		}, CREATE_SELF_COPY_TIMEOUT);

		// upon opening, pin online conversations to local
		final AsyncTasks.SyncDataTaskConversations syncConversations = new AsyncTasks.SyncDataTaskConversations(
				this, currentUser, prefs, prefEditor);
		syncConversations.execute();
		Handler handlerConversations = new Handler();
		handlerConversations.postDelayed(new Runnable() {

			@Override
			public void run() {
				if (syncConversations.getStatus() == AsyncTask.Status.RUNNING) {
					Toast.makeText(getApplicationContext(),
							"Sync Conversations Timed Out", Toast.LENGTH_SHORT)
							.show();
					timeoutFlagConv = true;
					syncConversations.cancel(true);
				}
			}
		}, CONVERSATIONS_TIMEOUT);

		// upon opening, pin online notes to local
		final AsyncTasks.SyncDataTaskNotes syncNotes = new AsyncTasks.SyncDataTaskNotes(
				this, currentUser, prefs, prefEditor);
		syncNotes.execute();
		Handler handlerNotes = new Handler();
		handlerNotes.postDelayed(new Runnable() {

			@Override
			public void run() {
				if (syncNotes.getStatus() == AsyncTask.Status.RUNNING) {
					Toast.makeText(getApplicationContext(),
							"Sync Notes Timed Out", Toast.LENGTH_SHORT).show();
					timeoutFlagNotes = true;
					syncNotes.cancel(true);
				}
			}
		}, NOTES_TIMEOUT);

		// check ecardIds that were scanned/cached offline
		final AsyncTasks.SyncDataTaskCachedIds syncCachedIds = new AsyncTasks.SyncDataTaskCachedIds(
				this, currentUser, prefs, prefEditor);
		syncCachedIds.execute();
		Handler handlerCachedIds = new Handler();
		handlerCachedIds.postDelayed(new Runnable() {

			@Override
			public void run() {
				if (syncCachedIds.getStatus() == AsyncTask.Status.RUNNING) {
					Toast.makeText(getApplicationContext(),
							"Sync CachedIds Timed Out", Toast.LENGTH_SHORT)
							.show();
					timeoutFlagCachedIds = true;
					syncCachedIds.cancel(true);
				}
			}
		}, CACHEIDS_TIMEOUT);

		Thread timerThread = new Thread() {
			private boolean flagSyncSelfDone = false;
			private boolean flagSyncNotesDone = false;
			private boolean flagSyncConvDone = false;
			private boolean flagSyncCachedIdsDone = false;

			public void run() {
				while (totalProgress != 100) {
					try {
						sleep(500);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if(totalProgress == 80) {
						Log.i("msg", "all others complete");
						// when all other syncs complete, generate UserInfo objects from LocalDataStore
						getContacts();
						totalProgress = 100;
					}
					if( (createSelfCopy.getStatus() != AsyncTask.Status.RUNNING || timeoutFlagSelf) && !flagSyncSelfDone) {
						// In whatever case, this process is completed
						totalProgress = totalProgress + WEIGHT_SELF;
						flagSyncSelfDone  = true;
						Log.i("self", totalProgress.toString());
					}
					if( (syncNotes.getStatus() != AsyncTask.Status.RUNNING || timeoutFlagNotes) && !flagSyncNotesDone) {
						// In whatever case, this process is completed
						totalProgress = totalProgress + WEIGHT_NOTES;
						flagSyncNotesDone  = true;
						Log.i("note", totalProgress.toString());
					}
					if( (syncConversations.getStatus() != AsyncTask.Status.RUNNING || timeoutFlagConv) && !flagSyncConvDone) {
						// In whatever case, this process is completed
						totalProgress = totalProgress + WEIGHT_CONV;
						flagSyncConvDone  = true;
						Log.i("conv", totalProgress.toString());
					}
					if( (syncCachedIds.getStatus() != AsyncTask.Status.RUNNING || timeoutFlagCachedIds) && !flagSyncCachedIdsDone) {
						// In whatever case, this process is completed
						totalProgress = totalProgress + WEIGHT_CACHEDIDS;
						flagSyncCachedIdsDone  = true;
						Log.i("cache", totalProgress.toString());
					}
					Message myMessage = new Message();
					myMessage.obj = totalProgress;
					handlerJump.sendMessage(myMessage);
					
				}
			}
		};
		timerThread.start();
	}
	
	Handler handlerJump = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			progressButton1.setProgress(totalProgress);
			if(totalProgress == 100) {
				// if there is network, wait till self sync completes before finishing BufferOpening
				Intent intent = new Intent(ActivityBufferOpening.this, ActivityMain.class);
				intent.putExtra("imgFromTmpData", imgFromTmpData);
				ActivityBufferOpening.this.startActivity(intent);
				Timer timer = new Timer();
				timer.schedule(new TimerTask() {

					@Override
					public void run() {
						ActivityBufferOpening.this.finish();
						
						// have to delayed finishing, so desktop don't show
					}
				}, 500);
			}
		}
	};

	private void checkPortrait() {
		ParseQuery<ParseObject> query = ParseQuery.getQuery("ECardInfo");
		query.fromLocalDatastore();
		query.getInBackground(currentUser.get("ecardId").toString(),
				new GetCallback<ParseObject>() {

					@Override
					public void done(ParseObject object, ParseException e) {
						if (e == null && object != null) {
							byte[] tmpImgData = (byte[]) object
									.get("tmpImgByteArray");
							if (tmpImgData != null) {
								imgFromTmpData = true;
							}
						}
					}

				});

	}

}
