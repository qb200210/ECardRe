package com.micklestudios.knowell.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;
import android.app.Activity;

import com.micklestudios.knowell.ActivityBufferOpening;
import com.micklestudios.knowell.ActivityDesign;
import com.micklestudios.knowell.ActivityDetails;
import com.micklestudios.knowell.ActivityMain;
import com.micklestudios.knowell.ActivityScanned;
import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.ParseACL;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParsePush;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.micklestudios.knowell.R;

public class AsyncTasks {

  public static class SyncDataTaskHistory extends
      AsyncTask<String, Void, String> {

    private Context context;
    private ParseUser currentUser;
    private SharedPreferences prefs;
    private SharedPreferences.Editor prefEditor;
    private boolean flagShouldSync;
    private boolean flagToast;

    public SyncDataTaskHistory(Context context, ParseUser currentUser,
      SharedPreferences prefs, SharedPreferences.Editor prefEditor,
      boolean flagToast) {
      this.context = context;
      this.currentUser = currentUser;
      this.prefs = prefs;
      this.prefEditor = prefEditor;
      this.flagShouldSync = false;
      this.flagToast = flagToast;
    }

    @Override
    protected String doInBackground(String... params) {
      // get the stored shared last sync date, if null, default to 1969
      long millis = prefs.getLong("DateHistorySynced", 0L);
      Date lastSyncedDate = new Date(millis);

      long start = System.nanoTime();

      // Only pull those data when there is actual update

      ParseQuery query = ParseQuery.getQuery("History");
      query.whereEqualTo("userId", currentUser.getObjectId().toString());
      query.whereGreaterThan("updatedAt", lastSyncedDate);
      List<ParseObject> histObjects = null;
      try {
        histObjects = query.find();
      } catch (ParseException e2) {
        // TODO Auto-generated catch block
        e2.printStackTrace();
      }

      long elapsedTime = System.nanoTime() - start;
      Log.d("timer", "history: " + elapsedTime * 1e-9);

      if (histObjects != null && histObjects.size() != 0) {
        flagShouldSync = true;
        for (Iterator<ParseObject> iter = histObjects.iterator(); iter
          .hasNext();) {
          ParseObject objHist = iter.next();
          if (objHist.get("isDeleted") != null) {
            if ((boolean) objHist.get("isDeleted") == true) {
              try {
                // unpin the "deleted" object
                objHist.unpin();
                // remove it from the to-be-pinned list
                iter.remove();
              } catch (ParseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
              }
            }
          }
        }
        // pin histObjects
        try {
          ParseObject.pinAll(histObjects);
        } catch (ParseException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }

      // flush sharedpreference with today's date after all notes saved
      Date currentDate = new Date();
      prefEditor.putLong("DateHistorySynced", currentDate.getTime());
      prefEditor.commit();
      return null;
    }

    @Override
    protected void onPostExecute(String result) {
      if (flagToast)
        Toast.makeText(context, "History Up to Date", Toast.LENGTH_SHORT)
          .show();
    }

  }

  // sync local copy of self ecard
  // now server always wins, should change to check date
  public static class SyncDataTaskSelfCopy extends
      AsyncTask<String, Void, String> {

    private Context context;
    private ParseUser currentUser;
    private SharedPreferences prefs;
    private SharedPreferences.Editor prefEditor;
    private boolean flagShouldSync;
    private boolean imgFromTmpData;
    private boolean flagToast;

    public SyncDataTaskSelfCopy(Context context, ParseUser currentUser,
      SharedPreferences prefs, SharedPreferences.Editor prefEditor,
      boolean flagToast) {
      this.context = context;
      this.currentUser = currentUser;
      this.prefs = prefs;
      this.prefEditor = prefEditor;
      this.flagShouldSync = false;
      this.flagToast = flagToast;
    }

    @Override
    protected String doInBackground(String... url) {
      // get the stored shared last sync date, if null, default to 1969
      long millis = prefs.getLong("DateSelfSynced", 0L);
      long millisUser = prefs.getLong("DateSelfUserSynced", 0L);
      Date lastSyncedDateSelf = new Date(millis);
      Date lastSyncedDateSelfUser = new Date(millisUser);

      long start = System.nanoTime();

      ParseQuery<ParseUser> queryUser = ParseUser.getQuery();
      queryUser.whereGreaterThan("updatedAt", lastSyncedDateSelfUser);
      queryUser.whereEqualTo("objectId", currentUser.getObjectId());
      List<ParseUser> userObjects = null;
      try {
        userObjects = queryUser.find();
      } catch (ParseException e1) {
        // TODO Auto-generated catch block
        e1.printStackTrace();
      }
      long elapsedTime = System.nanoTime() - start;
      Log.d("timer", "selfchecknew: " + elapsedTime * 1e-9);

      start = System.nanoTime();

      if (userObjects != null && userObjects.size() != 0) {
        flagShouldSync = true;
        try {
          userObjects.get(0).pin();
          // flush sharedpreference with today's date
          Date currentDate = new Date();
          prefEditor.putLong("DateSelfUserSynced", currentDate.getTime());
          prefEditor.commit();
        } catch (ParseException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }

      }

      elapsedTime = System.nanoTime() - start;
      Log.d("timer", "pinselfusr: " + elapsedTime * 1e-9);

      start = System.nanoTime();

      ParseQuery<ParseObject> query = ParseQuery.getQuery("ECardInfo");
      // constraint: server value newer
      // if tmpImgByteArray was used, lastSyncedDate will be set to 1969, so
      // that it will make sure the
      // parse object is pulled and the parsefile created from tmpImgByteArray
      query.whereGreaterThan("updatedAt", lastSyncedDateSelf);
      query.whereEqualTo("objectId", currentUser.get("ecardId").toString());
      List<ParseObject> infoObjects = null;
      try {
        infoObjects = query.find();
      } catch (ParseException e1) {
        // TODO Auto-generated catch block
        e1.printStackTrace();
      }

      if (infoObjects != null && infoObjects.size() != 0) {
        flagShouldSync = true;
        // if there is a newer version on server, then sync it to local
        final ParseObject infoObjectTmp = infoObjects.get(0);
        // for webui, should do the same, check this array whenever can, then
        // convert it to parseFile
        byte[] tmpImgData = (byte[]) infoObjects.get(0).get("tmpImgByteArray");
        if (tmpImgData != null) {
          // if there is cached data in the array on server, convert to
          // ParseFile then clear the array
          final ParseFile file = new ParseFile("portrait.jpg", tmpImgData);
          try {
            file.save();
            Log.i("self copy", "Cached portrait saved!");
            infoObjectTmp.put("portrait", file);
            infoObjectTmp.remove("tmpImgByteArray");
            // do not use saveEventually, easily leads to corrupted data
            infoObjectTmp.save();
          } catch (ParseException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
          }
        }
        try {
          infoObjects.get(0).pin();
          // flush sharedpreference with today's date
          Date currentDate = new Date();
          prefEditor.putLong("DateSelfSynced", currentDate.getTime());
          prefEditor.commit();
        } catch (ParseException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }

      elapsedTime = System.nanoTime() - start;
      Log.d("timer", "pinselfcard: " + elapsedTime * 1e-9);
      return null;
    }

    @Override
    protected void onPostExecute(String result) {
      if (flagToast) {
        Toast.makeText(context, "My Card Up to Date", Toast.LENGTH_SHORT)
          .show();
      }
    }

  }

  // sync local copy of notes and corresponding ecards
  // this one is not critically depended on, so it doesn't have to be blocking
  // upon opening
  public static class SyncDataCompanyNames extends
      AsyncTask<String, Void, String> {

    private Context context;
    private SharedPreferences prefs;
    private SharedPreferences.Editor prefEditor;
    private boolean flagShouldSync;
    private boolean flagToast;

    public SyncDataCompanyNames(Context context, SharedPreferences prefs,
      SharedPreferences.Editor prefEditor, boolean flagToast) {
      this.context = context;
      this.prefs = prefs;
      this.prefEditor = prefEditor;
      this.flagShouldSync = false;
      this.flagToast = flagToast;
    }

    @Override
    protected String doInBackground(String... params) {
      // Retrieve the saved company list from sharedpreference
      // this will be used to populate autocomplete box
      ActivityDesign.companyNames = new ArrayList<String>();
      Set<String> tmpCompanyNames = new HashSet<String>();
      Set<String> fetchedCompanyList = prefs.getStringSet("listOfCompanyNames",
        null);
      if (fetchedCompanyList != null) {
        tmpCompanyNames.addAll(fetchedCompanyList);
      }

      long start = System.nanoTime();

      // get the stored shared last sync date, if null, default to 1969
      long millis = prefs.getLong("DateCompanySynced", 0L);
      Date lastSyncedDate = new Date(millis);
      ParseQuery<ParseObject> queryTemplate = ParseQuery
        .getQuery("ECardTemplate");
      queryTemplate.whereGreaterThan("updatedAt", lastSyncedDate);
      List<ParseObject> templateObjs = null;
      try {
        templateObjs = queryTemplate.find();
      } catch (ParseException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      if (templateObjs != null && templateObjs.size() != 0) {
        for (Iterator<ParseObject> iter = templateObjs.iterator(); iter
          .hasNext();) {
          ParseObject templateObj = iter.next();
          tmpCompanyNames.add(templateObj.get("companyName").toString());
        }
        // Set the values
        prefEditor.putStringSet("listOfCompanyNames", tmpCompanyNames);
        prefEditor.commit();
      }
      if (tmpCompanyNames != null) {
        ActivityDesign.companyNames.addAll(tmpCompanyNames);
      }

      long elapsedTime = System.nanoTime() - start;
      Log.d("timer", "updtprefcmpnames: " + elapsedTime * 1e-9);

      start = System.nanoTime();
      // pin down logos for those ecards that are already in localstore
      ParseQuery<ParseObject> queryInfo = ParseQuery.getQuery("ECardInfo");
      queryInfo.fromLocalDatastore();
      List<ParseObject> infoObjs = null;
      try {
        infoObjs = queryInfo.find();
      } catch (ParseException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      if (infoObjs != null && infoObjs.size() != 0) {
        HashSet<String> companyNames = new HashSet<String>();
        Object tmpString;
        for (ParseObject infoObj : infoObjs) {
          tmpString = infoObj.get("company");
          if (tmpString != null) {
            companyNames.add(tmpString.toString());
          }
        }
        // find the templates for local ecards
        ParseQuery<ParseObject> queryTemplate1 = ParseQuery
          .getQuery("ECardTemplate");
        queryTemplate1.whereContainedIn("companyName", companyNames);
        List<ParseObject> templateObjs1 = null;
        try {
          // found online template list satisfying collected ecards
          templateObjs1 = queryTemplate1.find();
        } catch (ParseException e) {
          e.printStackTrace();
        }
        if (templateObjs1 != null && templateObjs1.size() != 0) {
          // loop over the found templates and pin those not yet in
          // localdatastore
          // loop over found templates and record objId
          ArrayList<String> objIds = new ArrayList<String>();
          for (Iterator<ParseObject> iter = templateObjs1.iterator(); iter
            .hasNext();) {
            ParseObject templateObj = iter.next();
            objIds.add(templateObj.getObjectId().toString());
          }
          // figure out what templates are already in localdatastore
          ParseQuery<ParseObject> queryTemplateLocal = ParseQuery
            .getQuery("ECardTemplate");
          queryTemplateLocal.fromLocalDatastore();
          queryTemplateLocal.whereContainedIn("objectId", objIds);
          List<ParseObject> templateObjs2 = null;
          try {
            // those template list already exist in localdatastore
            templateObjs2 = queryTemplateLocal.find();
          } catch (ParseException e1) {
            e1.printStackTrace();
          }
          if (templateObjs2 != null && templateObjs2.size() != 0) {
            for (Iterator<ParseObject> iter1 = templateObjs1.iterator(); iter1
              .hasNext();) {
              ParseObject objOnline = iter1.next();
              // remove already existed local records from found online list
              for (Iterator<ParseObject> iter2 = templateObjs2.iterator(); iter2
                .hasNext();) {
                ParseObject objOffline = iter2.next();
                if (objOnline.getObjectId() == objOffline.getObjectId()) {
                  // duplicate record found
                  if (!objOnline.getUpdatedAt()
                    .after(objOffline.getUpdatedAt())) {
                    // if offline record is latest, remove this duplicate online
                    // record
                    iter1.remove();
                    break;
                  }
                }
              }
            }
          }
          try {
            // pin down the template list
            if (templateObjs1 != null && templateObjs1.size() != 0) {
              ParseObject.pinAll(templateObjs1);
            }
          } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        }

      }

      elapsedTime = System.nanoTime() - start;
      Log.d("timer", "synccompnames: " + elapsedTime * 1e-9);

      // flush sharedpreference with today's date
      Date currentDate = new Date();
      prefEditor.putLong("DateCompanySynced", currentDate.getTime());
      prefEditor.commit();

      return null;
    }

  }

  // sync local copy of notes and corresponding ecards
  public static class SyncDataTaskNotes extends AsyncTask<String, Void, String> {

    private Context context;
    private ParseUser currentUser;
    private SharedPreferences prefs;
    private SharedPreferences.Editor prefEditor;
    private boolean flagShouldSync;
    private boolean flagToast;

    public SyncDataTaskNotes(Context context, ParseUser currentUser,
      SharedPreferences prefs, SharedPreferences.Editor prefEditor,
      boolean flagToast) {
      this.context = context;
      this.currentUser = currentUser;
      this.prefs = prefs;
      this.prefEditor = prefEditor;
      this.flagShouldSync = false;
      this.flagToast = flagToast;
    }

    @Override
    protected String doInBackground(String... params) {
      // get the stored shared last sync date, if null, default to 1969
      long millis = prefs.getLong("DateNoteSynced", 0L);
      Date lastSyncedDate = new Date(millis);

      long start = System.nanoTime();
      long elapsedTime;

      ParseQuery<ParseObject> query = ParseQuery.getQuery("ECardNote");
      query.whereEqualTo("userId", currentUser.getObjectId());
      query.whereGreaterThan("updatedAt", lastSyncedDate);
      List<ParseObject> noteObjects = null;
      try {
        noteObjects = query.find();
      } catch (ParseException e1) {
        e1.printStackTrace();
      }
      if (noteObjects != null && noteObjects.size() != 0) {
        // If some of the notes have been updated, otherwise skip the rest
        flagShouldSync = true;

        for (Iterator<ParseObject> iter = noteObjects.iterator(); iter
          .hasNext();) {
          ParseObject objNote = iter.next();
          // unpin those notes that are deleted along with the ecards
          if (objNote.get("isDeleted") != null) {
            if ((boolean) objNote.get("isDeleted") == true) {
              try {
                // unpin the "deleted" object
                objNote.unpin();
                if (objNote.get("ecardId").toString() != currentUser.get(
                  "ecardId").toString()) {
                  // If the note is with self, then unpinning card may result in
                  // crash
                  // unpin corresponding local Ecard copy
                  ParseQuery queryInfoLocal = ParseQuery.getQuery("ECardInfo");
                  queryInfoLocal.fromLocalDatastore();
                  ParseObject objEcard = queryInfoLocal.get(objNote.get(
                    "ecardId").toString());
                  objEcard.unpin();
                }
                // remove the note from the to-be-pinned list
                iter.remove();
              } catch (ParseException e) {
                e.printStackTrace();
              }
            }
          }
        }
        // Now the remaining list is for updated Notes, this part is to cache
        // all parseFiles
        for (Iterator<ParseObject> iter = noteObjects.iterator(); iter
          .hasNext();) {
          ParseObject objNote = iter.next();
          byte[] tmpVoiceData = (byte[]) objNote.get("tmpVoiceByteArray");
          if (tmpVoiceData != null) {
            // if there is cached data in the array on server, convert to
            // ParseFile then clear the array
            final ParseFile file = new ParseFile("voicenote.mp4", tmpVoiceData);
            try {
              file.save();
              Log.i("notes", "Cached voice note saved!");
              objNote.put("voiceNotes", file);
              objNote.remove("tmpVoiceByteArray");
              // do not use saveEventually, easily leads to corrupted data
              objNote.save();
            } catch (ParseException e1) {
              // TODO Auto-generated catch block
              e1.printStackTrace();
            }
          }
          if (objNote.get("voiceNotes") != null) {
            // This is to cache all associated parseFiles
            ParseFile voiceNote = (ParseFile) objNote.get("voiceNotes");
            try {
              // dummy statement to force caching data
              voiceNote.getData();
            } catch (ParseException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
            }
          }
        }

        // pin noteObjects -- parseFiles have already been cached before
        try {
          ParseObject.pinAll(noteObjects);
        } catch (ParseException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }

      elapsedTime = System.nanoTime() - start;
      Log.d("timer", "notepinned: " + elapsedTime * 1e-9);

      // flush sharedpreference with today's date after all notes saved
      Date currentDate = new Date();
      prefEditor.putLong("DateNoteSynced", currentDate.getTime());
      prefEditor.commit();

      // This part is about checking those ecards that gets updated

      long millisInfo = prefs.getLong("DateInfoSynced", 0L);
      Date lastInfoSyncedDate = new Date(millis);

      start = System.nanoTime();

      // generate list of ecards
      ParseQuery<ParseObject> queryNote = ParseQuery.getQuery("ECardNote");
      queryNote.fromLocalDatastore();
      queryNote.whereEqualTo("userId", currentUser.getObjectId().toString());
      List<ParseObject> noteObjs = null;
      try {
        noteObjs = queryNote.find();
      } catch (ParseException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      TreeSet<String> ecardIdsTree = new TreeSet();
      if (noteObjs != null) {
        // if the current user has at least one note
        for (ParseObject noteObj : noteObjs) {
          ecardIdsTree.add(noteObj.get("ecardId").toString());
        }
      }

      // now check and pin down those updated ecards
      // TO-DO: For now ecards are not deletable.
      ParseQuery queryInfo = new ParseQuery<ParseObject>("ECardInfo");
      queryInfo.whereContainedIn("objectId", ecardIdsTree);
      queryInfo.whereGreaterThan("updatedAt", lastInfoSyncedDate);
      List<ParseObject> infoObjects = null;
      try {
        infoObjects = queryInfo.find();
      } catch (ParseException e1) {
        e1.printStackTrace();
      }
      // pin infoObjects
      if (infoObjects != null) {
        for (ParseObject objInfo : infoObjects) {
          // This is to cache all associated parseFiles
          // TO-DO: any better way? This is going to cause a lot of traffic
          ParseFile portraitImg = (ParseFile) objInfo.get("portrait");
          if (portraitImg != null) {
            try {
              portraitImg.getData();
            } catch (ParseException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
            }
          }
        }
        try {
          ParseObject.pinAll(infoObjects);
        } catch (ParseException e1) {
          e1.printStackTrace();
        }
      }

      elapsedTime = System.nanoTime() - start;
      Log.d("timer", "notecardpined: " + elapsedTime * 1e-9);

      // TO-DO: Create Ecard Update notifications as the "news feed"

      // flush sharedpreference with today's date after all notes saved
      Date currentDate1 = new Date();
      prefEditor.putLong("DateInfoSynced", currentDate1.getTime());
      prefEditor.commit();
      return null;
    }

    @Override
    protected void onPostExecute(String result) {
      if (flagToast) {
        Toast.makeText(context, "Card Collection Up to Date",
          Toast.LENGTH_SHORT).show();
      }
    }

  }

  public static class SyncDataTaskConversations extends
      AsyncTask<String, Void, String> {

    private Context context;
    private ParseUser currentUser;
    private SharedPreferences prefs;
    private SharedPreferences.Editor prefEditor;
    private boolean flagShouldSync;
    private boolean flagToast;

    public SyncDataTaskConversations(Context context, ParseUser currentUser,
      SharedPreferences prefs, SharedPreferences.Editor prefEditor,
      boolean flagToast) {
      this.context = context;
      this.currentUser = currentUser;
      this.prefs = prefs;
      this.prefEditor = prefEditor;
      this.flagShouldSync = false;
      this.flagToast = flagToast;
    }

    @Override
    protected String doInBackground(String... params) {

      // remove those conversations associated with cards that are already
      // collected
      // find all local conversations
      ParseQuery<ParseObject> queryLocal = ParseQuery.getQuery("Conversations");
      queryLocal.whereEqualTo("partyB", currentUser.get("ecardId").toString());
      queryLocal.fromLocalDatastore();
      List<ParseObject> convObjs = null;
      try {
        convObjs = queryLocal.find();
      } catch (ParseException e3) {
        e3.printStackTrace();
      }
      TreeSet<String> ecardIdsToCollect = new TreeSet();
      if (convObjs != null) {
        if (convObjs.size() > 0) {
          for (ParseObject convObj : convObjs) {
            // record all ecards associated with local conversations
            ecardIdsToCollect.add(convObj.get("partyA").toString());
          }
          // check notes to see if the note corresponding to this ecard in conv
          // already exist locally
          ParseQuery<ParseObject> queryNote = ParseQuery.getQuery("ECardNote");
          queryNote.whereContainedIn("ecardId", ecardIdsToCollect);
          queryNote.whereNotEqualTo("isDeleted", true);
          queryNote
            .whereEqualTo("userId", currentUser.getObjectId().toString());
          queryNote.fromLocalDatastore();
          List<ParseObject> noteObjs = null;
          try {
            noteObjs = queryNote.find();
          } catch (ParseException e) {
            e.printStackTrace();
          }
          TreeSet<String> ecardIdsToRemove = new TreeSet();
          if (noteObjs != null) {
            if (noteObjs.size() > 0) {
              for (ParseObject noteObj : noteObjs) {
                ecardIdsToRemove.add(noteObj.get("ecardId").toString());
              }
            }
          }
          // remove those conversations
          for (ParseObject convObj : convObjs) {
            if (ecardIdsToRemove.contains(convObj.get("partyA").toString())) {
              // if a conversation object corresponds to an existing note,
              // remove it
              convObj.put("isDeleted", true);
              try {
                convObj.save();
                convObj.unpin();
              } catch (ParseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
              }
            }
          }

        }
      }

      // get the stored shared last sync date, if null, default to 1969
      long millis = prefs.getLong("DateConversationsSynced", 0L);
      Date lastSyncedDate = new Date(millis);

      long start = System.nanoTime();
      long elapsedTime;

      ParseQuery<ParseObject> query = ParseQuery.getQuery("Conversations");
      query.whereEqualTo("partyB", currentUser.get("ecardId").toString());
      query.whereGreaterThan("updatedAt", lastSyncedDate);
      List<ParseObject> convObjects = null;
      try {
        convObjects = query.find();
      } catch (ParseException e2) {
        // TODO Auto-generated catch block
        e2.printStackTrace();
      }
      if (convObjects != null && convObjects.size() != 0) {
        flagShouldSync = true;
        for (Iterator<ParseObject> iter = convObjects.iterator(); iter
          .hasNext();) {
          ParseObject objConv = iter.next();
          if (objConv.get("isDeleted") != null) {
            if ((boolean) objConv.get("isDeleted") == true) {
              try {
                // unpin the "deleted" object
                objConv.unpin();
                // remove it from the to-be-pinned list
                iter.remove();
              } catch (ParseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
              }
            }
          }
        }

        if (convObjects.size() != 0) {
          // only proceed to pin conversations and ecards if there are remaining
          // conv items

          TreeSet<String> ecardIdsTree = new TreeSet<String>();
          for (Iterator<ParseObject> iter = convObjects.iterator(); iter
            .hasNext();) {
            ParseObject objConv = iter.next();
            ecardIdsTree.add(objConv.get("partyA").toString());
          }

          Log.i("ecardIds", ecardIdsTree.toString());

          elapsedTime = System.nanoTime() - start;
          Log.d("timer", "convchked: " + elapsedTime * 1e-9);

          start = System.nanoTime();

          // find associated Ecards
          ParseQuery<ParseObject> query1 = ParseQuery.getQuery("ECardInfo");
          query1.whereContainedIn("objectId", ecardIdsTree);
          List<ParseObject> infoObjects = null;
          try {
            infoObjects = query1.find();
          } catch (ParseException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
          }
          // pin infoObjects
          Log.i("infoObjects", "a+" + infoObjects.size());

          if (infoObjects != null) {
            try {
              ParseObject.pinAll(infoObjects);
              Log.i("infoObjects", "done+" + infoObjects.size());

            } catch (ParseException e1) {
              e1.printStackTrace();
            }
            for (Iterator<ParseObject> iter = infoObjects.iterator(); iter
              .hasNext();) {
              ParseObject objInfo = iter.next();
              // This is to cache all associated parseFiles
              ParseFile portraitImg = (ParseFile) objInfo.get("portrait");
              if (portraitImg != null) {
                try {
                  portraitImg.getData();
                } catch (ParseException e) {
                  // TODO Auto-generated catch block
                  e.printStackTrace();
                }
              }
            }
          }
          // pin convObjects
          try {
            ParseObject.pinAll(convObjects);
          } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        }
      }

      elapsedTime = System.nanoTime() - start;
      Log.d("timer", "convcardpined: " + elapsedTime * 1e-9);

      // flush sharedpreference with today's date after all notes saved
      Date currentDate = new Date();
      prefEditor.putLong("DateConversationsSynced", currentDate.getTime());
      prefEditor.commit();
      return null;
    }

    @Override
    protected void onPostExecute(String result) {
      if (flagToast)
        Toast.makeText(context, "Notifications Up to Date", Toast.LENGTH_SHORT)
          .show();
    }

  }

  public static class SyncDataTaskCachedIds extends
      AsyncTask<String, Void, String> {

    private Context context;
    private ParseUser currentUser;
    private ECardSQLHelperCachedIds db;
    private ECardSQLHelperCachedShares dbShares;
    List<String> scannedIDs;
    List<String> partyBs;
    List<OfflineDataCachedIds> olDatas;
    List<OfflineDataCachedShares> olDatasShares;
    private boolean flagShouldSync;
    private SharedPreferences prefs;
    private SharedPreferences.Editor prefEditor;
    private boolean flagToast;

    public SyncDataTaskCachedIds(Context context, ParseUser currentUser,
      SharedPreferences prefs, SharedPreferences.Editor prefEditor,
      boolean flagToast) {
      this.context = context;
      this.currentUser = currentUser;
      this.prefs = prefs;
      this.prefEditor = prefEditor;
      this.flagShouldSync = false;
      this.flagToast = flagToast;
    }

    @Override
    protected String doInBackground(String... params) {
      // Upon opening, if there is Internet connection, try to store cached IDs
      db = new ECardSQLHelperCachedIds(context);
      // getting all local db data to check against EcardIds
      olDatas = db.getAllData();
      if (olDatas.size() != 0) {
        flagShouldSync = true;
        Log.i("CachedIds", "Found unsaved Ecards");
        // If there are unsaved offline list, check and save them
        scannedIDs = new LinkedList<String>();
        for (Iterator<OfflineDataCachedIds> iter = olDatas.iterator(); iter
          .hasNext();) {
          OfflineDataCachedIds olData = iter.next();
          String scannedID = olData.getEcardID();
          scannedIDs.add(scannedID);
        }
        addCachedEcardIds();
      }

      // check if there is cached shares too
      dbShares = new ECardSQLHelperCachedShares(context);
      // getting all local db data to check against EcardIds
      olDatasShares = dbShares.getAllData();
      if (olDatasShares.size() != 0) {
        flagShouldSync = true;
        Log.i("CachedShares", "Found unsent Shares");
        // If there are unsaved offline list, check and save them
        partyBs = new LinkedList<String>();
        for (Iterator<OfflineDataCachedShares> iter = olDatasShares.iterator(); iter
          .hasNext();) {
          OfflineDataCachedShares olData = iter.next();
          String partyB = olData.getPartyB();
          partyBs.add(partyB);
        }
        sendCachedShares();
      }

      return null;
    }

    private void sendCachedShares() {
      // Check whether the partyB (target ecardId) actually exists
      ParseQuery<ParseObject> query = ParseQuery.getQuery("ECardInfo");
      query.whereContainedIn("objectId", partyBs);
      List<ParseObject> infoObjects = null;
      try {
        infoObjects = query.find();
      } catch (ParseException e2) {
        e2.printStackTrace();
      }
      List<String> partyBsRemained = new ArrayList<String>();
      if (infoObjects != null) {
        // if at least some of ecardIds in partyBs are valid, record them, then
        // ignore the rest
        for (Iterator<ParseObject> iter = infoObjects.iterator(); iter
          .hasNext();) {
          ParseObject obj = iter.next();
          partyBsRemained.add(obj.getObjectId().toString());
          partyBs.remove(obj.getObjectId().toString());
        }
        for (Iterator<String> iter = partyBs.iterator(); iter.hasNext();) {
          // these Ids that correspond to non-existent ecard will be stored in
          // partyBs, remove them from local
          final String idsToRemove = iter.next();
          List<OfflineDataCachedShares> olDatas = dbShares.getData("partyB",
            idsToRemove);
          dbShares.deleteData(olDatas.get(0));
        }
        // refill the partyBs with valid Ids
        partyBs = partyBsRemained;
        // over here should already have a list of valid partyBs
        // Now check if the shares exist online, if yes, flip them, then add the
        // rest
        ParseQuery<ParseObject> queryConv = ParseQuery
          .getQuery("Conversations");
        queryConv.whereEqualTo("partyA", currentUser.get("ecardId").toString());
        queryConv.whereContainedIn("partyB", partyBs);
        List<ParseObject> listConvs = null;
        try {
          listConvs = queryConv.find();
        } catch (ParseException e1) {
          // TODO Auto-generated catch block
          e1.printStackTrace();
        }
        ParseObject self = null;
        if (listConvs != null) {
          // If some of the conversations already exist, flip them then add the
          // rest
          // first get self info because ActivityMain isn't reached yet
          ParseQuery<ParseObject> querySelf = ParseQuery.getQuery("ECardInfo");
          querySelf.fromLocalDatastore();
          try {
            self = querySelf.get(currentUser.get("ecardId").toString());
          } catch (ParseException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
          }
          for (Iterator<ParseObject> iter = listConvs.iterator(); iter
            .hasNext();) {
            ParseObject obj = iter.next();
            obj.put("read", false);
            try {
              obj.save();
            } catch (ParseException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
            }
            // remove the already flipped conv from the list
            partyBs.remove(obj.get("partyB").toString());
            // remove from local database
            List<OfflineDataCachedShares> olDatas = dbShares.getData("partyB",
              obj.get("partyB").toString());
            dbShares.deleteData(olDatas.get(0));
            // send push notification
            sendPushNotification(obj.get("partyB").toString(), self);
          }
        }
        for (Iterator<String> iter = partyBs.iterator(); iter.hasNext();) {
          // these remained are the conv that doesn't exist, create them
          final String targetEcardId = iter.next();
          ParseObject object = new ParseObject("Conversations");
          object.put("partyA", currentUser.get("ecardId").toString());
          object.put("partyB", targetEcardId);
          object.put("read", false);
          try {
            object.save();
          } catch (ParseException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
          }
          // upon save of the new conversation, send out a push to the other
          // device
          sendPushNotification(targetEcardId, self);
        }
      } else {
        // none of ecardIds in partyBs is valid, delete them all
        for (Iterator<String> iter = partyBs.iterator(); iter.hasNext();) {
          // these Ids that correspond to non-existent ecard will be stored in
          // partyBs, remove them from local
          final String idsToRemove = iter.next();
          List<OfflineDataCachedShares> olDatas = dbShares.getData("partyB",
            idsToRemove);
          dbShares.deleteData(olDatas.get(0));
        }
        partyBs.clear();
      }
    }

    private void sendPushNotification(String targetEcardId, ParseObject self) {
      ParseQuery pushQuery = ParseInstallation.getQuery();
      pushQuery.whereEqualTo("ecardId", targetEcardId);
      JSONObject jsonObject = new JSONObject();
      try {
        if (self != null) {
          jsonObject.put("alert", "Hi, this is " + self.get("firstName") + " "
            + self.get("lastName") + ", please save my card.");
          jsonObject.put(
            "link",
            "https://www.micklestudios.com/search?id="
              + currentUser.get("ecardId").toString() + "&fn="
              + self.get("firstName") + "&ln=" + self.get("lastName"));
        } else {
          jsonObject.put("alert", "Hi, please save my card.");
          jsonObject
            .put("link", "https://www.micklestudios.com/search?id="
              + currentUser.get("ecardId").toString()
              + "&fn=Mysterious&ln=UserX");
        }
        jsonObject.put("action", "EcardOpenConversations");
      } catch (JSONException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      ParsePush push = new ParsePush();
      push.setQuery(pushQuery);
      push.setData(jsonObject);
      push.sendInBackground();
    }

    public void addCachedEcardIds() {
      ParseQuery<ParseObject> query = ParseQuery.getQuery("ECardInfo");
      query.whereContainedIn("objectId", scannedIDs);
      List<ParseObject> infoObjects = null;
      try {
        infoObjects = query.find();
      } catch (ParseException e2) {
        // TODO Auto-generated catch block
        e2.printStackTrace();
      }

      if (infoObjects == null) {
        // None in the saved EcardInfo IDs are valid, delete everything
        Log.i("addCachedEcardIds", "Entire list contains no valid EcardID");
        if (olDatas.size() != 0) {
          // if the cached userID don't exist, delete local records
          for (int i = 0; i < olDatas.size(); i++) {
            db.deleteData(olDatas.get(i));
          }
        }
      } else {
        // At least one Ecard objectId is valid
        List<String> ecardExistList = new LinkedList<String>();
        for (int i = 0; i < infoObjects.size(); i++) {
          ecardExistList.add(infoObjects.get(i).getObjectId());
          // create list of valid EcardIDs
        }
        for (Iterator<String> iter = scannedIDs.iterator(); iter.hasNext();) {
          String scannedID = iter.next();
          // loop over all local records and delete invalid ones
          if (!(ecardExistList.contains(scannedID))) {
            // if local record does not correspond to
            // existing ecardList, delete it
            List<OfflineDataCachedIds> olDatas = db.getData("ecardID",
              scannedID);
            db.deleteData(olDatas.get(0));
            // remove this record from scannedIDs
            iter.remove();
          }
          // if local record correspond to existing userList,
          // ready for updating colectedID list
        }

        // Now the scannedIDs is the record of fully valid
        // EcardInfo

        ParseQuery<ParseObject> queryNote = ParseQuery.getQuery("ECardNote");
        // need to do it from server so to avoid duplicate
        // adding due to out-of-sync
        queryNote.whereEqualTo("userId", currentUser.getObjectId());
        queryNote.whereContainedIn("ecardId", scannedIDs);
        List<ParseObject> noteObjects = null;
        try {
          noteObjects = queryNote.find();
        } catch (ParseException e2) {
          // TODO Auto-generated catch block
          e2.printStackTrace();
        }
        ArrayList<String> toRemove = new ArrayList<String>();
        if (noteObjects != null) {
          // these are the ecards that are already collected, including those
          // existed but deleted
          for (Iterator<ParseObject> iter = noteObjects.iterator(); iter
            .hasNext();) {
            ParseObject object = iter.next();
            if (object.getBoolean("isDeleted") == true) {
              // for those notes that exist but deleted, flip them
              object.put("isDeleted", false);
              try {
                object.save();
              } catch (ParseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
              }
            }
            Log.i("addCachedEcardIds", "ECard "
              + object.get("ecardId").toString() + " already existed!");
            toRemove.add(object.get("ecardId").toString());
            // Either way, delete the local db record.
            // This is because local db is only a temp storage for offline added
            // Ecards
            // Should be emptied when cards are collected
            List<OfflineDataCachedIds> olDatas = db.getData("ecardID", object
              .get("ecardId").toString());
            if (olDatas.size() != 0) {
              // if the record exists in local db, delete it
              OfflineDataCachedIds olData = olDatas.get(0);
              db.deleteData(olData);
            }
          }
        }
        if (!toRemove.isEmpty()) {
          // remove the records in scannedID that
          // are already collected
          scannedIDs.removeAll(toRemove);
        }
        // add the remaining unique ecards

        List<ParseObject> infoToBePinned = new ArrayList<ParseObject>();
        List<ParseObject> noteToBePinned = new ArrayList<ParseObject>();
        for (Iterator<String> iter = scannedIDs.iterator(); iter.hasNext();) {
          String scannedID = iter.next();
          ParseObject ecardNote = new ParseObject("ECardNote");
          ecardNote.setACL(new ParseACL(currentUser));
          ecardNote.put("userId", currentUser.getObjectId());
          ecardNote.put("ecardId", scannedID);
          // cannot know where the card was
          // collected since no network/ geoinfo
          // at that time
          // fetch the EcardInfo to be added to
          // extract some info that needs to be
          // placed into EcardNote
          ParseQuery<ParseObject> queryInfo = ParseQuery.getQuery("ECardInfo");
          ParseObject object = null;
          try {
            object = queryInfo.get(scannedID);
          } catch (ParseException e1) {
            e1.printStackTrace();
          }
          if (object != null) {
            ecardNote.put("EcardUpdatedAt", object.getUpdatedAt());
            infoToBePinned.add(object);
            Log.i("addCachedEcardIds", "Ecard " + scannedID + " added!");
          }

          List<OfflineDataCachedIds> olDatas = db.getData("ecardID", scannedID);
          if (olDatas.size() != 0) {
            // if the record exists in local db,
            // delete it
            OfflineDataCachedIds olData = olDatas.get(0);
            ecardNote.put("whenMet", new Date());
            ecardNote.put("event_met", olData.getEventMet());
            ecardNote.put("where_met", olData.getWhereMet());
            ecardNote.put("notes", olData.getNotes());
            String filepath = olData.getVoiceNote();
            if (filepath != "null") {
              // if there is such voice note file to be saved
              FileInputStream fileInputStream = null;
              File file = new File(filepath);
              if (file.exists()) {
                byte[] bFile = new byte[(int) file.length()];
                // convert file into array of bytes
                try {
                  fileInputStream = new FileInputStream(file);
                  fileInputStream.read(bFile);
                  fileInputStream.close();
                } catch (FileNotFoundException e1) {
                  // TODO Auto-generated catch block
                  e1.printStackTrace();
                } catch (IOException e1) {
                  // TODO Auto-generated catch block
                  e1.printStackTrace();
                }
                // FIX: Now assume reaching this step network is always
                // available
                final ParseFile voiceFile = new ParseFile("voicenote.mp4",
                  bFile);
                try {
                  voiceFile.save();
                  file.delete();
                  ecardNote.put("voiceNotes", voiceFile);
                } catch (ParseException e) {
                  // TODO Auto-generated catch block
                  e.printStackTrace();
                }
              }
            }
            noteToBePinned.add(ecardNote);
            db.deleteData(olData);
          }
        }
        try {
          ParseObject.pinAll(infoToBePinned);
        } catch (ParseException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
        try {
          ParseObject.saveAll(noteToBePinned);
          ParseObject.pinAll(noteToBePinned);
        } catch (ParseException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }

        scannedIDs.clear();
      }
    }

    @Override
    protected void onPostExecute(String result) {
      if (flagToast) {
        Toast.makeText(context, "Cleared Offline Collection",
          Toast.LENGTH_SHORT).show();
      }
    }

  }

  public static class AddCardNetworkAvailable extends
      AsyncTask<String, Void, String> {

    private static final int ECARD_ADDED = 3;
    private Activity mActivity;
    private ParseUser currentUser;
    private String scannedId;
    private int flag = 0;
    private String deletedNoteId;
    private String filepath;
    private Date newDate;
    private static final String AUDIO_RECORDER_FOLDER = "AudioRecorder";

    public AddCardNetworkAvailable(Activity mActivity, ParseUser currentUser,
      String scannedId, String deletedNoteId, Date newDate) {
      this.mActivity = mActivity;
      this.currentUser = currentUser;
      this.scannedId = scannedId;
      this.deletedNoteId = deletedNoteId;
      this.newDate = newDate;
      filepath = getFilename();
    }

    private String getFilename() {
      String filepath = Environment.getExternalStorageDirectory().getPath();
      File file = new File(filepath, AUDIO_RECORDER_FOLDER);
      if (!file.exists()) {
        file.mkdirs();
      }
      return (file.getAbsolutePath() + "/voicenote.mp4");
    }

    @Override
    protected String doInBackground(String... params) {
      // Once entered here, ecard has confirmed to exist and not collected.
      if (deletedNoteId != null) {
        // if the note existed but deleted, flip the flag and save note changes
        ParseQuery<ParseObject> queryNote = ParseQuery.getQuery("ECardNote");
        ParseObject ecardNote;
        try {
          ecardNote = queryNote.get(deletedNoteId);
          saveNote(ecardNote);
        } catch (ParseException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      } else {
        // if the note didn't exist, create note and save note changes
        createNote();
      }

      return null;
    }

    private void saveNote(final ParseObject ecardNote) {
      // if note existed but deleted, directly get the note and flip the flag
      if (ecardNote != null) {
        // pin corresponding ecard
        ParseQuery<ParseObject> queryInfo = ParseQuery.getQuery("ECardInfo");
        ParseObject ecardObject = null;
        try {
          ecardObject = queryInfo.get(scannedId);
        } catch (ParseException e1) {
          e1.printStackTrace();
        }
        if (ecardObject != null) {
          try {
            ecardObject.pin();
          } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        }
        ecardNote.put("EcardUpdatedAt", ecardObject.getUpdatedAt());
        ecardNote.put("isDeleted", false);
        saveAllToParse(ecardNote);
      }

    }

    private void createNote() {
      // pin corresponding ecard
      ParseQuery<ParseObject> queryInfo = ParseQuery.getQuery("ECardInfo");
      ParseObject ecardObject = null;
      try {
        ecardObject = queryInfo.get(scannedId);
      } catch (ParseException e1) {
        e1.printStackTrace();
      }
      if (ecardObject != null) {
        try {
          ecardObject.pin();
        } catch (ParseException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }

      final ParseObject ecardNote = new ParseObject("ECardNote");
      ecardNote.setACL(new ParseACL(currentUser));
      ecardNote.put("userId", currentUser.getObjectId());
      ecardNote.put("ecardId", scannedId);
      ecardNote.put("EcardUpdatedAt", ecardObject.getUpdatedAt());
      saveAllToParse(ecardNote);
    }

    private void saveAllToParse(final ParseObject ecardNote) {
      // convert file into array of bytes
      File file = new File(filepath);
      if (file.exists()) {
        FileInputStream fileInputStream = null;
        byte[] bFile = new byte[(int) file.length()];
        try {
          fileInputStream = new FileInputStream(file);
          fileInputStream.read(bFile);
          fileInputStream.close();
        } catch (FileNotFoundException e1) {
          // TODO Auto-generated catch block
          e1.printStackTrace();
        } catch (IOException e1) {
          // TODO Auto-generated catch block
          e1.printStackTrace();
        }
        if (ECardUtils.isNetworkAvailable(mActivity)) {
          final ParseFile voiceFile = new ParseFile("voicenote.mp4", bFile);
          try {
            voiceFile.save();
            ecardNote.put("voiceNotes", voiceFile);
          } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
          saveChangesToParse(ecardNote);
        } else {
          // if network not available, save voicenote with unique name then
          // record in local database
          ecardNote.put("tmpVoiceByteArray", bFile);
          // flush sharedpreferences to 1969 so next time app opens with
          // internet, convert the file
          Date currentDate = new Date(0);
          SharedPreferences prefs = mActivity.getSharedPreferences(
            AppGlobals.MY_PREFS_NAME, mActivity.MODE_PRIVATE);
          SharedPreferences.Editor prefEditor = prefs.edit();
          prefEditor.putLong("DateNoteSynced", currentDate.getTime());
          prefEditor.commit();
          saveChangesToParse(ecardNote);
        }
      } else {
        // there was no voice note, directly proceed
        saveChangesToParse(ecardNote);
      }
    }

    private void saveChangesToParse(ParseObject object) {
      EditText whereMet = (EditText) mActivity.findViewById(R.id.PlaceAdded2);
      EditText eventMet = (EditText) mActivity.findViewById(R.id.EventAdded2);
      EditText notes = (EditText) mActivity.findViewById(R.id.EditNotes);
      object.put("whenMet", newDate);
      object.put("where_met", whereMet.getText().toString());
      object.put("event_met", eventMet.getText().toString());
      object.put("notes", notes.getText().toString());
      try {
        object.save();
        object.pin();
        flag = ECARD_ADDED;
        Log.i("savetoparse", "flag flipped");
        deleteLocalVoiceNote();
      } catch (ParseException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }

    }

    private void deleteLocalVoiceNote() {
      File myFile = new File(filepath);
      if (myFile.exists())
        myFile.delete();
    }

    protected void onPostExecute(String result) {
      switch (flag) {
      case ECARD_ADDED:
        Toast.makeText(mActivity, "Card Added", Toast.LENGTH_SHORT).show();
        break;
      default:
        Toast.makeText(mActivity, "Error Adding Card...", Toast.LENGTH_SHORT)
          .show();
        break;
      }
    }

  }

}
