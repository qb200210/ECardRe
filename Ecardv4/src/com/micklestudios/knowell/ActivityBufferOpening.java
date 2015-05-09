package com.micklestudios.knowell;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
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
import android.os.Environment;
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
  private static final long COMPANYNAME_TIMEOUT = 60000;
  Integer WEIGHT_SELF = 20;
  Integer WEIGHT_NOTES = 20;
  Integer WEIGHT_CONV = 20;
  Integer WEIGHT_CACHEDIDS = 20;
  Integer totalProgress = 0;
  public static final String MY_PREFS_NAME = "KnoWellSyncParams";
  private static final String KNOWELL_ROOT = "KnoWell";
  ParseUser currentUser;
  // flag to see if there is portrait cached offline that cannot be converted
  // to ParseFile yet.
  boolean imgFromTmpData = false;

  private boolean timeoutFlagSelf = false;
  private boolean timeoutFlagConv = false;
  private boolean timeoutFlagNotes = false;
  private boolean timeoutFlagCachedIds = false;
  private boolean timeoutFlagCompany = false;
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
    
    // if tmpImgByteArray not null, need to convert to img file regardless
    // of network
    checkPortrait();
    checkFolders();
    
    // check sharedpreferences
    final SharedPreferences prefs = getSharedPreferences(MY_PREFS_NAME,
      MODE_PRIVATE);
    SharedPreferences.Editor prefEditor = prefs.edit();

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

      

      // syncing data within given timeout
      // when self sync done, transition
      syncAllDataUponOpening(prefs, prefEditor);
    } else {
      // no network, jump into ActivityMain

      Timer timer = new Timer();
      timer.schedule(new TimerTask() {

        @Override
        public void run() {
          // if no network, generate userInfo objects directly from
          // localDataStore
          getContacts();
          getConvContacts();
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
  
  private void checkFolders() {
    String filepath = Environment.getExternalStorageDirectory().getPath();
    File file = new File(filepath, KNOWELL_ROOT);
    if (!file.exists()) {
      file.mkdirs();
    }
  }

  protected void createCompanyNamesFromLocal(SharedPreferences prefs) {
    //Retrieve the saved company list from sharedpreference
    // this will be used to populate autocomplete box
    ActivityDesign.companyNames = new ArrayList<String>();
    Set<String> tmpCompanyNames = new HashSet<String>();
    tmpCompanyNames = prefs.getStringSet("listOfCompanyNames", null);
    if(tmpCompanyNames != null){
      ActivityDesign.companyNames.addAll(tmpCompanyNames);
    }    
  }

  protected void getConvContacts() {
    Log.i("actbuf", "inside getconvcontacts");
    ActivityConversations.potentialUsers = new ArrayList<UserInfo>();
    /* A map of all the ECardNote objects to the noteID */
    final HashMap<String, ParseObject> infoIdToConvObjectMap = new HashMap<String, ParseObject>();
    // During SyncConversations, all conversations should have been synced to local
    ParseQuery<ParseObject> queryConvs = ParseQuery.getQuery("Conversations");
    queryConvs.fromLocalDatastore();
    queryConvs.whereEqualTo("partyB", currentUser.get("ecardId").toString());
    List<ParseObject> objectConvList = null;
    try {
      objectConvList = queryConvs.find();
    } catch (ParseException e) {
      e.printStackTrace();
    }
    if(objectConvList != null && objectConvList.size() != 0){
      // If there are conversations, don't worry about notes yet, just create userInfo using ecards
      for(Iterator<ParseObject> iter = objectConvList.iterator(); iter.hasNext();){
        ParseObject objectConv = iter.next();
        // don't need to check if the conversation is deleted, because that should be done by SyncConversations
        String infoObjectId = objectConv.get("partyA").toString();
        infoIdToConvObjectMap.put(infoObjectId, objectConv);
      }
      /*
       * Now, query the ECardInfoTable to get all the ECardInfo for the conversations
       * collected here.
       */
      ParseQuery<ParseObject> queryInfo = ParseQuery.getQuery("ECardInfo");
      queryInfo.fromLocalDatastore();
      queryInfo.whereContainedIn("objectId", infoIdToConvObjectMap.keySet());
      List<ParseObject> objectInfoList = null;
      try {
        objectInfoList = queryInfo.find();
      } catch (ParseException e) {
        e.printStackTrace();
      }
      Log.i("actbuf", " "+ objectConvList.size()+ " "+ objectInfoList.size() );
        
      if(objectInfoList != null && objectInfoList.size() != 0){
        for(Iterator<ParseObject> iter = objectInfoList.iterator(); iter.hasNext();){
          ParseObject objectInfo = iter.next();
          UserInfo contact = new UserInfo(objectInfo);
          if(contact != null){
            Log.i("actbuf", contact.getFirstName());
            // No need to put note as part of UserInfo -- will execute note_query from localdatastore later
            // Dont need to keep mapping to actual conversations objects -- they are not as critical
            ActivityConversations.potentialUsers.add(contact);
          }
        }
      }        
    }
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
                  // Create sets to add the strings we find in the contacts.
                  HashSet<String> setCompany = new HashSet<String>();
                  HashSet<String> setWhereMet = new HashSet<String>();
                  HashSet<String> setEventMet = new HashSet<String>();
                  HashSet<String> setAll = new HashSet<String>();

                  ActivitySearch.autoCompleteListAll = new ArrayList<String>();
                  ActivitySearch.autoCompleteListCompany = new ArrayList<String>();
                  ActivitySearch.autoCompleteListEvent = new ArrayList<String>();
                  ActivitySearch.autoCompleteListWhere = new ArrayList<String>();

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
                      contact.setEventMet(objectNote.getString("event_met"));
                      contact.setWhereMet(objectNote.getString("where_met"));
                      contact.setNote(objectNote.getString("notes"));

                      ActivitySearch.allUsers.add(contact);

                      setCompany.add(contact.getCompany());
                      setWhereMet.add(contact.getWhereMet());
                      setEventMet.add(contact.getEventMet());
                      setAll.addAll(contact.getAllStrings());
                    }
                  }
                  ActivitySearch.autoCompleteListCompany.addAll(setCompany);
                  ActivitySearch.autoCompleteListEvent.addAll(setEventMet);
                  ActivitySearch.autoCompleteListWhere.addAll(setWhereMet);
                  ActivitySearch.autoCompleteListAll.addAll(setAll);
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
          Toast.makeText(getApplicationContext(), "Self Copy Timed Out",
            Toast.LENGTH_SHORT).show();
          timeoutFlagSelf = true;
          createSelfCopy.cancel(true);
        }
      }
    }, CREATE_SELF_COPY_TIMEOUT);
    
    // update the local string list for available company templates -- only the names, not the actual object
    final AsyncTasks.SyncDataCompanyNames syncCompanyNames = new AsyncTasks.SyncDataCompanyNames(this, prefs, prefEditor);
    syncCompanyNames.execute();
    Handler handlerCompanyNames = new Handler();
    handlerCompanyNames.postDelayed(new Runnable() {

      @Override
      public void run() {
        if (syncCompanyNames.getStatus() == AsyncTask.Status.RUNNING) {
          Toast.makeText(getApplicationContext(), "Company List Timed Out",
            Toast.LENGTH_SHORT).show();
          timeoutFlagCompany = true;
          syncCompanyNames.cancel(true);
        }
      }
    }, COMPANYNAME_TIMEOUT);

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
            "Sync Conversations Timed Out", Toast.LENGTH_SHORT).show();
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
          Toast.makeText(getApplicationContext(), "Sync Notes Timed Out",
            Toast.LENGTH_SHORT).show();
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
          Toast.makeText(getApplicationContext(), "Sync CachedIds Timed Out",
            Toast.LENGTH_SHORT).show();
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
          if (totalProgress == 80) {
            Log.i("msg", "all others complete");
            // when all other syncs complete, generate UserInfo objects from
            // LocalDataStore
            getContacts();
            getConvContacts();
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
    @Override
    public void handleMessage(Message msg) {
      progressButton1.setProgress(totalProgress);
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
      object = query.get(currentUser.get("ecardId").toString());
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
