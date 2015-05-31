package com.micklestudios.knowell;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

import com.micklestudios.knowell.infrastructure.ConversationsListAdapter;
import com.micklestudios.knowell.infrastructure.HistoryListAdapter;
import com.micklestudios.knowell.infrastructure.UserInfo;
import com.micklestudios.knowell.utils.ECardUtils;
import com.micklestudios.knowell.utils.RobotoEditText;
import com.nhaarman.listviewanimations.appearance.StickyListHeadersAdapterDecorator;
import com.nhaarman.listviewanimations.appearance.simple.AlphaInAnimationAdapter;
import com.nhaarman.listviewanimations.util.StickyListHeadersListViewWrapper;
import com.parse.FindCallback;
import com.parse.ParseACL;
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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class ActivityHistory extends ActionBarActivity {

  protected static final int SAVE_CARD = 0;
  private ParseUser currentUser;
  public static ArrayList<ParseObject> historyObjects;
  HistoryListAdapter adapter;
  AlphaInAnimationAdapter animationAdapter;
  ListView listView;
  private Dialog dialog;
  private static final long SCAN_TIMEOUT = 5000;


  @Override
  protected void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_conversations);
    currentUser = ParseUser.getCurrentUser();
    
    showActionBar();
    
    dialog = new Dialog(ActivityHistory.this);
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
    dialog.setContentView(R.layout.layout_dialog_scanned_process);
    
    retrieveAllViews();
    
    initializeContactList();
    
  }

  private void retrieveAllViews() {
    listView = (ListView) findViewById(R.id.activity_conversations_listview);  
    LinearLayout noNotifView = (LinearLayout) findViewById(R.id.no_notifications);
//    if(historyObjects == null || historyObjects.size() == 0){
//      listView.setVisibility(View.GONE);
//      noNotifView.setVisibility(View.VISIBLE);
//    }
  }

  private void initializeContactList() {
  listView.setOnItemClickListener(new OnItemClickListener() {

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
      long id) {    
      ParseObject selectedRecord = (ParseObject) listView.getItemAtPosition(position);
      editHistory(selectedRecord);
    }
  });
  ParseQuery<ParseObject> queryHistory = ParseQuery.getQuery("History");
  queryHistory.fromLocalDatastore();
  queryHistory.whereEqualTo("userId", currentUser.getObjectId().toString());
  queryHistory.findInBackground(new FindCallback<ParseObject>(){

    @Override
    public void done(List<ParseObject> objects, ParseException e) {
      if(e == null){
        adapter = new HistoryListAdapter(getApplicationContext(),
          objects);
        animationAdapter = new AlphaInAnimationAdapter(adapter);
        animationAdapter.setAbsListView(listView);
        assert animationAdapter.getViewAnimator() != null;
        animationAdapter.getViewAnimator().setInitialDelayMillis(100);

        listView.setAdapter(animationAdapter);
        adapter.reSortDate(false);
        animationAdapter.notifyDataSetChanged();
      } else {
        e.printStackTrace();
      }
      
    }
    
  });
  
  
  
}
  
  private void editHistory(final ParseObject selectedRecord) {
    
    LayoutInflater inflater = getLayoutInflater();
    final View dialogView = inflater.inflate(R.layout.layout_add_history, null);
    LinearLayout dialogHeader = (LinearLayout) dialogView
      .findViewById(R.id.dialog_header);
    final TextView dialogText = (TextView) dialogView
      .findViewById(R.id.dialog_text);
    TextView dialogTitle = (TextView) dialogView
      .findViewById(R.id.dialog_title);
    dialogHeader
      .setBackgroundColor(getResources().getColor(R.color.blue_extra));
    // Set dialog title and main EditText
    dialogTitle.setText("Keep record?");
    
    final RobotoEditText addHistoryNameView = (RobotoEditText) dialogView.findViewById(R.id.add_history_name);
    final RobotoEditText addHistoryEmailView = (RobotoEditText) dialogView.findViewById(R.id.add_history_email);
    final RobotoEditText addHistorySmsView = (RobotoEditText) dialogView.findViewById(R.id.add_history_sms);
    final RobotoEditText addHistoryNotesView = (RobotoEditText) dialogView.findViewById(R.id.add_history_note);
    
    if(selectedRecord.get("fullName") != null){
      addHistoryNameView.setText(selectedRecord.get("fullName").toString());
    }
    if(selectedRecord.get("email") != null){
      addHistoryEmailView.setText(selectedRecord.get("email").toString());
    }
    if(selectedRecord.get("message") != null){
      addHistorySmsView.setText(selectedRecord.get("message").toString());
    }
    if(selectedRecord.get("notes") != null){
      addHistoryNotesView.setText(selectedRecord.get("notes").toString());
    }
    
    new AlertDialog.Builder(this).setView(dialogView)
    .setPositiveButton("Save", new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int whichButton) {

        String nameString = addHistoryNameView.getText().toString();
        String emailString = addHistoryEmailView.getText().toString();
        String messageString = addHistorySmsView.getText().toString();
        String notesString = addHistoryNotesView.getText().toString();
        
        if(nameString!=null && !nameString.isEmpty()){
          selectedRecord.put("fullName", nameString);
        }
        if(emailString!=null && !emailString.isEmpty()){
          selectedRecord.put("email", emailString);
        }
        if(messageString!=null && !messageString.isEmpty()){
          selectedRecord.put("message", messageString);
        }
        if(notesString!=null && !notesString.isEmpty()){
          selectedRecord.put("notes", notesString);
        }
        selectedRecord.saveEventually();
        selectedRecord.pinInBackground();

        adapter.notifyDataSetChanged();
        listView.invalidateViews();
        
      }
    }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int whichButton) {

      }
    }).setCancelable(false).show();
  }
 
  @SuppressLint("InflateParams")
  private void showActionBar() {
    LayoutInflater inflator = (LayoutInflater) this
      .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    View v = inflator.inflate(R.layout.layout_actionbar_search, null);
    LinearLayout btnBack = (LinearLayout) v.findViewById(R.id.btn_back);
    TextView title = (TextView) v.findViewById(R.id.search_actionbar_title);
    title.setText("History");
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
