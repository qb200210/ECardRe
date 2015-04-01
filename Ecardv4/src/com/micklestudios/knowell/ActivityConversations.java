package com.micklestudios.knowell;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

import com.micklestudios.knowell.infrastructure.ConversationsListAdapter;
import com.micklestudios.knowell.infrastructure.UserInfo;
import com.micklestudios.knowell.utils.ECardUtils;
import com.nhaarman.listviewanimations.appearance.StickyListHeadersAdapterDecorator;
import com.nhaarman.listviewanimations.appearance.simple.AlphaInAnimationAdapter;
import com.nhaarman.listviewanimations.util.StickyListHeadersListViewWrapper;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.micklestudios.knowell.R;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class ActivityConversations extends ActionBarActivity {

  protected static final int SAVE_CARD = 0;
  private ParseUser currentUser;
  public static ArrayList<UserInfo> userNames;
  ConversationsListAdapter adapter;
  AlphaInAnimationAdapter animationAdapter;
  StickyListHeadersAdapterDecorator stickyListHeadersAdapterDecorator;
  StickyListHeadersListView listView;
  private Dialog dialog;
  private static final long SCAN_TIMEOUT = 5000;

  @Override
  protected void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_conversations);
    currentUser = ParseUser.getCurrentUser();
    userNames = new ArrayList<UserInfo>();
    
    dialog = new Dialog(ActivityConversations.this);
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
    dialog.setContentView(R.layout.layout_dialog_scanned_process);
    final ImageView progress_image = (ImageView) dialog
    	      .findViewById(R.id.process_dialog_image);

    listView = (StickyListHeadersListView) findViewById(R.id.activity_conversations_listview);
    listView.setOnItemClickListener(new OnItemClickListener() {

	@Override
      public void onItemClick(AdapterView<?> parent, View view, int position,
        long id) {
        final UserInfo selectedUser = (UserInfo) listView.getItemAtPosition(position);

     // add new card asynchronically
        if (ECardUtils.isNetworkAvailable(ActivityConversations.this)) {        	
        	
            dialog.show();
        	
          final SyncDataTaskScanQR scanQR = new SyncDataTaskScanQR(ActivityConversations.this,
        		  selectedUser.getObjId(), selectedUser.getFirstName(), selectedUser.getLastName());
          scanQR.execute();
          final Runnable myCancellable = new Runnable() {

            @Override
            public void run() {
              if (scanQR.getStatus() == AsyncTask.Status.RUNNING) {
            	  if (dialog.isShowing()) {
                      dialog.dismiss();
                    }
                Toast.makeText(getApplicationContext(), "Poor network ... Please try again",
                  Toast.LENGTH_SHORT).show();
                // network poor, turn to offline mode for card collection
                
                // upon failed network, dismiss dialog
//                Intent intent = new Intent(getBaseContext(),
//                  ActivityScanned.class);
//                // passing UserInfo is made possible through Parcelable
//                intent.putExtra("userinfo", selectedUser);
//                intent.putExtra("offlineMode", true);
//      	        intent.putExtra("deletedNoteId", (String)null);
//      	        startActivityForResult(intent, SAVE_CARD);
                scanQR.cancel(true);
              }
            }
          };
          final Handler handlerScanQR = new Handler();
          handlerScanQR.postDelayed(myCancellable, SCAN_TIMEOUT);     
          
       // upon back button press, cancel both the scanQR AsyncTask and the
          // timed handler
          progress_image.setBackgroundResource(R.drawable.progress);
          dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
              Toast.makeText(getApplicationContext(), "canceled",
                Toast.LENGTH_SHORT).show();
              scanQR.cancel(true);
              handlerScanQR.removeCallbacks(myCancellable);
            }
          });
          // QB: Need fix! Window leaked
          dialog.show();
        } else {
          // no network, directly switch to offline card collection mode          
        	Toast.makeText(getApplicationContext(), "No network ... Please try again",
                    Toast.LENGTH_SHORT).show();

//          Intent intent = new Intent(getBaseContext(), ActivityScanned.class);
//          // passing UserInfo is made possible through Parcelable
//          intent.putExtra("userinfo", selectedUser);
//          intent.putExtra("offlineMode", false);
//  	      intent.putExtra("deletedNoteId", (String)null);
//          startActivityForResult(intent, SAVE_CARD);
        }
        
      }
    });

    getContacts();
  }

  private void getContacts() {
    ParseQuery<ParseObject> query = ParseQuery.getQuery("Conversations");
    query.fromLocalDatastore();
    query.whereEqualTo("partyB", currentUser.get("ecardId").toString());
    query.findInBackground(new FindCallback<ParseObject>() {

      @Override
      public void done(List<ParseObject> conversationList, ParseException e) {
        if (e == null) {
          if (conversationList.size() != 0) {
            Toast.makeText(ActivityConversations.this,
              "conv: " + conversationList.size(), Toast.LENGTH_SHORT).show();
            for (Iterator<ParseObject> iter = conversationList.iterator(); iter
              .hasNext();) {
              ParseObject objectConversation = iter.next();
              // collect EcardInfo IDs satisfying Note searches
              // here object is Note object
              String ecardIdString = (String) objectConversation.get("partyA");
              // NEED FIX: If somehow the card wasn't cached to local, it will
              // crash
              UserInfo contact = new UserInfo(ecardIdString, "", "", true,
                false, false);
              userNames.add(contact);
            }
            
            Log.i("usr", userNames.toString());

            adapter = new ConversationsListAdapter(ActivityConversations.this, userNames);
            animationAdapter = new AlphaInAnimationAdapter(adapter);
            stickyListHeadersAdapterDecorator = new StickyListHeadersAdapterDecorator(
              animationAdapter);
            stickyListHeadersAdapterDecorator
              .setListViewWrapper(new StickyListHeadersListViewWrapper(listView));

            assert animationAdapter.getViewAnimator() != null;
            animationAdapter.getViewAnimator().setInitialDelayMillis(500);

            assert stickyListHeadersAdapterDecorator.getViewAnimator() != null;
            stickyListHeadersAdapterDecorator.getViewAnimator()
              .setInitialDelayMillis(500);

            listView.setAdapter(stickyListHeadersAdapterDecorator);
            adapter.reSortName(true);
            stickyListHeadersAdapterDecorator.notifyDataSetChanged();
          }
        } else {
          Toast.makeText(getBaseContext(), "General parse error!",
            Toast.LENGTH_SHORT).show();
        }
      }
    });
  }

  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

  }
  
  private class SyncDataTaskScanQR extends AsyncTask<String, Void, UserInfo> {

	    private Context context;
	    private String scannedId;
	    private String firstName;
	    private String lastName;
		private boolean flagCardDoesnotExist = false;
		private boolean flagAlreadyCollected = false;
		private String deletedNoteId = null;

	    public SyncDataTaskScanQR(Context context, String scannedId,
	      String firstName, String lastName) {
	      this.context = context;
	      this.scannedId = scannedId;
	      this.firstName = firstName;
	      this.lastName = lastName;

	    }

	    @Override
	    protected UserInfo doInBackground(String... url) {
	      // check if ecard actually exists? check if ecard already collected?
	    	ParseUser currentUser = ParseUser.getCurrentUser();
	    	ParseQuery<ParseObject> queryNote = ParseQuery.getQuery("ECardNote");
	    	Log.i("add", currentUser.getObjectId() + "  "+ scannedId);
	    	queryNote.whereEqualTo("userId", currentUser.getObjectId());
			queryNote.whereEqualTo("ecardId", scannedId);
			List<ParseObject> foundNotes = null;
	    	try {
	    		foundNotes = queryNote.find();
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    	if(foundNotes == null || foundNotes.size()==0){
	    		// if either ecard doesn't exist or not collected
	    		ParseQuery<ParseObject> queryInfo = ParseQuery.getQuery("ECardInfo");
	    		ParseObject objectScanned = null;
	    		try {
					objectScanned = queryInfo.get(scannedId);
				} catch (ParseException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}
				if (objectScanned == null) {
					// if ecard doesn't exist
	    			flagCardDoesnotExist = true;
	    			return null;
				} else {
	    			// if the ecard exists and not collected, create the userInfo using found object
	    			UserInfo newUser = new UserInfo(objectScanned);
	    			return newUser;
	    		}
	    	} else {
	    		// if the note existed, check whether collected or deleted
	    		boolean isDeleted = foundNotes.get(0).getBoolean("isDeleted");
	    		if(!isDeleted){
		    		flagAlreadyCollected = true;
		    		UserInfo newUser = new UserInfo(scannedId, firstName, lastName, true,
		    		        true, false);
		    		return newUser;
	    		} else {
	    			// note existed but already deleted. Now recover it
	    			deletedNoteId = foundNotes.get(0).getObjectId();
	    			ParseQuery<ParseObject> queryInfo = ParseQuery.getQuery("ECardInfo");
	        		ParseObject objectScanned = null;
	        		try {
	    				objectScanned = queryInfo.get(scannedId);
	    			} catch (ParseException e2) {
	    				// TODO Auto-generated catch block
	    				e2.printStackTrace();
	    			}
	    			if (objectScanned == null) {
	    				// if ecard doesn't exist
	        			flagCardDoesnotExist = true;
	        			return null;
	    			} else {
	        			// if the ecard exists and not collected, create the userInfo using found object
	        			UserInfo newUser = new UserInfo(objectScanned);
	        			return newUser;
	        		}
	    		}
	    	}
	    
	      // Create new userInfo class based on the scannedId.
	      // Will pull info from Parse
	      
	    }

	    @Override
	    protected void onPostExecute(UserInfo newUser) {
	      // if ecard already collected, switch to ActivityDetails
	      if(flagAlreadyCollected){
	    	  Toast.makeText(getBaseContext(), "Ecard already collected", Toast.LENGTH_SHORT).show();
	    	  Intent intent = new Intent(getBaseContext(), ActivityDetails.class);
	          // passing UserInfo is made possible through Parcelable
	          intent.putExtra("userinfo", newUser);
	          startActivity(intent);
	          if (dialog.isShowing()) {
                  dialog.dismiss();
                }
	      } else if(flagCardDoesnotExist){
	    	  Toast.makeText(getBaseContext(), "Ecard invalid", Toast.LENGTH_SHORT).show();
	    	  if (dialog.isShowing()) {
                  dialog.dismiss();
                }
	      } else if(deletedNoteId!=null && !flagCardDoesnotExist){
	    	  Intent intent = new Intent(getBaseContext(), ActivityScanned.class);
		      // passing UserInfo is made possible through Parcelable
		      intent.putExtra("userinfo", newUser);
		      intent.putExtra("offlineMode", false);
		      intent.putExtra("deletedNoteId", deletedNoteId);
		      startActivity(intent);
		      if (dialog.isShowing()) {
                  dialog.dismiss();
                }
	      } else {    	
		      // upon successful scan and pull of info
		      Intent intent = new Intent(getBaseContext(), ActivityScanned.class);
		      // passing UserInfo is made possible through Parcelable
		      intent.putExtra("userinfo", newUser);
		      intent.putExtra("offlineMode", false);
		      intent.putExtra("deletedNoteId", (String)null);
		      startActivity(intent);
		      if (dialog.isShowing()) {
                  dialog.dismiss();
                }
	      }
	    }

	  }

}
