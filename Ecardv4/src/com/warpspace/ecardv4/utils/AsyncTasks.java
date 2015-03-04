package com.warpspace.ecardv4.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.ParseACL;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.warpspace.ecardv4.ActivityBufferOpening;

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

		public SyncDataTaskSelfCopy(Context context, ParseUser currentUser){
			this.context = context;
			this.currentUser = currentUser;
		}
		@Override
		protected String doInBackground(String... url) {

			ParseQuery<ParseObject> query = ParseQuery.getQuery("ECardInfo");
			ParseObject infoObject = null;
			try {
				infoObject = query.get(currentUser.get("ecardId").toString());
			} catch (ParseException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			if (infoObject != null) {
				final ParseObject infoObjectTmp = infoObject;
				byte[] tmpImgData = (byte[]) infoObject.get("tmpImgByteArray");
				if (tmpImgData != null) {
					// if there is cached data in the array, convert to ParseFile then clear the array
					final ParseFile file = new ParseFile("portrait.jpg", tmpImgData);
					// cannot save in thread, otherwise file could be empty when Design saved

					file.saveInBackground(new SaveCallback() {

						@Override
						public void done(ParseException e) {
							if (e == null) {
								Log.i("self copy", "Cached portrait saved!");
								infoObjectTmp.put("portrait", file);
								infoObjectTmp.remove("tmpImgByteArray");
								// do not use saveEventually, easily leads to corrupted data
								infoObjectTmp.saveInBackground();
							} else {
								e.printStackTrace();
							}
						}

					});

				}
				try {
					infoObject.pin();
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			Toast.makeText(context, "saved self copy", Toast.LENGTH_SHORT).show();
		}

	}
	
	// sync local copy of notes and corresponding ecards
	public static class SyncDataTaskNotes extends AsyncTask<String, Void, String> {

		private Context context;
		private ParseUser currentUser;

		public SyncDataTaskNotes(Context context, ParseUser currentUser){
			this.context = context;
			this.currentUser = currentUser;
		}
		
		@Override
		protected String doInBackground(String... params) {
			ParseQuery<ParseObject> query = ParseQuery.getQuery("ECardNote");
			query.whereEqualTo("userId", currentUser.getObjectId());
			List<ParseObject> noteObjectsTmp = null;
			try {
				noteObjectsTmp = query.find();
			} catch (ParseException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			final List<ParseObject> noteObjects = noteObjectsTmp;
			ParseQuery<ParseObject> queryLocal = ParseQuery.getQuery("ECardNote");
			queryLocal.fromLocalDatastore();
			queryLocal.whereEqualTo("userId", currentUser.getObjectId());
			queryLocal.findInBackground(new FindCallback<ParseObject>() {

				private TreeSet<String> serverNoteIds = new TreeSet<String>();
				private TreeSet<String> ecardIdsTree = new TreeSet<String>();
				private List<ParseObject> toBeUnpinned = new ArrayList<ParseObject>();

				@Override
				public void done(List<ParseObject> localObjects, ParseException e) {
					if (e == null) {
						if (localObjects.size() != 0) {
							// unpin all local note records that do not exist on server
							if (noteObjects.size() != 0) {
								for (Iterator<ParseObject> iter = noteObjects.iterator(); iter.hasNext();) {
									ParseObject obj = iter.next();
									serverNoteIds.add(obj.getObjectId());
								}
								for (Iterator<ParseObject> iter = localObjects.iterator(); iter.hasNext();) {
									ParseObject localObj = iter.next();
									if (!serverNoteIds.contains(localObj.getObjectId())) {
										// if the local record doesn't exist on server, record for unpin
										toBeUnpinned.add(localObj);
									}
								}
							} else {
								toBeUnpinned = localObjects;
							}
							if (toBeUnpinned.size() != 0) {
								try {
									ParseObject.unpinAll(toBeUnpinned);
								} catch (ParseException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}
							}
						}
						if (noteObjects.size() != 0) {
							// pin down all note records to local
							// placeholder: should compare time to decide whether to save to server or pin to local
							try {
								ParseObject.pinAll(noteObjects);
							} catch (ParseException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
							for (Iterator<ParseObject> iter = noteObjects.iterator(); iter.hasNext();) {
								ParseObject objNote = iter.next();
								ecardIdsTree.add(objNote.get("ecardId").toString());
							}
							ParseQuery<ParseObject> query1 = ParseQuery.getQuery("ECardInfo");
							query1.whereContainedIn("objectId", ecardIdsTree);
							List<ParseObject> infoObjects = null;
							try {
								infoObjects = query1.find();
							} catch (ParseException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
							if (infoObjects != null) {
								try {
									ParseObject.pinAll(infoObjects);
								} catch (ParseException e1) {
									e1.printStackTrace();
								}								
							}
						}
					} else {
						e.printStackTrace();
					}
				}

			});
			
			return null;
		}
		
		@Override
		protected void onPostExecute(String result) {
			Toast.makeText(context, "synced notes", Toast.LENGTH_SHORT).show();
		}
		
	}
	
	public static class SyncDataTaskConversations extends AsyncTask<String, Void, String> {
		
		private Context context;
		private ParseUser currentUser;
		
		public SyncDataTaskConversations(Context context, ParseUser currentUser){
			this.context = context;
			this.currentUser = currentUser;
		}

		@Override
		protected String doInBackground(String... params) {
				// find all conversations from the parse
				ParseQuery<ParseObject> query = ParseQuery.getQuery("Conversations");
				query.whereEqualTo("partyB", currentUser.get("ecardId").toString());
				List<ParseObject> objectsTmp = null;
				try {
					objectsTmp = query.find();
				} catch (ParseException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				final List<ParseObject> objects = objectsTmp;

				// remove all objects locally then pin from parse
				// this is special because for conversations, server always wins
				ParseQuery<ParseObject> queryLocal = ParseQuery.getQuery("Conversations");
				queryLocal.fromLocalDatastore();
				queryLocal.whereEqualTo("partyB", currentUser.get("ecardId").toString());
				queryLocal.findInBackground(new FindCallback<ParseObject>() {

					private TreeSet<String> serverConversationIds = new TreeSet<String>();
					private TreeSet<String> ecardIdsTree = new TreeSet<String>();
					private List<ParseObject> toBeUnpinned = new ArrayList<ParseObject>();

					@Override
					public void done(List<ParseObject> localObjects, ParseException e) {
						if (e == null) {
							if (localObjects.size() != 0) {
								// unpin all local conversation records that do not exist on server
								if (objects.size() != 0) {
									for (Iterator<ParseObject> iter = objects.iterator(); iter.hasNext();) {
										ParseObject obj = iter.next();
										serverConversationIds.add(obj.getObjectId());
									}
									for (Iterator<ParseObject> iter = localObjects.iterator(); iter.hasNext();) {
										ParseObject localObj = iter.next();
										if (!serverConversationIds.contains(localObj.getObjectId())) {
											// if the local record doesn't exist on server, record for unpin
											toBeUnpinned.add(localObj);
										}
									}
								} else {
									toBeUnpinned = localObjects;
								}
								if (toBeUnpinned.size() != 0) {
									try {
										ParseObject.unpinAll(toBeUnpinned);
									} catch (ParseException e1) {
										// TODO Auto-generated catch block
										e1.printStackTrace();
									}
								}
							}
							if (objects.size() != 0) {
								// pin down all conversation records to local
								try {
									ParseObject.pinAll(objects);
								} catch (ParseException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}
								// directly find and save all ecards associated with incoming requests
								for (Iterator<ParseObject> iter = objects.iterator(); iter.hasNext();) {
									ParseObject objConversation = iter.next();
									ecardIdsTree.add(objConversation.get("partyA").toString());
								}
								ParseQuery<ParseObject> query1 = ParseQuery.getQuery("ECardInfo");
								query1.whereContainedIn("objectId", ecardIdsTree);
								List<ParseObject> infoObjects = null;
								try {
									infoObjects = query1.find();
								} catch (ParseException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}
								if (infoObjects != null) {
									try {
										ParseObject.pinAll(infoObjects);
									} catch (ParseException e1) {
										// TODO Auto-generated catch block
										e1.printStackTrace();
									}
									// Toast.makeText(context,"Incoming cards cached to local", Toast.LENGTH_SHORT).show();
								}								
							}
						} else {
							e.printStackTrace();
						}
					}
				});
			
			return null;
		}
		
		@Override
		protected void onPostExecute(String result) {
			Toast.makeText(context, "synced conversations", Toast.LENGTH_SHORT).show();
		}
		
	}
	
	public static class SyncDataTaskCachedIds extends AsyncTask<String, Void, String> {

		private Context context;
		private ParseUser currentUser;
		ECardSQLHelper db;
		List<String> scannedIDs;
		List<OfflineData> olDatas;

		public SyncDataTaskCachedIds(Context context, ParseUser currentUser){
			this.context = context;
			this.currentUser = currentUser;
		}
		
		@Override
		protected String doInBackground(String... params) {
			// Upon opening, if there is Internet connection, try to store cached IDs
			db = new ECardSQLHelper(context);
			// getting all local db data to check against EcardIds
			olDatas = db.getAllData();
			if (olDatas.size() != 0) {
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
			Toast.makeText(context, "synced cachedIds", Toast.LENGTH_SHORT).show();
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
