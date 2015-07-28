package com.micklestudios.knowell;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ToxicBakery.viewpager.transforms.FlipHorizontalTransformer;
import com.micklestudios.knowell.infrastructure.UserInfo;
import com.micklestudios.knowell.utils.AsyncResponse;
import com.micklestudios.knowell.utils.AsyncTasks;
import com.micklestudios.knowell.utils.DetailsPagerAdapter;
import com.micklestudios.knowell.utils.ECardSQLHelperCachedIds;
import com.micklestudios.knowell.utils.ECardSQLHelperCachedShares;
import com.micklestudios.knowell.utils.ECardUtils;
import com.micklestudios.knowell.utils.GeocoderHelper;
import com.micklestudios.knowell.utils.MyScrollView;
import com.micklestudios.knowell.utils.MyViewPager;
import com.micklestudios.knowell.utils.OfflineDataCachedIds;
import com.micklestudios.knowell.utils.OfflineDataCachedShares;
import com.parse.FindCallback;
import com.parse.ParseACL;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParsePush;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

public class ActivityScanned extends ActivityDetails implements AsyncResponse {

  private static final long ADDCARD_TIMEOUT = 10000;
  private MyScrollView scrollView;

  ParseUser currentUser;
  private UserInfo scannedUser;
  private String noteId = null;

  // need to use this to hold the interface to be passed to GeocoderHelper
  // constructor, otherwise NullPoint
  AsyncResponse delegate = null;
  private String whereMet = null;
  private boolean flagOfflineMode;
  private String deletedNoteId = null;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    // two possiblities of entering ActivityScanned:
    // 1. online, checked ecard exist and not collected
    // 2. offline, didn't check ecard exist or if collected

    super.onCreate(savedInstanceState);
    currentUser = ParseUser.getCurrentUser();
    setContentView(R.layout.activity_scanned);
    showActionBar();
    filepath = getFilename();

    scrollView = (MyScrollView) findViewById(R.id.scroll_view_scanned);
    scrollView.setmScrollable(true);

    Bundle data = getIntent().getExtras();
    if(data != null){
      scannedUser = (UserInfo) data.getParcelable("userinfo");
      flagOfflineMode = (boolean) data.get("offlineMode");
      deletedNoteId = (String) data.get("deletedNoteId");
    }
    
    // This fixes the lost data/ crash issues upon restoring from resume
    if(savedInstanceState != null){
      scannedUser = savedInstanceState.getParcelable("userinfo");
      flagOfflineMode = savedInstanceState.getBoolean("offlineMode");
      deletedNoteId = savedInstanceState.getString("deletedNoteId");
    }
    
    TextView motto = (TextView) findViewById(R.id.motto);
    String tmpString = scannedUser.getMotto();
    if (tmpString != null)
      motto.setText(tmpString);

    DetailsPagerAdapter mAdapter = new DetailsPagerAdapter(
      getSupportFragmentManager(), scannedUser, this);

    final MyViewPager mPager = (MyViewPager) findViewById(R.id.pager22);
    mPager.setAdapter(mAdapter);
    mPager.setCurrentItem(0x40000000);
    mPager.setPageTransformer(true, new FlipHorizontalTransformer());

    if (!flagOfflineMode) {
      // get location only when online
      // getting "where met" city info
      // this will be used later -- where "this" is ambiguous, so directly
      // storing delegate for later use
      delegate = this;
      // if there is network, start a thread to get location name
      Location location = getLocation();
      if (location != null) {
        Log.i("ActScan", "location not null");
        new GeocoderHelper(delegate).fetchCityName(getBaseContext(), location);
      } else {
        // if getting location fails, will bypass the processFinish() function
        Toast.makeText(getBaseContext(), "I need to turn on GPS ...",
          Toast.LENGTH_SHORT).show();
        whereMet = null;
      }
    }

    // replay related
    replayButtonPanel = (ImageView) findViewById(R.id.panel_play_button);
    recorderButton = (ImageView) findViewById(R.id.panel_recorder_button);
    timerButton = (ImageView) findViewById(R.id.stop_recording);

    // display note if the note existed but deleted
    if (deletedNoteId != null) {
      ParseQuery<ParseObject> query = ParseQuery.getQuery("ECardNote");
      query.whereEqualTo("userId", currentUser.getObjectId().toString());
      query.whereEqualTo("ecardId", scannedUser.getObjId());
      query.findInBackground(new FindCallback<ParseObject>() {

        @Override
        public void done(List<ParseObject> objects, ParseException e) {
          if (e == null) {
            if (objects != null) {
              noteId = objects.get(0).getObjectId().toString();
              displayNote(objects.get(0));
            }
          } else {
            e.printStackTrace();
          }
        }
      });
    } else {
      // this is a new note
      TextView updatedAt = (TextView) findViewById(R.id.LastUpdated);
      Date today = new Date();
      TextView whenMet2 = (TextView) findViewById(R.id.DateAdded2);
      whenMet2.setText(android.text.format.DateFormat.format("MMM", today)
        + " " + android.text.format.DateFormat.format("dd", today) + ", "
        + android.text.format.DateFormat.format("yyyy", today));
      newDate = today;
      whenMet2.setOnClickListener(new OnClickListener() {

        @Override
        public void onClick(View v) {
          Calendar newCalendar = Calendar.getInstance();
          newCalendar.setTime(newDate);
          DatePickerDialog dialog = new DatePickerDialog(ActivityScanned.this,
            new mDateSetListener(), newCalendar.get(Calendar.YEAR), newCalendar
              .get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));
          dialog.setTitle("When did we meet?");
          dialog.show();
        }

      });
      updatedAt.setText(android.text.format.DateFormat.format("MMM", today)
        + " " + android.text.format.DateFormat.format("dd", today) + ", "
        + android.text.format.DateFormat.format("yyyy", today));
      // disable replay button!
      replayButtonPanel.setVisibility(View.GONE);
    }

    // recorder-related begins
    recorderButton.setOnClickListener(recorderListener);
    timerButton.setOnClickListener(timerListener);
    replayButtonPanel.setOnClickListener(replayListener);
    // recorder-related ends

    // This is the life-saver! It fixes the bug that scrollView will go to the
    // bottom of GridView upon open
    // below is to re-scroll to the first view in the LinearLayout
    // SquareLayout mainCardContainer = (SquareLayout)
    // findViewById(R.id.details_maincard_container);
    // scrollView.requestChildFocus(mainCardContainer, null);

  }
  
  @Override
  public void onSaveInstanceState(Bundle outState) {
    outState.putParcelable("userinfo", scannedUser);
    outState.putBoolean("offlineMode", flagOfflineMode);
    outState.putString("deletedNoteId", deletedNoteId);
    super.onSaveInstanceState(outState);
  }
 
  @Override
  void showActionBar() {
    LayoutInflater inflator = (LayoutInflater) this
      .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    View v = inflator.inflate(R.layout.layout_actionbar_search, null);
    LinearLayout btnBack = (LinearLayout) v.findViewById(R.id.btn_back);
    TextView title = (TextView) v.findViewById(R.id.search_actionbar_title);
    title.setText("Add Card");
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
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.details_actionbar, menu);
    return true;
  }
  
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // this function is called when either action bar icon is tapped
    switch (item.getItemId()) {
    case R.id.details_discard:
      Toast.makeText(this, "Discarded Card!", Toast.LENGTH_SHORT).show();
      setResult(RESULT_CANCELED);
      deleteLocalVoiceNote();
      this.finish();
      return true;
    case R.id.details_save:
      // save scanned card either online or cache it offline
      if (ECardUtils.isNetworkAvailable(this)) {
        // if ActivityScanned started out as online and now it's still online,
        // go ahead and add
        final AsyncTasks.AddCardNetworkAvailable addNewCard = new AsyncTasks.AddCardNetworkAvailable(
          this, currentUser, scannedUser.getObjId(), deletedNoteId, newDate);
        addNewCard.execute();
        Handler handlerAddNewCard = new Handler();
        handlerAddNewCard.postDelayed(new Runnable() {

          @Override
          public void run() {
            if (addNewCard.getStatus() == AsyncTask.Status.RUNNING) {
              Toast.makeText(getApplicationContext(),
                "Adding New Card Timed Out", Toast.LENGTH_SHORT).show();
              // if poor network, cache the scannedID to local db, wait till
              // network comes back to add Ecard
              cacheScannedIds(scannedUser.getObjId());
              addNewCard.cancel(true);
            }
          }
        }, ADDCARD_TIMEOUT);
        // Upon save when online, no matter if save successful, ask whether want
        // to share back
        // Will check if notification already exists
        askIfShareBack(true);
        Log.e("share", "after share back");
      } else {
        // if ActivityScanned started out as offline, it means there was no
        // check on ecard existence or collected, cache it for later check
        // no network, cache to local database
        cacheScannedIds(scannedUser.getObjId());
        // TO-DO: disabled offline shareback because there maybe no
        // userId, so ACL of conversation can be messy
        //askIfShareBack(false);
      }
      return true;
    default:
      return true;
    }
  }

  @SuppressLint("NewApi")
  private void askIfShareBack(final boolean isOnline) {
    // Get the layout inflater
    LayoutInflater inflater = getLayoutInflater();
    View dialogView = inflater.inflate(R.layout.layout_empty,
      null);
    LinearLayout dialogHeader = (LinearLayout) dialogView
      .findViewById(R.id.dialog_header);
    final TextView dialogText = (TextView) dialogView
      .findViewById(R.id.dialog_text);
    TextView dialogTitle = (TextView) dialogView
      .findViewById(R.id.dialog_title);
    dialogHeader
      .setBackgroundColor(getResources().getColor(R.color.blue_extra));
    // Set dialog title and main EditText
    dialogTitle.setText("Share my card back?");

    new AlertDialog.Builder(this).setView(dialogView)
      .setPositiveButton("Sure", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int whichButton) {
          if (isOnline) {
            if (ECardUtils.isNetworkAvailable(ActivityScanned.this)) {
              // only send push immediately if online
              shareBackOnline(scannedUser.getObjId(), scannedUser.getUserId());
            } else {
              // TO-DO: disabled offline shareback because there maybe no
              // userId, so ACL of conversation can be messy
              // If no network, cache the share back request then wait till next
              // time app opens with network to send push
              // shareBackOffline(scannedUser.getObjId());
            }
          } else {
            // TO-DO: disabled offline shareback because there maybe no userId,
            // so ACL of conversation can be messy
            // Offline, but still want to share back
            // shareBackOffline(scannedUser.getObjId());
          }
          setResult(RESULT_OK);
          ActivityScanned.this.finish();
        }

      }).setNegativeButton("Nope", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int whichButton) {
          setResult(RESULT_OK);
          ActivityScanned.this.finish();
        }
      }).setCancelable(false).show();

  }

  private void shareBackOffline(String scannedId) {
    ECardSQLHelperCachedShares db = new ECardSQLHelperCachedShares(this);
    List<OfflineDataCachedShares> olDatas = db.getData("partyB", scannedId);
    if (olDatas.size() == 0) {
      // if the target Id is not among local db records, cache it
      db.addData(new OfflineDataCachedShares(currentUser.get("ecardId")
        .toString(), scannedId));
    } else {
      Toast.makeText(getBaseContext(),
        "Already in local push queue, but still cached!", Toast.LENGTH_SHORT)
        .show();
      // flip the flag, give it a chance to be revisited
      olDatas.get(0).setStored(0);
      db.updataData(olDatas.get(0));
    }
  }

  private void cacheScannedIds(String scannedId) {

    ECardSQLHelperCachedIds db = new ECardSQLHelperCachedIds(this);
    List<OfflineDataCachedIds> olDatas = db.getData("ecardID", scannedId);
    EditText whereMet = (EditText) findViewById(R.id.PlaceAdded2);
    EditText eventMet = (EditText) findViewById(R.id.EventAdded2);
    EditText notes = (EditText) findViewById(R.id.EditNotes);
    if (olDatas.size() == 0) {
      // if EcardID is not among local db records, cache it

      File file = new File(filepath);
      if (file.exists()) {
        // if there is voice note to be cached, rename it for later use
        String newFilepath = getUniqueFilename();
        File newFile = new File(newFilepath);
        file.renameTo(newFile);
        file.delete();
        db.addData(new OfflineDataCachedIds(scannedId, whereMet.getText()
          .toString(), eventMet.getText().toString(), notes.getText()
          .toString(), newFilepath));
      } else {
        // no voice note to save
        db.addData(new OfflineDataCachedIds(scannedId, whereMet.getText()
          .toString(), eventMet.getText().toString(), notes.getText()
          .toString(), "null"));
      }
      Toast.makeText(getBaseContext(),
        "Ecard cached, will add when next time connect to internet",
        Toast.LENGTH_SHORT).show();
    } else {
      Toast.makeText(getBaseContext(),
        "Already in local queue, but still cached!", Toast.LENGTH_SHORT).show();
      // flip the flag, give it a chance to be revisited
      olDatas.get(0).setStored(0);
      db.updataData(olDatas.get(0));
    }
  }

  private Location getLocation() {
    LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    List<String> providers = lm.getProviders(true);
    Location location = null;

    for (int i = providers.size() - 1; i >= 0; i--) {
      location = lm.getLastKnownLocation(providers.get(i));
      if (location != null)
        break;
    }

    if (location == null) {
      if (LocationManager.NETWORK_PROVIDER != "network") {
        lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 3600000,
          1000, onLocationChange);
      }
    }

    return location;
  }

  LocationListener onLocationChange = new LocationListener() {
    public void onLocationChanged(Location fix) {

      Log.i("onLocationChange", "Location found");
      new GeocoderHelper(delegate).fetchCityName(getBaseContext(), fix);

    }

    public void onProviderDisabled(String provider) {
      // required for interface, not used
    }

    public void onProviderEnabled(String provider) {
      // required for interface, not used
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
      // required for interface, not used
    }
  };

  @Override
  public void processFinish(String output) {
    Log.i("GeocoderHelperAdd", output);
    Toast.makeText(this, output, Toast.LENGTH_SHORT).show();
    // save the obtained cityName to global variable to be passed to
    // ActivityNotes
    whereMet = output;
    EditText whereMet2 = (EditText) findViewById(R.id.PlaceAdded2);
    if (whereMet != null) {
      whereMet2.setText(whereMet);
    }
  }

  public void shareBackOnline(final String targetEcardId, String targetUserId) {
    // Meanwhile, create a record in conversations -- so web app can check since
    // it cannot receive notification
    // need to see how to fix ACL so only both parties can access conversation
    // If the other card non-exist, then it doesn't hurt, it'll just be no one
    // will receive notifications
    ParseQuery<ParseObject> queryConv = ParseQuery.getQuery("Conversations");
    queryConv.whereEqualTo("partyA", currentUser.get("ecardId").toString());
    queryConv.whereEqualTo("partyB", targetEcardId);
    List<ParseObject> listConv = null;
    try {
      listConv = queryConv.find();
    } catch (ParseException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }
    if (listConv == null || listConv.size() == 0 || listConv.size() == 1) {
      ParseObject object = null;
      // if there is no existing notification, create one
      if(listConv == null || listConv.size() == 0){
        object = new ParseObject("Conversations");
        ParseACL myACL = new ParseACL();
        myACL.setPublicReadAccess(false);
        myACL.setPublicWriteAccess(false);
        myACL.setReadAccess(currentUser.getObjectId().toString(), true);
        myACL.setWriteAccess(currentUser.getObjectId().toString(), true);
        myACL.setReadAccess(targetUserId, true);
        myACL.setWriteAccess(targetUserId, true);
        object.setACL(myACL);
        object.put("partyA", currentUser.get("ecardId").toString());
        object.put("partyB", targetEcardId);
        object.put("read", false);
      } 
      if(listConv.size() == 1) {
        // If there is existing notification, check if it has been deleted
        object = listConv.get(0);
        if ((boolean) object.get("isDeleted") == true || (boolean) object.get("read") == true) {
          object.put("isDeleted", false);
          object.put("read", false);
        }
      }
      object.saveEventually(new SaveCallback() {

        @Override
        public void done(ParseException arg0) {
          // what if offline? so far so good... no notification, but will create
          // conversations records
          // make sure the conversation record is created before a notification
          // is sent
          // Send push to the other party according to their ecardId recorded in
          // an installation
          ParseQuery pushQuery = ParseInstallation.getQuery();
          pushQuery.whereEqualTo("ecardId", targetEcardId);
          JSONObject jsonObject = new JSONObject();
          try {
            jsonObject.put("alert",
              "This is " + ActivityMain.myselfUserInfo.getFirstName() + " "
                + ActivityMain.myselfUserInfo.getLastName() + " from "
                + ActivityMain.myselfUserInfo.getCompany());
            jsonObject.put("link", "https://www.micklestudios.com/search?id="
              + currentUser.get("ecardId").toString() + "&fn="
              + ActivityMain.myselfUserInfo.getFirstName() + "&ln="
              + ActivityMain.myselfUserInfo.getLastName());
            jsonObject.put("action", "EcardOpenConversations");
          } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
          ParsePush push = new ParsePush();
          push.setQuery(pushQuery);
          push.setData(jsonObject);
          push.sendInBackground();
        }

      });
    } else {
      // if there is existing notifications
      // mark that notification as unread no matter it's read or not
      // By construction, the list should only have 1 record
      listConv.get(0).put("read", false);
      try {
        listConv.get(0).save();
        // send push after flipping the flag
        ParseQuery pushQuery = ParseInstallation.getQuery();
        pushQuery.whereEqualTo("ecardId", targetEcardId);
        JSONObject jsonObject = new JSONObject();
        try {
          jsonObject.put("alert",
            "Hi, this is " + ActivityMain.myselfUserInfo.getFirstName() + " "
              + ActivityMain.myselfUserInfo.getLastName()
              + ", please save my card.");
          jsonObject.put("link", "https://www.micklestudios.com/search?id="
            + currentUser.get("ecardId").toString() + "&fn="
            + ActivityMain.myselfUserInfo.getFirstName() + "&ln="
            + ActivityMain.myselfUserInfo.getLastName());
          jsonObject.put("action", "EcardOpenConversations");
        } catch (JSONException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
        ParsePush push = new ParsePush();
        push.setQuery(pushQuery);
        push.setData(jsonObject);
        push.sendInBackground();
      } catch (ParseException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }
}
