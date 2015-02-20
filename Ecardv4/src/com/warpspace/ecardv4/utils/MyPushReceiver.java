package com.warpspace.ecardv4.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import org.json.JSONException;
import org.json.JSONObject;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class MyPushReceiver extends BroadcastReceiver {
	
	ParseUser currentUser;	

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
        
		syncAllLocalConversations();
	}
	
	private void syncAllLocalConversations() {
		  // find all conversations from the parse
		  ParseQuery<ParseObject> query = ParseQuery.getQuery("Conversations");
	      query.whereEqualTo("partyB", currentUser.get("ecardId").toString());
	      query.findInBackground(new FindCallback<ParseObject>(){

				@Override
				public void done(final List<ParseObject> objects, ParseException e) {
					if(e == null){					
						// remove all objects locally then pin from parse
						// this is special because for conversations, server always wins
						ParseQuery<ParseObject> queryLocal = ParseQuery.getQuery("Conversations");
						queryLocal.fromLocalDatastore();
						queryLocal.whereEqualTo("partyB", currentUser.get("ecardId").toString());
						queryLocal.findInBackground(new FindCallback<ParseObject>(){

							private TreeSet<String> serverConversationIds = new TreeSet<String>();
							private TreeSet<String> ecardIdsTree = new TreeSet<String>();
							private List<ParseObject> toBeUnpinned = new ArrayList<ParseObject>();

							@Override
							public void done(List<ParseObject> localObjects, ParseException e) {
								if(e==null){
									if(localObjects.size() !=0){
										// unpin all local conversation records that do not exist on server
										if(objects.size() != 0){
											for(Iterator<ParseObject> iter = objects.iterator(); iter.hasNext();){
												ParseObject obj = iter.next();
												serverConversationIds.add(obj.getObjectId());
											}
											for(Iterator<ParseObject> iter = localObjects.iterator(); iter.hasNext();){
												ParseObject localObj = iter.next();
												if(!serverConversationIds.contains(localObj.getObjectId())){
													// if the local record doesn't exist on server, record for unpin
													toBeUnpinned.add(localObj);
												}
											}
										} else {
											toBeUnpinned = localObjects;
										}
										if(toBeUnpinned.size() !=0){
											ParseObject.unpinAllInBackground(toBeUnpinned);	
										}
									}
									if(objects.size()!=0){
										// pin down all conversation records to local
										ParseObject.pinAllInBackground(objects);
										// directly find and save all ecards associated with incoming requests
										for(Iterator<ParseObject> iter= objects.iterator(); iter.hasNext();){
											ParseObject objConversation = iter.next();
											ecardIdsTree.add(objConversation.get("partyA").toString());
										}
										ParseQuery<ParseObject> query1 = ParseQuery.getQuery("ECardInfo");
								        query1.whereContainedIn("objectId", ecardIdsTree);
								        query1.findInBackground(new FindCallback<ParseObject>(){

											@Override
											public void done(List<ParseObject> infoObjects, ParseException e) {
												if(e==null){
													if(infoObjects != null){
														ParseObject.pinAllInBackground(infoObjects);	
														// Toast.makeText(context,"Incoming cards cached to local", Toast.LENGTH_SHORT).show();									
													}
												} else{
													e.printStackTrace();
												}
											}
								        	
								        });
									}
								}else {
									e.printStackTrace();
								}
							}				    
					    });					
					} else {
						e.printStackTrace();
					}
					
				}
	      });
		}
}
