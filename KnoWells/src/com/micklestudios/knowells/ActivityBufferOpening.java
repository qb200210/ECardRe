package com.micklestudios.knowells;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.f2prateek.progressbutton.ProgressButton;
import com.micklestudios.knowells.R;
import com.micklestudios.knowells.utils.AppGlobals;
import com.micklestudios.knowells.utils.AsyncTasks;
import com.micklestudios.knowells.utils.AsyncTasks.SyncDataTaskCachedIds;
import com.micklestudios.knowells.utils.AsyncTasks.SyncDataTaskConversations;
import com.micklestudios.knowells.utils.AsyncTasks.SyncDataTaskHistory;
import com.micklestudios.knowells.utils.AsyncTasks.SyncDataTaskNotes;
import com.micklestudios.knowells.utils.AsyncTasks.SyncDataTaskSelfCopy;
import com.micklestudios.knowells.utils.ECardUtils;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

public class ActivityBufferOpening extends Activity {

  private static final long CREATE_SELF_COPY_TIMEOUT = 60000;
  private static final long CACHEIDS_TIMEOUT = 60000;
  private static final long NOTES_TIMEOUT = 60000;
  private static final long CONVERSATIONS_TIMEOUT = 60000;
  private static final long COMPANYNAME_TIMEOUT = 60000;
  private static final long HISTORY_TIMEOUT = 60000;

  Integer WEIGHT_SELF = 20;
  Integer WEIGHT_NOTES = 20;
  Integer WEIGHT_CONV = 20;
  Integer WEIGHT_CACHEDIDS = 20;
  Integer totalProgress = 0;

  private static final String KNOWELL_ROOT = "KnoWell";
  // flag to see if there is portrait cached offline that cannot be converted
  // to ParseFile yet.
  boolean imgFromTmpData = false;

  private boolean timeoutFlagSelf = false;
  private boolean timeoutFlagConv = false;
  private boolean timeoutFlagNotes = false;
  private boolean timeoutFlagCachedIds = false;
  private boolean timeoutFlagCompany = false;
  private boolean flagSyncSelfDone = false;
  private boolean flagSyncNotesDone = false;
  private boolean flagSyncConvDone = false;
  private boolean flagSyncCachedIdsDone = false;

  private ProgressButton progressButton1;
  private TextView progressText;
  private SharedPreferences prefs;
  private Editor prefEditor;
  protected boolean timeoutFlagHistory = false;
  private SyncDataTaskHistory syncHistory;
  private SyncDataTaskSelfCopy createSelfCopy;
  private SyncDataTaskConversations syncConversations;
  private SyncDataTaskNotes syncNotes;
  private SyncDataTaskCachedIds syncCachedIds;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
    if (getActionBar() != null) {
      getActionBar().hide();
    }
    setContentView(R.layout.activity_buffer_opening);
    AppGlobals.currentUser = ParseUser.getCurrentUser();
    progressButton1 = (ProgressButton) findViewById(R.id.pin_progress_1);
    progressButton1.setProgress(totalProgress);

    progressText = (TextView) findViewById(R.id.loading_progress);
    progressText.setText("Working on it ...");
    
    

    // if tmpImgByteArray not null, need to convert to img file regardless
    // of network
    checkPortrait();
    checkFolders();

    // check sharedpreferences
    prefs = getSharedPreferences(
      AppGlobals.MY_PREFS_NAME, MODE_PRIVATE);
    prefEditor = prefs.edit();

    if (ECardUtils.isNetworkAvailable(this)) {
      // Below is for the sake of push notification
      ParseInstallation.getCurrentInstallation().put("ecardId",
        AppGlobals.currentUser.get("ecardId").toString());
      ParseInstallation.getCurrentInstallation().saveInBackground(
        new SaveCallback() {

          @Override
          public void done(ParseException arg0) {
          }
        });

      // syncing data within given timeout
      // when self sync done, transition
      InitializeAsyncTasks();
      syncAllDataUponOpening();
    } else {
      // no network, jump into ActivityMain

      Timer timer = new Timer();
      timer.schedule(new TimerTask() {

        @Override
        public void run() {
          // if no network, generate userInfo objects directly from
          // localDataStore
          AppGlobals.initializeAllContactsBlocking();
          AppGlobals.initializePotentialUsersBlocking();
          createCompanyNamesFromLocal(prefs);
          totalProgress = 100;
          Message myMessage = new Message();
          myMessage.obj = totalProgress;
          handlerJump.sendMessage(myMessage);
        }

      }, 500);
    }

    // only display the splash screen for an amount of time
    // should I have it depend on the completion of self-copy sync instead?

  }

  private void InitializeAsyncTasks() {
    // TODO Auto-generated method stub
    syncHistory = new AsyncTasks.SyncDataTaskHistory(
      this, AppGlobals.currentUser, prefs, prefEditor, false);
    createSelfCopy = new AsyncTasks.SyncDataTaskSelfCopy(
      this, AppGlobals.currentUser, prefs, prefEditor, false);
    syncConversations = new AsyncTasks.SyncDataTaskConversations(
      this, AppGlobals.currentUser, prefs, prefEditor, false);
    syncNotes = new AsyncTasks.SyncDataTaskNotes(
      this, AppGlobals.currentUser, prefs, prefEditor, false);
    syncCachedIds = new AsyncTasks.SyncDataTaskCachedIds(
      this, AppGlobals.currentUser, prefs, prefEditor, false);
    
    // set cancel button
    Button skipSync = (Button) findViewById(R.id.cancel_sync_button);
    skipSync.setOnClickListener(new OnClickListener(){

      @Override
      public void onClick(View v) {
        syncCachedIds.cancel(true);
        syncNotes.cancel(true);
        syncConversations.cancel(true);
        createSelfCopy.cancel(true);
        timeoutFlagConv = true;
        timeoutFlagCachedIds = true;
        timeoutFlagNotes = true;
        timeoutFlagSelf = true;
      }
      
    });
  }

  private void checkFolders() {
    String filepath = Environment.getExternalStorageDirectory().getPath();
    File file = new File(filepath, KNOWELL_ROOT);
    if (!file.exists()) {
      file.mkdirs();
    }
  }

  protected void createCompanyNamesFromLocal(SharedPreferences prefs) {
    // Retrieve the saved company list from sharedpreference
    // this will be used to populate autocomplete box
    ActivityDesign.companyNames = new ArrayList<String>();
    Set<String> tmpCompanyNames = new HashSet<String>();
    tmpCompanyNames = prefs.getStringSet("listOfCompanyNames", null);
    if (tmpCompanyNames != null) {
      ActivityDesign.companyNames.addAll(tmpCompanyNames);
    }
  }

  private void syncAllDataUponOpening() {
    

    // sync history, Supposely not critical, so don't need to wait on it
    
    syncHistory.execute();
    Handler handlerHistory = new Handler();
    handlerHistory.postDelayed(new Runnable() {

      @Override
      public void run() {
        if (syncHistory.getStatus() == AsyncTask.Status.RUNNING) {
          Toast.makeText(getApplicationContext(), "Sync History Timed Out",
            Toast.LENGTH_SHORT).show();
          timeoutFlagHistory  = true;
          syncHistory.cancel(true);
        }
      }
    }, HISTORY_TIMEOUT);

    // Create/refresh local copy every time app opens
    
    createSelfCopy.execute();
    Handler handler = new Handler();
    handler.postDelayed(new Runnable() {

      @Override
      public void run() {
        if (createSelfCopy.getStatus() == AsyncTask.Status.RUNNING) {
          Toast.makeText(getApplicationContext(), "Sync My Card Timed Out",
            Toast.LENGTH_SHORT).show();
          timeoutFlagSelf = true;
          createSelfCopy.cancel(true);
        }
      }
    }, CREATE_SELF_COPY_TIMEOUT);

    // upon opening, pin online conversations to local
    
    syncConversations.execute();
    Handler handlerConversations = new Handler();
    handlerConversations.postDelayed(new Runnable() {

      @Override
      public void run() {
        if (syncConversations.getStatus() == AsyncTask.Status.RUNNING) {
          Toast.makeText(getApplicationContext(),
            "Sync Notifications Timed Out", Toast.LENGTH_SHORT).show();
          timeoutFlagConv = true;
          syncConversations.cancel(true);
        }
      }
    }, CONVERSATIONS_TIMEOUT);

    // upon opening, pin online notes to local
    
    syncNotes.execute();
    Handler handlerNotes = new Handler();
    handlerNotes.postDelayed(new Runnable() {

      @Override
      public void run() {
        if (syncNotes.getStatus() == AsyncTask.Status.RUNNING) {
          Toast.makeText(getApplicationContext(),
            "Sync Card Collection Timed Out", Toast.LENGTH_SHORT).show();
          timeoutFlagNotes = true;
          syncNotes.cancel(true);
        }
      }
    }, NOTES_TIMEOUT);

    // check ecardIds that were scanned/cached offline
    
    syncCachedIds.execute();
    Handler handlerCachedIds = new Handler();
    handlerCachedIds.postDelayed(new Runnable() {

      @Override
      public void run() {
        if (syncCachedIds.getStatus() == AsyncTask.Status.RUNNING) {
          Toast.makeText(getApplicationContext(),
            "Sync Offline Collection Timed Out", Toast.LENGTH_SHORT).show();
          timeoutFlagCachedIds = true;
          syncCachedIds.cancel(true);
        }
      }
    }, CACHEIDS_TIMEOUT);
    
    

    Thread timerThread = new Thread() {

      public void run() {
        while (totalProgress != 100) {
          try {
            sleep(500);
          } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
          if (totalProgress == 80) {
            Log.i("msg", "all others complete");
            // when all other syncs complete, generate UserInfo objects from
            // LocalDataStore
            
            // update the local string list for available company templates -- only the
            // names, not the actual object
            final AsyncTasks.SyncDataCompanyNames syncCompanyNames = new AsyncTasks.SyncDataCompanyNames(
              ActivityBufferOpening.this, prefs, prefEditor, false);
            syncCompanyNames.execute();  
            
            AppGlobals.initializeAllContactsBlocking();
            AppGlobals.initializePotentialUsersBlocking();
            totalProgress = 100;
          }
          if ((createSelfCopy.getStatus() != AsyncTask.Status.RUNNING || timeoutFlagSelf)
            && !flagSyncSelfDone) {
            // In whatever case, this process is completed
            totalProgress = totalProgress + WEIGHT_SELF;
            flagSyncSelfDone = true;
            Log.i("self", totalProgress.toString());
          }
          if ((syncNotes.getStatus() != AsyncTask.Status.RUNNING || timeoutFlagNotes)
            && !flagSyncNotesDone) {
            // In whatever case, this process is completed
            totalProgress = totalProgress + WEIGHT_NOTES;
            flagSyncNotesDone = true;
            Log.i("note", totalProgress.toString());
          }
          if ((syncConversations.getStatus() != AsyncTask.Status.RUNNING || timeoutFlagConv)
            && !flagSyncConvDone) {
            // In whatever case, this process is completed
            totalProgress = totalProgress + WEIGHT_CONV;
            flagSyncConvDone = true;
            Log.i("conv", totalProgress.toString());
          }
          if ((syncCachedIds.getStatus() != AsyncTask.Status.RUNNING || timeoutFlagCachedIds)
            && !flagSyncCachedIdsDone) {
            // In whatever case, this process is completed
            totalProgress = totalProgress + WEIGHT_CACHEDIDS;
            flagSyncCachedIdsDone = true;
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
    private boolean progressSelfShown = false;
    private boolean progressConvShown = false;
    private boolean progressNotesShown = false;
    private boolean progressCachedIdsShown = false;

    @Override
    public void handleMessage(Message msg) {
      progressButton1.setProgress(totalProgress);
      if (flagSyncSelfDone && !progressSelfShown) {
        progressText.setText("My Card Up to Date");
        progressSelfShown = true;
      }
      if (flagSyncConvDone && !progressConvShown) {
        progressText.setText("Notifications Up to Date");
        progressConvShown = true;
      }
      if (flagSyncCachedIdsDone && !progressCachedIdsShown) {
        progressText.setText("Cleared Offline Collections");
        progressCachedIdsShown = true;
      }
      if (flagSyncNotesDone && !progressNotesShown) {
        progressText.setText("Card Collection Up to Date");
        progressNotesShown = true;
      }
      if (totalProgress == 100) {
        // if there is network, wait till self sync completes before finishing
        // BufferOpening
        // check portrait again
        checkPortrait();
        Intent intent = new Intent(ActivityBufferOpening.this,
          ActivityMain.class);
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
    ParseObject object;
    try {
      object = query.get(AppGlobals.currentUser.get("ecardId").toString());
      byte[] tmpImgData = (byte[]) object.get("tmpImgByteArray");
      if (tmpImgData != null) {
        imgFromTmpData = true;
      } else {
        imgFromTmpData = false;
      }
    } catch (ParseException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

}
