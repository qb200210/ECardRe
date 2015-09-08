package com.micklestudios.knowells;

import java.util.HashMap;
import java.util.List;

import com.google.zxing.Result;
import com.micklestudios.knowells.infrastructure.UserInfo;
import com.micklestudios.knowells.utils.AppGlobals;
import com.micklestudios.knowells.utils.ECardUtils;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

public class ActivityOpenLink extends ActionBarActivity{
  private Dialog dialog;
  private static final int OPEN_WEB = 3001;
  private static final long SCAN_TIMEOUT = 60000;

  @Override
  protected void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // setContentView(R.layout.activity_qrscanner);
    AppGlobals.initializeAllContactsBlocking();
    AppGlobals.initializePotentialUsersBlocking(); 
    // handleDecode();
    final Intent intent = getIntent();
    final String action = intent.getAction();
    if(intent.getData()!=null){
      // Toast.makeText(this, intent.getDataString(), Toast.LENGTH_SHORT).show();
      Intent intent1 = new Intent(this,
        ActivityMain.class);
      startActivity(intent1);
      handleDecode(intent.getData().toString());
    } else{
      Intent intent1 = new Intent(this,
        ActivityMain.class);
      startActivity(intent1);
      this.finish();
    }
  }
  
  public void handleDecode(String rawResult) {
    // Create an instance of the dialog fragment and show it
    dialog = new Dialog(this);
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
    dialog.setContentView(R.layout.layout_dialog_scanned_process);

    TextView dialog_text = (TextView) dialog.findViewById(R.id.dialog_status);
    // ImageView progress_image = (ImageView) dialog
    // .findViewById(R.id.process_dialog_image);

    HashMap<String, String> valuesMap = ECardUtils.parseQRString(rawResult);

    if (valuesMap == null) {
      dialog_text.setText("The scanned QR is invalid");
      Intent intent = new Intent(getBaseContext(), ActivityWebView.class);
      intent.putExtra("url", rawResult);
      startActivityForResult(intent, OPEN_WEB);
      
      // progress_image.setBackgroundResource(R.drawable.ic_action_cancel);

      dialog.show();

      // Hide after some seconds
      final Handler handler = new Handler();
      final Runnable runnable = new Runnable() {
        @Override
        public void run() {
          if (dialog.isShowing()) {
            dialog.dismiss();
            finish();
          }

          // onPause();
          // onResume();
        }
      };

      handler.postDelayed(runnable, 2000);
    } else {
      dialog_text.setText("Successfully Identified QR Code. Processing...");
      // progress_image.setBackgroundResource(R.drawable.ic_action_done);

      final String scannedId = valuesMap.get("id");
      final String firstName = valuesMap.get("fn");
      final String lastName = valuesMap.get("ln");

      // Will be hanging if cannot pull from Parse, since this is not done in
      // background thread

      // add new card asynchronically
      if (ECardUtils.isNetworkAvailable(this)) {
        final SyncDataTaskScanQR scanQR = new SyncDataTaskScanQR(this,
          scannedId, firstName, lastName);
        scanQR.execute();
        final Runnable myCancellable = new Runnable() {

          @Override
          public void run() {
            if (scanQR.getStatus() == AsyncTask.Status.RUNNING) {
              Toast.makeText(getApplicationContext(), "Add New Card Timed Out",
                Toast.LENGTH_SHORT).show();
              // network poor, turn to offline mode for card collection
              UserInfo newUser = new UserInfo(scannedId, firstName, lastName,
                false, false, false);
              // upon failed network, dismiss dialog
              if (dialog.isShowing()) {
                dialog.dismiss();
              }
              Intent intent = new Intent(getBaseContext(),
                ActivityScanned.class);
              // passing UserInfo is made possible through Parcelable
              intent.putExtra("userinfo", newUser);
              intent.putExtra("offlineMode", true);
              intent.putExtra("deletedNoteId", (String) null);
              startActivity(intent);
              scanQR.cancel(true);
              finish();
            }
          }
        };
        final Handler handlerScanQR = new Handler();
        handlerScanQR.postDelayed(myCancellable, SCAN_TIMEOUT);

        // upon back button press, cancel both the scanQR AsyncTask and the
        // timed handler
        // progress_image.setBackgroundResource(R.drawable.progress);
        dialog_text.setText("Processing");
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
          @Override
          public void onCancel(DialogInterface dialog) {
            Toast.makeText(getApplicationContext(), "canceled",
              Toast.LENGTH_SHORT).show();
            scanQR.cancel(true);
            handlerScanQR.removeCallbacks(myCancellable);
            onPause();
            onResume();
          }
        });
        // QB: Need fix! Window leaked
        dialog.show();
      } else {
        // no network, directly switch to offline card collection mode
        UserInfo newUser = new UserInfo(scannedId, firstName, lastName, false,
          false, false);
        if (dialog.isShowing()) {
          dialog.dismiss();
        }
        Toast.makeText(getApplicationContext(), "Network unavailable",
          Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(getBaseContext(), ActivityScanned.class);
        // passing UserInfo is made possible through Parcelable
        intent.putExtra("userinfo", newUser);
        intent.putExtra("offlineMode", true);
        intent.putExtra("deletedNoteId", (String) null);
        startActivity(intent);
        finish();
      }

    }
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
        finish();
      } else if (flagCardDoesnotExist) {
        Toast.makeText(getBaseContext(), "Ecard invalid", Toast.LENGTH_SHORT)
          .show();
        if (dialog.isShowing()) {
          dialog.dismiss();
        }
        onPause();
        onResume();
      } else if (deletedNoteId != null && !flagCardDoesnotExist) {
        Intent intent = new Intent(getBaseContext(), ActivityScanned.class);
        // passing UserInfo is made possible through Parcelable
        intent.putExtra("userinfo", newUser);
        intent.putExtra("offlineMode", false);
        intent.putExtra("deletedNoteId", deletedNoteId);
        startActivity(intent);
        finish();
      } else {
        // upon successful scan and pull of info
        Intent intent = new Intent(getBaseContext(), ActivityScanned.class);
        // passing UserInfo is made possible through Parcelable
        intent.putExtra("userinfo", newUser);
        intent.putExtra("offlineMode", false);
        intent.putExtra("deletedNoteId", (String) null);
        startActivity(intent);
        finish();
      }
    }

  }
}
