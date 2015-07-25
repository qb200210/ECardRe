package com.micklestudios.knowell.utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import android.util.Log;
import android.widget.Toast;

import com.micklestudios.knowell.ActivitySearch;
import com.micklestudios.knowell.infrastructure.UserInfo;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

// The class to store all the globals in the app
public class AppGlobals {

  // No need for a constructor.
  private AppGlobals() {
  }

  // The global list of users in the Conversation List
  public static ArrayList<UserInfo> potentialUsers;

  // List of all the users in the system.
  public static ArrayList<UserInfo> allUsers;

  public static ParseUser currentUser;

  // Global Strings
  public static final String MY_PREFS_NAME = "KnoWellSyncParams";
  public static final String PUSH_CHANNEL_NAME = "KnoWellPush";
  
  public static void ensureNonNullUponResume() {
    if(allUsers == null || potentialUsers == null){
      // If null, block the code and make sure data is re-populated
      initializeAllContactsBlocking();
      initializePotentialUsersBlocking();
    }
  }

  public static void initializePotentialUsers() {
    initializePotentialUsers(false);
  }

  public static void initializePotentialUsers(boolean forced) {
    currentUser = ParseUser.getCurrentUser();

    if (potentialUsers == null) {
      potentialUsers = new ArrayList<UserInfo>();
    }

    if (forced || potentialUsers.size() == 0) {
      potentialUsers.clear();

      Log.i("actbuf", "inside getconvcontacts");
      AppGlobals.potentialUsers.clear();
      /* A map of all the ECardNote objects to the noteID */
      final HashMap<String, Date> infoIdToConvDateMap = new HashMap<String, Date>();
      // During SyncConversations, all conversations should have been synced to
      // local
      ParseQuery<ParseObject> queryConvs = ParseQuery.getQuery("Conversations");
      queryConvs.fromLocalDatastore();
      queryConvs.whereEqualTo("partyB", currentUser.get("ecardId").toString());
      queryConvs.whereNotEqualTo("isDeleted", true);
      List<ParseObject> objectConvList = null;
      try {
        objectConvList = queryConvs.find();
      } catch (ParseException e) {
        e.printStackTrace();
      }
      if (objectConvList != null && objectConvList.size() != 0) {
        // If there are conversations, don't worry about notes yet, just create
        // userInfo using ecards
        for (Iterator<ParseObject> iter = objectConvList.iterator(); iter
          .hasNext();) {
          ParseObject objectConv = iter.next();
          // don't need to check if the conversation is deleted, because that
          // should be done by SyncConversations
          String infoObjectId = objectConv.get("partyA").toString();
          Log.i("actbuf", objectConv.getUpdatedAt().toString());

          infoIdToConvDateMap.put(infoObjectId, objectConv.getUpdatedAt());
        }
        /*
         * Now, query the ECardInfoTable to get all the ECardInfo for the
         * conversations collected here.
         */
        ParseQuery<ParseObject> queryInfo = ParseQuery.getQuery("ECardInfo");
        queryInfo.fromLocalDatastore();
        queryInfo.whereContainedIn("objectId", infoIdToConvDateMap.keySet());
        List<ParseObject> objectInfoList = null;
        try {
          objectInfoList = queryInfo.find();
        } catch (ParseException e) {
          e.printStackTrace();
        }
        Log.i("actbuf",
          " " + objectConvList.size() + " " + objectInfoList.size());

        if (objectInfoList != null && objectInfoList.size() != 0) {
          for (Iterator<ParseObject> iter = objectInfoList.iterator(); iter
            .hasNext();) {
            ParseObject objectInfo = iter.next();
            UserInfo contact = new UserInfo(objectInfo);
            if (contact != null) {
              Log.i("actbuf", contact.getFirstName());
              // No need to put note as part of UserInfo -- will execute
              // note_query from localdatastore later
              // Dont need to keep mapping to actual conversations objects --
              // they
              // are not as critical
              Log.i("actbuf", infoIdToConvDateMap.get(objectInfo.getObjectId())
                .toString());
              contact.setWhenMet(infoIdToConvDateMap.get(objectInfo
                .getObjectId()));
              // If there are 20 conversations, while only 4 of corresponding
              // ecard pinned down, then final conv for display will be 4
              AppGlobals.potentialUsers.add(contact);
            }
          }
        }
      }
    }
  }

  public static void initializeAllContacts() {
    initializeAllContacts(false);
  }

  public static void initializeAllContacts(boolean forced) {
    if (allUsers == null) {
      allUsers = new ArrayList<UserInfo>();
    }

    if (forced || allUsers.size() == 0) {
      allUsers.clear();

      /* A map of all the ECardNote objects to the noteID */
      final HashMap<String, ParseObject> noteIdToNoteObjectMap = new HashMap<String, ParseObject>();

      ParseQuery<ParseObject> queryNotes = ParseQuery.getQuery("ECardNote");
      queryNotes.fromLocalDatastore();
      queryNotes.whereEqualTo("userId", AppGlobals.currentUser.getObjectId());
      queryNotes.whereNotEqualTo("isDeleted", true);
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
                        // Contact has been created. Populate the "createdAt"
                        // from
                        // the note object.
                        String infoObjectId = (String) objectInfo.getObjectId();
                        ParseObject objectNote = noteIdToNoteObjectMap
                          .get(infoObjectId);
                        contact.setWhenMet((Date) objectNote.get("whenMet"));
                        contact.setEventMet(objectNote.getString("event_met"));
                        contact.setWhereMet(objectNote.getString("where_met"));
                        contact.setNotes(objectNote.getString("notes"));

                        allUsers.add(contact);

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
            Log.e("Knowell", "General parse error!");
          }
        }
      });
    }
  }
  
  public static void initializeAllContactsBlocking() {
    if (allUsers == null) {
      allUsers = new ArrayList<UserInfo>();
    }

    
      allUsers.clear();
      
      currentUser = ParseUser.getCurrentUser();

      /* A map of all the ECardNote objects to the noteID */
      final HashMap<String, ParseObject> noteIdToNoteObjectMap = new HashMap<String, ParseObject>();

      ParseQuery<ParseObject> queryNotes = ParseQuery.getQuery("ECardNote");
      queryNotes.fromLocalDatastore();
      queryNotes.whereEqualTo("userId", AppGlobals.currentUser.getObjectId());
      queryNotes.whereNotEqualTo("isDeleted", true);
      List<ParseObject> objectsNoteList = null;
      try {
        objectsNoteList = queryNotes.find();
      } catch (ParseException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      
        if (objectsNoteList != null) {
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

          List<ParseObject> objectInfoList = null;
          try {
            objectInfoList = queryInfo.find();
          } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
          if(objectInfoList!=null){
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
                // Contact has been created. Populate the "createdAt"
                // from
                // the note object.
                String infoObjectId = (String) objectInfo.getObjectId();
                ParseObject objectNote = noteIdToNoteObjectMap
                  .get(infoObjectId);
                contact.setWhenMet((Date) objectNote.get("whenMet"));
                contact.setEventMet(objectNote.getString("event_met"));
                contact.setWhereMet(objectNote.getString("where_met"));
                contact.setNotes(objectNote.getString("notes"));
  
                allUsers.add(contact);
  
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
      
  }
  
  public static void initializePotentialUsersBlocking() {
    

    if (potentialUsers == null) {
      potentialUsers = new ArrayList<UserInfo>();
    }
    potentialUsers.clear();
    currentUser = ParseUser.getCurrentUser();

      /* A map of all the ECardNote objects to the noteID */
      final HashMap<String, Date> infoIdToConvDateMap = new HashMap<String, Date>();
      // During SyncConversations, all conversations should have been synced to
      // local
      ParseQuery<ParseObject> queryConvs = ParseQuery.getQuery("Conversations");
      queryConvs.fromLocalDatastore();
      queryConvs.whereEqualTo("partyB", currentUser.get("ecardId").toString());
      queryConvs.whereNotEqualTo("isDeleted", true);
      List<ParseObject> objectConvList = null;
      try {
        objectConvList = queryConvs.find();
      } catch (ParseException e) {
        e.printStackTrace();
      }
      if (objectConvList != null && objectConvList.size() != 0) {
        // If there are conversations, don't worry about notes yet, just create
        // userInfo using ecards
        for (Iterator<ParseObject> iter = objectConvList.iterator(); iter
          .hasNext();) {
          ParseObject objectConv = iter.next();
          // don't need to check if the conversation is deleted, because that
          // should be done by SyncConversations
          String infoObjectId = objectConv.get("partyA").toString();
          Log.i("actbuf", objectConv.getUpdatedAt().toString());

          infoIdToConvDateMap.put(infoObjectId, objectConv.getUpdatedAt());
        }
        /*
         * Now, query the ECardInfoTable to get all the ECardInfo for the
         * conversations collected here.
         */
        ParseQuery<ParseObject> queryInfo = ParseQuery.getQuery("ECardInfo");
        queryInfo.fromLocalDatastore();
        queryInfo.whereContainedIn("objectId", infoIdToConvDateMap.keySet());
        List<ParseObject> objectInfoList = null;
        try {
          objectInfoList = queryInfo.find();
        } catch (ParseException e) {
          e.printStackTrace();
        }

        if (objectInfoList != null && objectInfoList.size() != 0) {
          for (Iterator<ParseObject> iter = objectInfoList.iterator(); iter
            .hasNext();) {
            ParseObject objectInfo = iter.next();
            UserInfo contact = new UserInfo(objectInfo);
            if (contact != null) {
              contact.setWhenMet(infoIdToConvDateMap.get(objectInfo
                .getObjectId()));
              // If there are 20 conversations, while only 4 of corresponding
              // ecard pinned down, then final conv for display will be 4
              AppGlobals.potentialUsers.add(contact);
            }
          }
        }
      }
    
  }

  public static void refreshGlobalParseBasedData() {
    initializeAllContacts();
    initializePotentialUsers();
  }
}
