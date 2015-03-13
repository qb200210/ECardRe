package com.warpspace.ecardv4.utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;
import android.app.Activity;

import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.ParseACL;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.warpspace.ecardv4.ActivityBufferOpening;
import com.warpspace.ecardv4.ActivityMain;

public class AsyncTasks {
	
	public static class SaveNoteNetworkAvailable extends AsyncTask<String, Void, String> {

		private Context context;
		private ParseUser currentUser;
		private String ecardNoteId;

		public SaveNoteNetworkAvailable(Context context, ParseUser currentUser, String ecardNoteId){
			this.context = context;
			this.currentUser = currentUser;
			this.ecardNoteId = ecardNoteId;
		}
		@Override
		protected String doInBackground(String... url) {

			ParseQuery<ParseObject> query = ParseQuery.getQuery("ECardNote");
			ParseObject noteObject = null;
			
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			Toast.makeText(context, "saved self copy", Toast.LENGTH_SHORT).show();
		}

	}
	
	// sync local copy of self ecard
	// now server always wins, should change to check date
	public static class SyncDataTaskSelfCopy extends AsyncTask<String, Void, String> {

		private Context context;
		private ParseUser currentUser;
		private SharedPreferences prefs;
		private SharedPreferences.Editor prefEditor;
		private boolean flagShouldSync;
		private boolean imgFromTmpData;

		public SyncDataTaskSelfCopy(Context context, ParseUser currentUser, SharedPreferences prefs, SharedPreferences.Editor prefEditor, boolean imgFromTmpData){
			this.context = context;
			this.currentUser = currentUser;
			this.prefs = prefs;
			this.prefEditor = prefEditor;
			this.flagShouldSync = false;
			this.imgFromTmpData = imgFromTmpData;
		}
		@Override
		protected String doInBackground(String... url) {
			// get the stored shared last sync date, if null, default to 1969
			long millis = prefs.getLong("DateSelfSynced", 0L);
			Date lastSyncedDate = new Date(millis);

			ParseQuery<ParseObject> query = ParseQuery.getQuery("ECardInfo");
			// constraint: server value newer
			// if tmpImgByteArray was used, lastSyncedDate will be set to 1969, so that it will make sure the 
			// parse object is pulled and the parsefile created from tmpImgByteArray
			query.whereGreaterThan("updatedAt", lastSyncedDate);
			query.whereEqualTo("objectId", currentUser.get("ecardId").toString());
			List<ParseObject> infoObjects = null;
			try {
				infoObjects = query.find();
			} catch (ParseException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			if(infoObjects !=null && infoObjects.size()!=0){
				flagShouldSync = true;
				// if there is a newer version on server, then sync it to local
				final ParseObject infoObjectTmp = infoObjects.get(0);
				// for webui, should do the same, check this array whenever can, then convert it to parseFile
				byte[] tmpImgData = (byte[]) infoObjects.get(0).get("tmpImgByteArray");
				if (tmpImgData != null) {
					// if there is cached data in the array on server, convert to ParseFile then clear the array
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
					Date currentDate=new Date();
					prefEditor.putLong("DateSelfSynced", currentDate.getTime());
					prefEditor.commit();
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			if(flagShouldSync){
				Toast.makeText(context, "saved self copy", Toast.LENGTH_SHORT).show();
			}
			// if there is network, wait till self sync completes before finishing BufferOpening
			Intent intent = new Intent(context, ActivityMain.class);
			intent.putExtra("imgFromTmpData", imgFromTmpData);
			context.startActivity(intent);
			((Activity)context).finish();
		}

	}
	
	// sync local copy of notes and corresponding ecards
	public static class SyncDataTaskNotes extends AsyncTask<String, Void, String> {

		private Context context;
		private ParseUser currentUser;
		private SharedPreferences prefs;
		private SharedPreferences.Editor prefEditor;
		private boolean flagShouldSync;

		public SyncDataTaskNotes(Context context, ParseUser currentUser, SharedPreferences prefs, SharedPreferences.Editor prefEditor){
			this.context = context;
			this.currentUser = currentUser;
			this.prefs = prefs;
			this.prefEditor = prefEditor;
			this.flagShouldSync = false;
		}
		
		@Override
		protected String doInBackground(String... params) {
			// get the stored shared last sync date, if null, default to 1969
			long millis = prefs.getLong("DateNoteSynced", 0L);
			Date lastSyncedDate = new Date(millis);
			
			ParseQuery<ParseObject> query = ParseQuery.getQuery("ECardNote");
			query.whereEqualTo("userId", currentUser.getObjectId());
			query.whereGreaterThan("updatedAt", lastSyncedDate);
			List<ParseObject> noteObjects = null;
			try {
				noteObjects = query.find();
			} catch (ParseException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			if(noteObjects != null && noteObjects.size()!=0){
				flagShouldSync = true;
				// unpin those notes that are deleted
				for(Iterator<ParseObject> iter = noteObjects.iterator(); iter.hasNext();){
					ParseObject objNote = iter.next();
					// This is to cache all associated parseFiles
					ParseFile voiceNote = (ParseFile) objNote.get("voiceNotes");
					if(voiceNote !=null){
						try {
							// dummy statement to force caching data
							voiceNote.getData();
						} catch (ParseException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}					
					if(objNote.get("isDeleted") != null){
						if( (boolean) objNote.get("isDeleted") == true){
							try {
								// unpin the "deleted" object
								objNote.unpin();
								// remove it from the to-be-pinned list
								iter.remove();
							} catch (ParseException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
				}
				TreeSet<String> ecardIdsTree = new TreeSet<String>();
				for(Iterator<ParseObject> iter = noteObjects.iterator(); iter.hasNext();){
					ParseObject objNote = iter.next();
					ecardIdsTree.add(objNote.get("ecardId").toString());
					byte[] tmpVoiceData = (byte[]) objNote.get("tmpVoiceByteArray");
					if (tmpVoiceData != null) {
						// if there is cached data in the array on server, convert to ParseFile then clear the array
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
				}
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
				if (infoObjects != null) {
					for(Iterator<ParseObject> iter = infoObjects.iterator(); iter.hasNext();){
						ParseObject objInfo = iter.next();
						// This is to cache all associated parseFiles
						ParseFile portraitImg = (ParseFile) objInfo.get("portrait");
						if(portraitImg !=null){
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
				// pin noteObjects
				try {
					ParseObject.pinAll(noteObjects);
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}			
			}
			// flush sharedpreference with today's date after all notes saved
			Date currentDate=new Date();
			prefEditor.putLong("DateNoteSynced", currentDate.getTime());
			prefEditor.commit();
			return null;
		}
		
		@Override
		protected void onPostExecute(String result) {
			if(flagShouldSync){
				Toast.makeText(context, "synced notes", Toast.LENGTH_SHORT).show();
			}
		}
		
	}
	
	public static class SyncDataTaskConversations extends AsyncTask<String, Void, String> {
		
		private Context context;
		private ParseUser currentUser;
		private SharedPreferences prefs;
		private SharedPreferences.Editor prefEditor;
		private boolean flagShouldSync;
		
		public SyncDataTaskConversations(Context context, ParseUser currentUser, SharedPreferences prefs, SharedPreferences.Editor prefEditor){
			this.context = context;
			this.currentUser = currentUser;
			this.prefs = prefs;
			this.prefEditor = prefEditor;
			this.flagShouldSync = false;
		}

		@Override
		protected String doInBackground(String... params) {
			// get the stored shared last sync date, if null, default to 1969
			long millis = prefs.getLong("DateConversationsSynced", 0L);
			Date lastSyncedDate = new Date(millis);	
			
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
			if(convObjects != null && convObjects.size()!=0){
				flagShouldSync = true;
				for(Iterator<ParseObject> iter = convObjects.iterator(); iter.hasNext();){
					ParseObject objConv = iter.next();			
					if(objConv.get("isDeleted") != null){
						if( (boolean) objConv.get("isDeleted") == true){
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
				TreeSet<String> ecardIdsTree = new TreeSet<String>();
				for(Iterator<ParseObject> iter = convObjects.iterator(); iter.hasNext();){
					ParseObject objConv = iter.next();
					ecardIdsTree.add(objConv.get("partyA").toString());						
				}
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
				if (infoObjects != null) {
					for(Iterator<ParseObject> iter = infoObjects.iterator(); iter.hasNext();){
						ParseObject objInfo = iter.next();
						// This is to cache all associated parseFiles
						ParseFile portraitImg = (ParseFile) objInfo.get("portrait");
						if(portraitImg !=null){
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
				// pin convObjects
				try {
					ParseObject.pinAll(convObjects);
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}		
			}
			// flush sharedpreference with today's date after all notes saved
			Date currentDate=new Date();
			prefEditor.putLong("DateConversationsSynced", currentDate.getTime());
			prefEditor.commit();
			return null;
		}
		
		@Override
		protected void onPostExecute(String result) {
			if(flagShouldSync){
				Toast.makeText(context, "synced conversations", Toast.LENGTH_SHORT).show();
			}
		}
		
	}
	
	public static class SyncDataTaskCachedIds extends AsyncTask<String, Void, String> {

		private Context context;
		private ParseUser currentUser;
		ECardSQLHelper db;
		List<String> scannedIDs;
		List<OfflineData> olDatas;
		private boolean flagShouldSync;
		

		public SyncDataTaskCachedIds(Context context, ParseUser currentUser){
			this.context = context;
			this.currentUser = currentUser;
			this.flagShouldSync = false;
		}
		
		@Override
		protected String doInBackground(String... params) {
			// Upon opening, if there is Internet connection, try to store cached IDs
			db = new ECardSQLHelper(context);
			// getting all local db data to check against EcardIds
			olDatas = db.getAllData();
			if (olDatas.size() != 0) {
				flagShouldSync = true;
				Log.i("CachedIds", "Found unsaved Ecards");
				// If there are unsaved offline list, check and save them
				scannedIDs = new LinkedList<String>();
				for (Iterator<OfflineData> iter = olDatas.iterator(); iter.hasNext();) {
					OfflineData olData = iter.next();
					String scannedID = olData.getEcardID();
					scannedIDs.add(scannedID);
				}
				addCachedEcardIds();
			}
			return null;
		}
		
		public void addCachedEcardIds() {
			ParseQuery<ParseObject> query = ParseQuery.getQuery("ECardInfo");
			query.whereContainedIn("objectId", scannedIDs);
			List<ParseObject> infoObjectsTmp = null;
			try {
				infoObjectsTmp = query.find();
			} catch (ParseException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
			List<ParseObject> infoObjects = infoObjectsTmp;
			
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
						List<OfflineData> olDatas = db.getData("ecardID", scannedID);
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
				List<ParseObject> noteObjectsTmp = null;
				try {
					noteObjectsTmp = queryNote.find();
				} catch (ParseException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}
				List<ParseObject> noteObjects = noteObjectsTmp;			
				ArrayList<String> toRemove = new ArrayList<String>();
				if (noteObjects != null) {
					// these are the ecards that are already
					// collected
					for (Iterator<ParseObject> iter = noteObjects.iterator(); iter.hasNext();) {
						ParseObject object = iter.next();
						Log.i("addCachedEcardIds", "ECard " + object.get("ecardId").toString() + " already existed!");
						toRemove.add(object.get("ecardId").toString());
						// Either way, delete the local db record. 
						// This is because local db is only a temp storage for offline added Ecards
						// Should be emptied when cards are collected
						List<OfflineData> olDatas = db.getData("ecardID", object.get("ecardId").toString());
						if (olDatas.size() != 0) {
							// if the record exists in local db, delete it
							OfflineData olData = olDatas.get(0);
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
					noteToBePinned.add(ecardNote);
					List<OfflineData> olDatas = db.getData("ecardID", scannedID);
					if (olDatas.size() != 0) {
						// if the record exists in local db,
						// delete it
						OfflineData olData = olDatas.get(0);
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
			if(flagShouldSync){
				Toast.makeText(context, "synced cachedIds", Toast.LENGTH_SHORT).show();
			}
		}
		
	}
	
	public static class AddCardNetworkAvailable extends AsyncTask<String, Void, String> {

		private static final int INVALID_ECARD = 1;
		private static final int ECARD_EXISTS = 2;
		private static final int ECARD_ADDED = 3;
		private Context context;
		private ParseUser currentUser;
		private String scannedId;
		private int flag = 0;

		public AddCardNetworkAvailable(Context context, ParseUser currentUser, String scannedId){
			this.context = context;
			this.currentUser = currentUser;
			this.scannedId = scannedId;
		}
		
		@Override
		protected String doInBackground(String... params) {

				ParseQuery<ParseObject> query = ParseQuery.getQuery("ECardInfo");
				ParseObject objectScanned = null;
				try {
					objectScanned = query.get(scannedId);
				} catch (ParseException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}
				if (objectScanned == null) {
					// If ecard non-exist on server
					Log.i("AddCard", "No such Ecard with ID: " + scannedId);
					flag = INVALID_ECARD;
				} else {
					// ecard found on server
					// now check if it's collected already on server
					ParseQuery<ParseObject> queryNote = ParseQuery.getQuery("ECardNote");
					queryNote.whereEqualTo("userId", currentUser.getObjectId());
					queryNote.whereEqualTo("ecardId", objectScanned.getObjectId());
					List<ParseObject> objects = null;
					try {
						objects = queryNote.find();
					} catch (ParseException e2) {
						// TODO Auto-generated catch block
						e2.printStackTrace();
					}
					if (objects == null) {
						// If Ecard not collected yet, add it to EcardNote
						final ParseObject ecardNote = new ParseObject("ECardNote");
						ecardNote.setACL(new ParseACL(currentUser));
						ecardNote.put("userId", currentUser.getObjectId());
						ecardNote.put("ecardId", scannedId);
						// if(cityName != null){
						// ecardNote.put("where", cityName);
						// }
						// fetch the EcardInfo to be added to extract some info that needs to be placed into EcardNote
						ParseQuery<ParseObject> queryInfo = ParseQuery.getQuery("ECardInfo");
						ParseObject object = null;
						try {
							object = queryInfo.get(scannedId);
						} catch (ParseException e1) {
							e1.printStackTrace();
						}
						if (object != null) {
							ecardNote.put("EcardUpdatedAt", object.getUpdatedAt());
							try {
								object.pin();
							} catch (ParseException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							Log.i("AddCard", "Ecard " + scannedId + " added!");
							flag = ECARD_ADDED;
						}
						// save the note, upon success, pin it to local
						ecardNote.saveInBackground(new SaveCallback(){

							@Override
							public void done(ParseException arg0) {
								try {
									ecardNote.pin();
								} catch (ParseException e) {
									e.printStackTrace();
								}
							}
							
						});
					} else {
						Log.i("AddCard", "Ecard " + scannedId + " exists!");
						flag = ECARD_EXISTS;
					}
				}
			
			return null;
		}
		
		protected void onPostExecute(String result) {
			switch(flag){
			  case ECARD_ADDED:
				  Toast.makeText(context, "Card Added", Toast.LENGTH_SHORT).show();
				  break;
			  case ECARD_EXISTS:
				  Toast.makeText(context, "Card exists", Toast.LENGTH_SHORT).show();
				  break;
			  case INVALID_ECARD:
				  Toast.makeText(context, "Invalid Card", Toast.LENGTH_SHORT).show();
				  break;
			  default:
				  Toast.makeText(context, "Error Adding Card...", Toast.LENGTH_SHORT).show();
				  break;
			}			
		}
		
	}
	
}
