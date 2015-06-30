package com.micklestudios.knowell.utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import android.util.Log;

import com.micklestudios.knowell.infrastructure.UserInfo;
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

  public static ParseUser currentUser;

  // Global Strings
  public static final String MY_PREFS_NAME = "KnoWellSyncParams";

  public static void initializePotentialUsers() {
    initializePotentialUsers(false);
  }

  public static void initializePotentialUsers(boolean forced) {
    currentUser = ParseUser.getCurrentUser();

    if (potentialUsers == null) {
      potentialUsers = new ArrayList<UserInfo>();
    }

    if (forced || potentialUsers.size() == 0) {
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

  public static void refreshGlobalParseBasedData() {

  }
}
