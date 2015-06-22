package com.micklestudios.knowell;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

import com.micklestudios.knowell.infrastructure.ConversationsListAdapter;
import com.micklestudios.knowell.infrastructure.UserInfo;
import com.micklestudios.knowell.utils.AsyncTasks;
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

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

public class ActivityConversations extends ActionBarActivity {

  protected static final int SAVE_CARD = 0;
  private ParseUser currentUser;
  public static ArrayList<UserInfo> potentialUsers;
  ConversationsListAdapter adapter;
  AlphaInAnimationAdapter animationAdapter;
  ListView listView;
  private Dialog dialog;
  private static final long SCAN_TIMEOUT = 5000;
  
//Possible modes of sorting.
 public static final int SORT_MODE_NAME_ASC = 1;
 public static final int SORT_MODE_NAME_DSC = 2;
 public static final int SORT_MODE_DATE_ASC = 3;
 public static final int SORT_MODE_DATE_DSC = 4;

 public static int currentSortMode = SORT_MODE_DATE_DSC;
 public static final String MY_PREFS_NAME = "KnoWellSyncParams";
 private static final long CONVERSATIONS_TIMEOUT = 60000;

  @Override
  protected void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_conversations);
    currentUser = ParseUser.getCurrentUser();
    
    showActionBar();
    
    dialog = new Dialog(ActivityConversations.this);
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
    dialog.setContentView(R.layout.layout_dialog_scanned_process);
    
    retrieveAllViews();
    
    initializeContactList();
    
  }

  private void retrieveAllViews() {
    listView = (ListView) findViewById(R.id.activity_conversations_listview);      
  }

  private void initializeContactList() {
  listView.setOnItemClickListener(new OnItemClickListener() {

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
      long id) {
//      final ImageView progress_image = (ImageView) dialog
//          .findViewById(R.id.process_dialog_image);
      final UserInfo selectedUser = (UserInfo) listView
        .getItemAtPosition(position);

      // add new card asynchronically
      if (ECardUtils.isNetworkAvailable(ActivityConversations.this)) {

        dialog.show();

        final SyncDataTaskScanQR scanQR = new SyncDataTaskScanQR(
          ActivityConversations.this, selectedUser.getObjId(), selectedUser
            .getFirstName(), selectedUser.getLastName());
        scanQR.execute();
        final Runnable myCancellable = new Runnable() {

          @Override
          public void run() {
            if (scanQR.getStatus() == AsyncTask.Status.RUNNING) {
              if (dialog.isShowing()) {
                dialog.dismiss();
              }
              Toast.makeText(getApplicationContext(),
                "Poor network ... Please try again", Toast.LENGTH_SHORT)
                .show();
              // network poor, turn to offline mode for card collection

              // upon failed network, dismiss dialog
              Intent intent = new Intent(getBaseContext(),
                ActivityConversations.class);
              // passing UserInfo is made possible through Parcelable
              intent.putExtra("userinfo", selectedUser);
              intent.putExtra("offlineMode", true);
              intent.putExtra("deletedNoteId", (String) null);
              startActivityForResult(intent, SAVE_CARD);
              scanQR.cancel(true);
            }
          }
        };
        final Handler handlerScanQR = new Handler();
        handlerScanQR.postDelayed(myCancellable, SCAN_TIMEOUT);

        TextView dialog_text = (TextView) dialog.findViewById(R.id.dialog_status);
        dialog_text.setText("Loading ...");
        // upon back button press, cancel both the scanQR AsyncTask and the
        // timed handler
        // progress_image.setBackgroundResource(R.drawable.progress);
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
        Toast.makeText(getApplicationContext(), "No network ... ",
          Toast.LENGTH_SHORT).show();
        
        // search localdatastore and warn if note already exists
        ParseQuery<ParseObject> queryNote = ParseQuery.getQuery("ECardNote");
        queryNote.fromLocalDatastore();
        Log.i("chk", currentUser.getObjectId().toString() + "  " + selectedUser.getObjId());
        queryNote.whereEqualTo("userId", currentUser.getObjectId().toString());
        queryNote.whereEqualTo("ecardId", selectedUser.getObjId());
        
        
        List<ParseObject> noteObjs = null;
        try {
          noteObjs = queryNote.find();
        } catch (ParseException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
        if(noteObjs != null && noteObjs.size() != 0){
          Toast.makeText(getApplicationContext(), "Ecard may have been collected already ",
            Toast.LENGTH_SHORT).show();
        }

        Intent intent = new Intent(getBaseContext(), ActivityScanned.class);
        // passing UserInfo is made possible through Parcelable
        intent.putExtra("userinfo", selectedUser);
        intent.putExtra("offlineMode", true);
        intent.putExtra("deletedNoteId", (String) null);
        startActivityForResult(intent, SAVE_CARD);
        
      }
      
    }
  });
  
  listView.setOnItemLongClickListener(new OnItemLongClickListener() {
    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view,
      final int position, long id) {
      final CharSequence[] items = {
        "Delete this record ..."
      };
      
      AlertDialog.Builder builder = new AlertDialog.Builder(ActivityConversations.this);
      builder.setItems(items, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int item) {
            if(item == 0){
              // select to delete this item
              UserInfo selectedRecord = (UserInfo) listView.getItemAtPosition(position);
              potentialUsers.remove(selectedRecord);
              pupulateListView();
              ParseQuery<ParseObject> queryConvs = ParseQuery.getQuery("Conversations");
              queryConvs.fromLocalDatastore();
              queryConvs.whereEqualTo("partyB", currentUser.get("ecardId").toString());
              queryConvs.whereEqualTo("partyA", selectedRecord.getObjId());
              queryConvs.findInBackground(new FindCallback<ParseObject>(){

                @Override
                public void done(List<ParseObject> objects, ParseException e) {
                  if(e==null){
                    for(ParseObject obj : objects){
                      obj.put("isDeleted", true);
                      obj.saveEventually();
                    }
                  }
                }
                
              });
            }
          }
      });
      AlertDialog alert = builder.create();
      alert.show();
      return true;
    }
  });
  
  
  pupulateListView();
  
  
}
  
  private void pupulateListView() {
    LinearLayout noNotifView = (LinearLayout) findViewById(R.id.no_notifications);
    if(potentialUsers == null || potentialUsers.size() == 0){
      listView.setVisibility(View.GONE);
      noNotifView.setVisibility(View.VISIBLE);
    }
    adapter = new ConversationsListAdapter(getApplicationContext(),
      potentialUsers);
    animationAdapter = new AlphaInAnimationAdapter(adapter);
    animationAdapter.setAbsListView(listView);
    assert animationAdapter.getViewAnimator() != null;
    animationAdapter.getViewAnimator().setInitialDelayMillis(100);

    listView.setAdapter(animationAdapter);
    currentSortMode = SORT_MODE_DATE_DSC;
    adapter.reSort();
    animationAdapter.notifyDataSetChanged();
  }

  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.conv_actionbar, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // this function is called when either action bar icon is tapped
    switch (item.getItemId()) {
    case R.id.manual_sync:
      if(ECardUtils.isNetworkAvailable(this)){
        // check sharedpreferences
        final SharedPreferences prefs = getSharedPreferences(MY_PREFS_NAME,
          MODE_PRIVATE);
        SharedPreferences.Editor prefEditor = prefs.edit();
        // manual sync conversations, pin online conversations to local
        final AsyncTasks.SyncDataTaskConversations syncConversations = new AsyncTasks.SyncDataTaskConversations(
          this, currentUser, prefs, prefEditor, true);
        syncConversations.execute();
        Handler handlerConversations = new Handler();
        handlerConversations.postDelayed(new Runnable() {
  
          @Override
          public void run() {
            if (syncConversations.getStatus() == AsyncTask.Status.RUNNING) {
              Toast.makeText(getApplicationContext(),
                "Sync Notifications Timed Out", Toast.LENGTH_SHORT).show();
              syncConversations.cancel(true);
            }
          }
        }, CONVERSATIONS_TIMEOUT);
      } else{
        Toast.makeText(getApplicationContext(),
          "No network ...", Toast.LENGTH_SHORT).show();
      }
      return true;
    default:
      return super.onOptionsItemSelected(item);
    }
  }
 
  @SuppressLint("InflateParams")
  private void showActionBar() {
    LayoutInflater inflator = (LayoutInflater) this
      .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    View v = inflator.inflate(R.layout.layout_actionbar_search, null);
    LinearLayout btnBack = (LinearLayout) v.findViewById(R.id.btn_back);
    TextView title = (TextView) v.findViewById(R.id.search_actionbar_title);
    title.setText("Notifications");
    btnBack.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {
        onBackPressed();
      }

    });
    if (getSupportActionBar() != null) {
      ActionBar actionBar = getSupportActionBar();
      actionBar.setDisplayHomeAsUpEnabled(false);
      actionBar.setDisplayShowHomeEnabled(false);
      actionBar.setDisplayShowCustomEnabled(true);
      actionBar.setDisplayShowTitleEnabled(false);
      actionBar.setCustomView(v);
    }
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
