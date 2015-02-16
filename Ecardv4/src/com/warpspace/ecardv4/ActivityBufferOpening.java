package com.warpspace.ecardv4;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseACL;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.warpspace.ecardv4.R;
import com.warpspace.ecardv4.utils.ECardSQLHelper;
import com.warpspace.ecardv4.utils.ECardUtils;
import com.warpspace.ecardv4.utils.OfflineData;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.widget.Toast;

public class ActivityBufferOpening extends Activity {
	
	ECardSQLHelper db;
	List<String> scannedIDs;
	List<OfflineData> olDatas;
	ParseUser currentUser;
	// flag to see if there is portrait cached offline that cannot be converted to ParseFile yet.
	boolean imgFromTmpData = false; 

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
    if (getActionBar() != null) {
      getActionBar().hide();
    }
    setContentView(R.layout.activity_buffer_opening);
    currentUser = ParseUser.getCurrentUser();

    // Below is for the sake of push notification
    ParseInstallation.getCurrentInstallation().put("ecardId", currentUser.get("ecardId").toString());
    ParseInstallation.getCurrentInstallation().saveInBackground(new SaveCallback() {

		@Override
		public void done(ParseException arg0) {
			// TODO Auto-generated method stub
			Toast.makeText(getApplicationContext(), "saved", Toast.LENGTH_SHORT).show();
		}
	});

    // the animation becomes laggy when object.save() is occupying the main
    // thread
    // ImageView imageCover = (ImageView)findViewById(R.id.imageCover);
    // Animation animation1 =
    // AnimationUtils.loadAnimation(getApplicationContext(), R.anim.zoomin);
    // imageCover.startAnimation(animation1);

    // Create/refresh local copy every time app opens
    if (ECardUtils.isNetworkAvailable(this)) {
	    createLocalSelfCopy(currentUser);
	    // upon opening, Sloppily copy online conversations to local
		syncAllLocalConversations();		
	    // check ecardIds that were scanned/cached offline
	    checkCachedIds();
    } 
    // if tmpImgByteArray not null, need to convert regardless of network
    checkPortrait();    
    
    timerToJump();
  }
  
  private void checkPortrait() {
	  ParseQuery<ParseObject> query = ParseQuery.getQuery("ECardInfo");
	  query.fromLocalDatastore();
	  query.getInBackground(currentUser.get("ecardId").toString(),
	      new GetCallback<ParseObject>() {

			@Override
			public void done(ParseObject object, ParseException e) {
	          if (e == null && object != null) {
	        	byte[] tmpImgData = (byte[]) object.get("tmpImgByteArray");
	        	if(tmpImgData != null){
	        		imgFromTmpData = true;
	        	}
	          }
			}
		  
	  });
	
}

private void checkCachedIds() {
	 
	// Upon opening, if there is Internet connection, try to store cached IDs
	db = new ECardSQLHelper(this);
	// getting all local db data to check against EcardIds
	olDatas = db.getAllData();
	if (olDatas.size() != 0) {
		Toast.makeText(getBaseContext(), "Found unsaved Ecards", Toast.LENGTH_SHORT).show();
		// If there are unsaved offline list, check and save them
		scannedIDs = new LinkedList<String>();
		for (Iterator<OfflineData> iter = olDatas.iterator(); iter.hasNext();) {
			OfflineData olData = iter.next();
			String scannedID = olData.getEcardID();
			scannedIDs.add(scannedID);
		}
		addCachedEcardIds();
	}
		
	
}
  
  public void addCachedEcardIds() {
		ParseQuery<ParseObject> query = ParseQuery.getQuery("ECardInfo");
		query.whereContainedIn("objectId", scannedIDs);
		query.findInBackground(new FindCallback<ParseObject>() {

			@Override
			public void done(List<ParseObject> objects, ParseException e) {
				if (e == null) {
					if (objects.size() == 0) {
						// None in the saved EcardInfo IDs are valid, delete
						// everything
						Toast.makeText(getBaseContext(), "Entire list contains no valid EcardID", Toast.LENGTH_SHORT).show();
						if (olDatas.size() != 0) {
							// if the cached userID don't exist, delete local
							// records
							for (int i = 0; i < olDatas.size(); i++) {
								db.deleteData(olDatas.get(i));
							}
						}
					} else {
						// At least one Ecard objectId is valid
						List<String> ecardExistList = new LinkedList<String>();
						for (int i = 0; i < objects.size(); i++) {
							ecardExistList.add(objects.get(i).getObjectId());
							// create list of valid EcardIDs
						}
						for (Iterator<String> iter = scannedIDs.iterator(); iter.hasNext();) {
							String scannedID = iter.next();
							// loop over all local records and delete invalid
							// ones
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

						ParseQuery<ParseObject> query = ParseQuery.getQuery("ECardNote");
						// need to do it from server so to avoid duplicate
						// adding due to out-of-sync
						query.whereEqualTo("userId", currentUser.getObjectId());
						query.whereContainedIn("ecardId", scannedIDs);
						query.findInBackground(new FindCallback<ParseObject>() {

							@Override
							public void done(List<ParseObject> objects, ParseException e) {
								if (e == null) {
									ArrayList<String> toRemove = new ArrayList<String>();
									if (objects.size() != 0) {
										// these are the ecards that are already
										// collected
										for (Iterator<ParseObject> iter = objects.iterator(); iter.hasNext();) {
											ParseObject object = iter.next();
											Toast.makeText(getBaseContext(), "ECard " + object.get("ecardId").toString() + " already existed!",
													Toast.LENGTH_SHORT).show();
											toRemove.add(object.get("ecardId").toString());
											// Either way, delete the local db
											// record. This is because local db
											// is only a temp storage for
											// offline added Ecards
											// Should be emptied when cards are
											// collected
											List<OfflineData> olDatas = db.getData("ecardID", object.get("ecardId").toString());
											if (olDatas.size() != 0) {
												// if the record exists in local
												// db, delete it
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
										ParseQuery<ParseObject> query = ParseQuery.getQuery("ECardInfo");
										ParseObject object = null;
										try {
											object = query.get(scannedID);
										} catch (ParseException e1) {
											// TODO Auto-generated catch block
											Toast.makeText(getBaseContext(), "General Parse Error", Toast.LENGTH_SHORT).show();
											e1.printStackTrace();
										}
										if (object != null) {
											// if null, no worries, these info
											// can be filled in later in
											// "refresh"
											ecardNote.put("EcardUpdatedAt", object.getUpdatedAt());
											object.pinInBackground();
											Toast.makeText(getBaseContext(), "Ecard " + scannedID + " added!", Toast.LENGTH_SHORT).show();
										}
										ecardNote.saveInBackground();
										ecardNote.pinInBackground();
										// Either way, delete the local db
										// record. This is because local db is
										// only a temp storage for offline added
										// Ecards
										// Should be emptied when cards are
										// collected
										List<OfflineData> olDatas = db.getData("ecardID", scannedID);
										if (olDatas.size() != 0) {
											// if the record exists in local db,
											// delete it
											OfflineData olData = olDatas.get(0);
											db.deleteData(olData);
										}
									}
									scannedIDs.clear();

								} else {
									Toast.makeText(getBaseContext(), "General Parse query error", Toast.LENGTH_SHORT).show();
								}
							}
						});
					}
				} else {
					Toast.makeText(getBaseContext(), "General Parse query error", Toast.LENGTH_SHORT).show();
				}
			}
		});
	}

public void timerToJump() {
    int timeout = 1000;
    // make the activity visible for 3 seconds before transitioning
    // This is important when first time sign up or login on this device
    // allows time to create local self copy

    Timer timer = new Timer();
    timer.schedule(new TimerTask() {

      @Override
      public void run() {
        Intent intent = new Intent(getBaseContext(), ActivityMain.class);
        intent.putExtra("imgFromTmpData", imgFromTmpData);
        startActivity(intent);
      }
    }, timeout);

    int timeout1 = timeout + 500;
    timer.schedule(new TimerTask() {

      @Override
      public void run() {
        finish();
        // have to delayed finishing, so desktop don't show
      }
    }, timeout1);
  }

  public void createLocalSelfCopy(ParseUser currentUser) {
    // Refresh local copies of records upon login EACH TIME app opens

    ParseQuery<ParseObject> query = ParseQuery.getQuery("ECardInfo");
    query.getInBackground(currentUser.get("ecardId").toString(),
      new GetCallback<ParseObject>() {

        @Override
        public void done(final ParseObject object, ParseException e) {
          if (e == null && object != null) {
        	byte[] tmpImgData = (byte[]) object.get("tmpImgByteArray");
        	if(tmpImgData != null){
        		// if there is cached data in the array, convert to ParseFile then clear the array
        		final ParseFile file = new ParseFile("portrait.jpg", tmpImgData);					                
            	// cannot save in thread, otherwise file could be empty when Design saved
									
				file.saveInBackground(new SaveCallback(){

					@Override
					public void done(ParseException e) {
						if(e==null){
							Toast.makeText(ActivityBufferOpening.this, "Cached portrait saved!", Toast.LENGTH_SHORT).show();		        	
							object.put("portrait", file);
							object.remove("tmpImgByteArray");
							object.saveEventually();
						} else {
							Log.i("bufferopen","error saving portrait");
						}
					}
					
				});
				
        	}
            object.pinInBackground();
          } else {
            // If no internet connection, no local copy can be saved
            Log.d("BufferOpening", "Cannot save self copy");
          }
        }
      });
  }
  
  private void syncAllLocalConversations() {
		ParseQuery<ParseObject> query = ParseQuery.getQuery("Conversations");
		query.fromLocalDatastore();
      query.whereEqualTo("partyB", currentUser.get("ecardId").toString());
      query.findInBackground(new FindCallback<ParseObject>(){

			@Override
			public void done(List<ParseObject> objects, ParseException e) {
				// Sloppy way of syncing: delete all local copies of conversation, then 
				// re-pin all online conversations
				if(e == null){
					if(objects.size()!=0){
						ParseObject.unpinAllInBackground(objects, new DeleteCallback(){

							@Override
							public void done(ParseException arg0) {
								ParseQuery<ParseObject> query = ParseQuery.getQuery("Conversations");
						        query.whereEqualTo("partyB", currentUser.get("ecardId").toString());
						        query.findInBackground(new FindCallback<ParseObject>(){

									@Override
									public void done(List<ParseObject> objects, ParseException e) {
										if(e == null){
											if(objects.size()!=0){
												Toast.makeText(getApplicationContext(), "pinned conversations", Toast.LENGTH_SHORT).show();
												ParseObject.pinAllInBackground(objects);												
											}
										} else {
											e.printStackTrace();
										}
										
									}
						        });
							}
							
						});
						
					} else {
						// if locally there is no conversation records, directly pin from cloud
						ParseQuery<ParseObject> query = ParseQuery.getQuery("Conversations");
				        query.whereEqualTo("partyB", currentUser.get("ecardId").toString());
				        query.findInBackground(new FindCallback<ParseObject>(){

							@Override
							public void done(List<ParseObject> objects, ParseException e) {
								if(e == null){
									if(objects.size()!=0){
										Toast.makeText(getApplicationContext(), "pinned conversations", Toast.LENGTH_SHORT).show();
										ParseObject.pinAllInBackground(objects);												
									}
								} else {
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
