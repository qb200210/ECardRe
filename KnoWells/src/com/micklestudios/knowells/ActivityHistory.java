package com.micklestudios.knowells;

import java.util.List;

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
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.micklestudios.knowells.R;
import com.micklestudios.knowells.infrastructure.HistoryListAdapter;
import com.micklestudios.knowells.infrastructure.UserInfo;
import com.micklestudios.knowells.utils.AppGlobals;
import com.micklestudios.knowells.utils.AsyncTasks;
import com.micklestudios.knowells.utils.ECardUtils;
import com.nhaarman.listviewanimations.appearance.simple.AlphaInAnimationAdapter;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

public class ActivityHistory extends ActionBarActivity {

  protected static final int SAVE_CARD = 0;
  private ParseUser currentUser;
  HistoryListAdapter adapter;
  AlphaInAnimationAdapter animationAdapter;
  ListView listView;
  private Dialog dialog;
  private static final long SCAN_TIMEOUT = 5000;
  private static final long HISTORY_TIMEOUT = 60000;

  @Override
  protected void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_history);
    currentUser = ParseUser.getCurrentUser();

    showActionBar();

    dialog = new Dialog(ActivityHistory.this);
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
    dialog.setContentView(R.layout.layout_dialog_scanned_process);

    retrieveAllViews();

    initializeContactList();

  }

  private void retrieveAllViews() {
    listView = (ListView) findViewById(R.id.activity_history_listview);
  }

  private void initializeContactList() {
    listView.setOnItemClickListener(new OnItemClickListener() {

      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position,
        long id) {
        ParseObject selectedRecord = (ParseObject) listView
          .getItemAtPosition(position);
        editHistory(selectedRecord);
      }
    });

    listView.setOnItemLongClickListener(new OnItemLongClickListener() {
      @Override
      public boolean onItemLongClick(AdapterView<?> parent, View view,
        final int position, long id) {

        final CharSequence[] items = { "Delete this record ..." };

        AlertDialog.Builder builder = new AlertDialog.Builder(
          ActivityHistory.this);
        builder.setItems(items, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int item) {
            if (item == 0) {
              // select to delete this item
              ParseObject selectedRecord = (ParseObject) listView
                .getItemAtPosition(position);
              selectedRecord.put("isDeleted", true);
              selectedRecord.saveEventually();
              adapter.remove(selectedRecord);
              populateListView();
            }
          }
        });
        AlertDialog alert = builder.create();
        alert.show();
        return true;
      }
    });

    populateListView();

  }

  private void populateListView() {
    ParseQuery<ParseObject> queryHistory = ParseQuery.getQuery("History");
    queryHistory.fromLocalDatastore();
    queryHistory.whereEqualTo("userId", currentUser.getObjectId().toString());
    queryHistory.whereNotEqualTo("isDeleted", true);
    queryHistory.whereExists("objectId");
    queryHistory.findInBackground(new FindCallback<ParseObject>() {

      @Override
      public void done(List<ParseObject> objects, ParseException e) {
        if (e == null) {
          LinearLayout noNotifView = (LinearLayout) findViewById(R.id.no_history);
          if (objects == null || objects.size() == 0) {
            listView.setVisibility(View.GONE);
            noNotifView.setVisibility(View.VISIBLE);
          } else {
            listView.setVisibility(View.VISIBLE);
            noNotifView.setVisibility(View.GONE);
          }
          adapter = new HistoryListAdapter(getApplicationContext(), objects);
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

    dialogTitle.setText(android.text.format.DateFormat.format("MMM",
      selectedRecord.getCreatedAt())
      + " "
      + android.text.format.DateFormat.format("dd",
        selectedRecord.getCreatedAt())
      + " "
      + android.text.format.DateFormat.format("yyyy",
        selectedRecord.getCreatedAt()));

    final EditText addHistoryNameView = (EditText) dialogView
      .findViewById(R.id.add_history_name);
    final EditText addHistoryEmailView = (EditText) dialogView
      .findViewById(R.id.add_history_email);
    final EditText addHistorySmsView = (EditText) dialogView
      .findViewById(R.id.add_history_sms);
    final EditText addHistoryNotesView = (EditText) dialogView
      .findViewById(R.id.add_history_note);

    if (selectedRecord.get("fullName") != null) {
      addHistoryNameView.setText(selectedRecord.get("fullName").toString());
    }
    if (selectedRecord.get("email") != null) {
      addHistoryEmailView.setText(selectedRecord.get("email").toString());
    }
    if (selectedRecord.get("message") != null) {
      addHistorySmsView.setText(selectedRecord.get("message").toString());
    }
    if (selectedRecord.get("notes") != null) {
      addHistoryNotesView.setText(selectedRecord.get("notes").toString());
    }

    new AlertDialog.Builder(this).setView(dialogView)
      .setPositiveButton("Save", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int whichButton) {

          String nameString = addHistoryNameView.getText().toString();
          String emailString = addHistoryEmailView.getText().toString();
          String messageString = addHistorySmsView.getText().toString();
          String notesString = addHistoryNotesView.getText().toString();

          if (nameString != null && !nameString.isEmpty()) {
            selectedRecord.put("fullName", nameString);
          }
          if (emailString != null && !emailString.isEmpty()) {
            selectedRecord.put("email", emailString);
          }
          if (messageString != null && !messageString.isEmpty()) {
            selectedRecord.put("message", messageString);
          }
          if (notesString != null && !notesString.isEmpty()) {
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

  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.conv_actionbar, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // this function is called when either action bar icon is tapped
    switch (item.getItemId()) {
    case R.id.manual_sync:
      if (ECardUtils.isNetworkAvailable(this)) {
        // check sharedpreferences
        final SharedPreferences prefs = getSharedPreferences(
          AppGlobals.MY_PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor prefEditor = prefs.edit();
        // sync history, Supposely not critical, so don't need to wait on it
        final AsyncTasks.SyncDataTaskHistory syncHistory = new AsyncTasks.SyncDataTaskHistory(
          this, currentUser, prefs, prefEditor, true);
        syncHistory.execute();
        Handler handlerHistory = new Handler();
        handlerHistory.postDelayed(new Runnable() {

          @Override
          public void run() {
            if (syncHistory.getStatus() == AsyncTask.Status.RUNNING) {
              Toast.makeText(getApplicationContext(), "Sync History Timed Out",
                Toast.LENGTH_SHORT).show();
              syncHistory.cancel(true);
            }
          }
        }, HISTORY_TIMEOUT);

        Thread timerThread = new Thread() {

          public void run() {
            while (syncHistory.getStatus() == AsyncTask.Status.RUNNING) {
              try {
                sleep(500);
              } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
              }
            }
            populateListView();
          }
        };
        timerThread.start();

      } else {
        Toast.makeText(getApplicationContext(), "No network ...",
          Toast.LENGTH_SHORT).show();
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
      Log.i("add", currentUser.getObjectId() + "  " + scannedId);
      queryNote.whereEqualTo("userId", currentUser.getObjectId());
      queryNote.whereEqualTo("ecardId", scannedId);
      List<ParseObject> foundNotes = null;
      try {
        foundNotes = queryNote.find();
      } catch (ParseException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      if (foundNotes == null || foundNotes.size() == 0) {
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
          // if the ecard exists and not collected, create the userInfo using
          // found object
          UserInfo newUser = new UserInfo(objectScanned);
          return newUser;
        }
      } else {
        // if the note existed, check whether collected or deleted
        boolean isDeleted = foundNotes.get(0).getBoolean("isDeleted");
        if (!isDeleted) {
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
            // if the ecard exists and not collected, create the userInfo using
            // found object
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
      if (flagAlreadyCollected) {
        Toast.makeText(getBaseContext(), "Ecard already collected",
          Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(getBaseContext(), ActivityDetails.class);
        // passing UserInfo is made possible through Parcelable
        intent.putExtra("userinfo", newUser);
        startActivity(intent);
        if (dialog.isShowing()) {
          dialog.dismiss();
        }
      } else if (flagCardDoesnotExist) {
        Toast.makeText(getBaseContext(), "Ecard invalid", Toast.LENGTH_SHORT)
          .show();
        if (dialog.isShowing()) {
          dialog.dismiss();
        }
      } else if (deletedNoteId != null && !flagCardDoesnotExist) {
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
        intent.putExtra("deletedNoteId", (String) null);
        startActivity(intent);
        if (dialog.isShowing()) {
          dialog.dismiss();
        }
      }
    }

  }

}
