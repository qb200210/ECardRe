package com.warpspace.ecardv4.utils;

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
	TreeSet<String> ecardIdsTree = new TreeSet<String>();

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
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Conversations");
        query.whereEqualTo("partyB", currentUser.get("ecardId").toString());
        query.findInBackground(new FindCallback<ParseObject>(){

			@Override
			public void done(List<ParseObject> objects, ParseException e) {
				if(e==null){
					if(objects != null){
						// save all found conversations to local upon new notification	
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
							public void done(List<ParseObject> objects, ParseException e) {
								if(e==null){
									if(objects != null){
										ParseObject.pinAllInBackground(objects);	
										// Toast.makeText(context,"Incoming cards cached to local", Toast.LENGTH_SHORT).show();									
									}
								} else{
									e.printStackTrace();
								}
							}
				        	
				        });
					}
				} else{
					e.printStackTrace();
				}
			}
        	
        });        
		
	}

}
