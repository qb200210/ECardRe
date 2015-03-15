package com.warpspace.ecardv4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import se.emilsjolander.stickylistheaders.StickyListHeadersListView;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.nhaarman.listviewanimations.appearance.StickyListHeadersAdapterDecorator;
import com.nhaarman.listviewanimations.appearance.simple.AlphaInAnimationAdapter;
import com.nhaarman.listviewanimations.util.StickyListHeadersListViewWrapper;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.warpspace.ecardv4.infrastructure.SearchListAdapter;
import com.warpspace.ecardv4.infrastructure.UserInfo;
import com.warpspace.ecardv4.utils.AsyncTasks;
import com.warpspace.ecardv4.utils.CurvedAndTiled;
import com.warpspace.ecardv4.utils.ECardUtils;
import com.warpspace.ecardv4.utils.MySimpleListViewAdapterForSearch;

public class ActivitySearch extends ActionBarActivity {

  AlertDialog actions;
  ParseUser currentUser;
  String[] sortMethodArray = { "A-Z", "Z-A", "New-Old", "Old-New" };
  private static final long NOTES_TIMEOUT = 10000;

  static int SEARCH_MENU_OVERHANG = 275;
  static int LIST_TOP_PADDING = 200;
  static final int SCROLL_ANIMATION_SPEED_MS_SLOW = 1000;
  static final int SCROLL_ANIMATION_SPEED_MS_NORMAL = 500;
  static final int SCROLL_ANIMATION_SPEED_MS_FAST = 250;

  View mainView;
  LinearLayout searchWidget;

  public static ArrayList<UserInfo> filteredUsers;
  private static ArrayList<UserInfo> allUsers;
  static ArrayList<String> autoCompleteList;
  SearchListAdapter adapter;
  AlphaInAnimationAdapter animationAdapter;
  StickyListHeadersAdapterDecorator stickyListHeadersAdapterDecorator;
  StickyListHeadersListView listView;

  ArrayAdapter<String> autoCompleteAdapter;
  AutoCompleteTextView searchBox;

  SparseIntArray searchListToUserListMap;

  boolean droppedDown = false;

  private ArrayList<String> ecardIds = new ArrayList<String>();
  private ArrayList<String> returnedIds = new ArrayList<String>();
  List<Integer> idsNote = Arrays.asList(R.id.query_event_met,
    R.id.query_where_met);
  List<Integer> idsCardEdit = Arrays.asList(R.id.query_name,
    R.id.query_job_title);
  List<Integer> idsCardView = Arrays.asList(R.id.query_company_name,
    R.id.query_where_work);
  List<String> fieldsNote = Arrays.asList("event_met_lc", "where_met_lc");

  @SuppressLint("InflateParams")
  @Override
  protected void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // show custom action bar (on top of standard action bar)
    showActionBar();
    mainView = getLayoutInflater().inflate(R.layout.activity_search, null);
    setContentView(mainView);

    currentUser = ParseUser.getCurrentUser();

    allUsers = new ArrayList<UserInfo>();
    filteredUsers = allUsers;
    autoCompleteList = new ArrayList<String>();
    searchListToUserListMap = new SparseIntArray();

    searchWidget = (LinearLayout) findViewById(R.id.lnlayout_search_widget);

    // build dialog for sorting selection options
    buildSortDialog();

    listView = (StickyListHeadersListView) findViewById(R.id.activity_stickylistheaders_listview);
    listView.setOnItemClickListener(new OnItemClickListener() {

      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position,
        long id) {
        UserInfo selectedUser = (UserInfo) listView.getItemAtPosition(position);

        Intent intent = new Intent(getBaseContext(), ActivityDetails.class);
        // passing UserInfo is made possible through Parcelable
        intent.putExtra("userinfo", selectedUser);
        startActivity(intent);
      }
    });

    getContacts();

    handleSearchDropDown();

    searchBox = (AutoCompleteTextView) findViewById(R.id.txt_autocomplete_search);
    searchBox.setOnItemClickListener(new OnItemClickListener() {

      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position,
        long id) {
        TextView selectedView = (TextView) view;
        int index = autoCompleteList.indexOf(selectedView.getText());

        int allUsersIndex = searchListToUserListMap.get(index);
        UserInfo selectedUser = allUsers.get(allUsersIndex);

        Intent intent = new Intent(getBaseContext(), ActivityDetails.class);
        // passing UserInfo is made possible through Parcelable
        intent.putExtra("userinfo", selectedUser);
        startActivity(intent);
      }
    });
  }

  private void populateAutoComplete(AutoCompleteTextView textBox) {
    autoCompleteAdapter = new ArrayAdapter<String>(this,
      android.R.layout.simple_dropdown_item_1line, autoCompleteList);
    textBox.setAdapter(autoCompleteAdapter);
  }

  private void handleSearchDropDown() {
    // Intercept all touch events to the drop down.
    final LinearLayout sv = (LinearLayout) findViewById(R.id.lnlayout_search_menu);
    sv.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
      }
    });

    // Set the dropdown animation.
    Button buttonPullDown = (Button) findViewById(R.id.btn_pull_down);
    buttonPullDown.setOnClickListener(new OnClickListener() {
      @SuppressWarnings("deprecation")
      @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
      @Override
      public void onClick(View v) {
        LinearLayout searchWidget = (LinearLayout) findViewById(R.id.lnlayout_search_widget);
        int sdk = android.os.Build.VERSION.SDK_INT;
        if (!droppedDown) {
          droppedDown = true;
          if (sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
            v.setBackgroundDrawable(getResources().getDrawable(
              R.drawable.semi_rounded_up_empty));
          } else {
            v.setBackground(getResources().getDrawable(
              R.drawable.semi_rounded_up_empty));
          }

          sv.animate().translationY(0)
            .setDuration(SCROLL_ANIMATION_SPEED_MS_NORMAL)
            .setInterpolator(new OvershootInterpolator()).start();
        } else {
          droppedDown = false;
          if (sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
            v.setBackgroundDrawable(getResources().getDrawable(
              R.drawable.semi_rounded_down_empty));
          } else {
            v.setBackground(getResources().getDrawable(
              R.drawable.semi_rounded_down_empty));
          }
          sv.animate().translationY(SEARCH_MENU_OVERHANG - sv.getHeight())
            .setDuration(SCROLL_ANIMATION_SPEED_MS_FAST)
            .setInterpolator(new LinearInterpolator()).start();
        }
      }
    });

    // The initial animation to retract the drop down menu.
    mainView.post(new Runnable() {
      @Override
      public void run() {
        RelativeLayout rLayout = (RelativeLayout) findViewById(R.id.rllayout_search_pull_down);
        int searchWidgetTotalHeight = searchWidget.getMeasuredHeight()
          + searchWidget.getPaddingTop();
        SEARCH_MENU_OVERHANG = searchWidgetTotalHeight
          + rLayout.getMeasuredHeight();
        listView.setPadding(0, searchWidgetTotalHeight, 0, 0);
        sv.animate()
          .translationY(SEARCH_MENU_OVERHANG - sv.getMeasuredHeight())
          .setDuration(SCROLL_ANIMATION_SPEED_MS_SLOW)
          .setInterpolator(new LinearInterpolator()).start();
      }
    });

    // Handle the search queries.
    OnClickListener filterTouchListener = new OnClickListener() {
      @Override
      public void onClick(View v) {
        TextView tv = (TextView) v;
        Toast.makeText(getApplicationContext(), tv.getText(),
          Toast.LENGTH_SHORT).show();
      }
    };

    TextView tvWhereMet = (TextView) findViewById(R.id.txt_where_met);
    tvWhereMet.setOnClickListener(filterTouchListener);

  }

  private void getContacts() {
    ecardIds.clear();
    returnedIds.clear();

    /* A map of all the ECardNote objects to the noteID */
    final HashMap<String, ParseObject> noteIdToNoteObjectMap = new HashMap<String, ParseObject>();

    ParseQuery<ParseObject> queryNotes = ParseQuery.getQuery("ECardNote");
    queryNotes.fromLocalDatastore();
    queryNotes.whereEqualTo("userId", currentUser.getObjectId());

    queryNotes.findInBackground(new FindCallback<ParseObject>() {
      @Override
      public void done(List<ParseObject> objectsNoteList,
        ParseException noteException) {
        if (noteException == null) {
          if (objectsNoteList.size() != 0) {
            // Got a list of all the notes. Now collect all the noteIDs.
            for (Iterator<ParseObject> iter = objectsNoteList.iterator(); iter
              .hasNext();) {
              ParseObject objectNote = iter.next();
              String infoObjectId = (String) objectNote.get("ecardId");

              // Add these values to the map.
              noteIdToNoteObjectMap.put(infoObjectId, objectNote);
            }

            /*
             * Now, query the ECardInfoTable to get all the ECardInfo for the
             * notes collected here.
             */
            ParseQuery<ParseObject> queryInfo = ParseQuery
              .getQuery("ECardInfo");
            queryInfo.fromLocalDatastore();
            queryInfo.whereContainedIn("objectId",
              noteIdToNoteObjectMap.keySet());

            queryInfo.findInBackground(new FindCallback<ParseObject>() {

              @Override
              public void done(List<ParseObject> objectInfoList,
                ParseException infoException) {
                // Now we have a list of ECardInfo objects. Populate the
                // userInfo list.
                if (infoException == null) {
                  // Iterate over the list.
                  for (Iterator<ParseObject> iter = objectInfoList.iterator(); iter
                    .hasNext();) {
                    ParseObject objectInfo = iter.next();
                    UserInfo contact = new UserInfo(objectInfo);
                    if (contact != null) {
                      // Contact has been created. Populate the "createdAt" from
                      // the note object.
                      String infoObjectId = (String) objectInfo.getObjectId();
                      ParseObject objectNote = noteIdToNoteObjectMap
                        .get(infoObjectId);
                      contact.setCreatedAt(objectNote.getCreatedAt());

                      allUsers.add(contact);

                      // Add the positions to the map to retrieve the search
                      // results.
                      int userIndex = allUsers.size() - 1;

                      // Add first names.
                      if (contact.getFirstName() != "") {
                        autoCompleteList.add(contact.getFirstName());
                        searchListToUserListMap.append(
                          autoCompleteList.size() - 1, userIndex);
                      }

                      // Add last names.
                      if (contact.getLastName() != "") {
                        autoCompleteList.add(contact.getLastName());
                        searchListToUserListMap.append(
                          autoCompleteList.size() - 1, userIndex);
                      }

                      // Add Company.
                      if (contact.getCompany() != "") {
                        autoCompleteList.add(contact.getCompany());
                        searchListToUserListMap.append(
                          autoCompleteList.size() - 1, userIndex);
                      }

                      // Add City.
                      if (contact.getCity() != "") {
                        autoCompleteList.add(contact.getCity());
                        searchListToUserListMap.append(
                          autoCompleteList.size() - 1, userIndex);
                      }

                      // Add Title.
                      if (contact.getTitle() != "") {
                        autoCompleteList.add(contact.getTitle());
                        searchListToUserListMap.append(
                          autoCompleteList.size() - 1, userIndex);
                      }
                    }
                  }

                  filteredUsers = new ArrayList<UserInfo>(allUsers);
                  adapter = new SearchListAdapter(getApplicationContext(),
                    filteredUsers);
                  animationAdapter = new AlphaInAnimationAdapter(adapter);
                  stickyListHeadersAdapterDecorator = new StickyListHeadersAdapterDecorator(
                    animationAdapter);
                  stickyListHeadersAdapterDecorator
                    .setListViewWrapper(new StickyListHeadersListViewWrapper(
                      listView));

                  assert animationAdapter.getViewAnimator() != null;
                  animationAdapter.getViewAnimator().setInitialDelayMillis(500);

                  assert stickyListHeadersAdapterDecorator.getViewAnimator() != null;
                  stickyListHeadersAdapterDecorator.getViewAnimator()
                    .setInitialDelayMillis(500);

                  listView.setAdapter(stickyListHeadersAdapterDecorator);
                  adapter.reSortName(true);
                  stickyListHeadersAdapterDecorator.notifyDataSetChanged();

                  // Also update the Autocomplete search box.
                  populateAutoComplete(searchBox);
                  autoCompleteAdapter.notifyDataSetChanged();
                }
              }
            });
          }
        } else {
          Toast.makeText(getBaseContext(), "General parse error!",
            Toast.LENGTH_SHORT).show();
        }
      }
    });
  }

  @SuppressLint("InflateParams")
  private void showActionBar() {
    LayoutInflater inflator = (LayoutInflater) this
      .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    View v = inflator.inflate(R.layout.layout_actionbar_search, null);
    ImageView btnBack = (ImageView) v.findViewById(R.id.btn_back);
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

  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.search_actionbar, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // this function is called when either action bar icon is tapped
    switch (item.getItemId()) {
    case R.id.sort_results:
      actions.show();
      return true;
    case R.id.download_cards:
      if (ECardUtils.isNetworkAvailable(this)) {
        // upon opening, pin online notes to local
        SharedPreferences prefs = getSharedPreferences(
          ActivityBufferOpening.MY_PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor prefEditor = prefs.edit();
        final AsyncTasks.SyncDataTaskNotes syncNotes = new AsyncTasks.SyncDataTaskNotes(
          this, currentUser, prefs, prefEditor);
        syncNotes.execute();
        Handler handlerNotes = new Handler();
        handlerNotes.postDelayed(new Runnable() {

          @Override
          public void run() {
            if (syncNotes.getStatus() == AsyncTask.Status.RUNNING) {
              Toast.makeText(getApplicationContext(), "Sync Notes Timed Out",
                Toast.LENGTH_SHORT).show();
              syncNotes.cancel(true);
              adapter.reSortDate(true);
            }
          }
        }, NOTES_TIMEOUT);
      } else {
        Toast.makeText(getApplicationContext(), "Network unavailable",
          Toast.LENGTH_SHORT).show();
      }
      return true;
    case R.id.log_out:
      ParseUser.logOut();
      Intent intentLogin = new Intent(this, ActivityPreLogin.class);
      startActivity(intentLogin);
      this.finish();
      return true;
    default:
      return super.onOptionsItemSelected(item);
    }
  }

  @SuppressLint({ "NewApi", "InflateParams" })
  private void buildSortDialog() {
    // Get the layout inflater
    LayoutInflater inflater = getLayoutInflater();
    View dialogAddMoreView = inflater.inflate(R.layout.layout_dialog_addmore,
      null);
    LinearLayout dialogHeader = (LinearLayout) dialogAddMoreView
      .findViewById(R.id.dialog_header);
    TextView dialogTitle = (TextView) dialogAddMoreView
      .findViewById(R.id.dialog_title);
    // Set dialog header background with rounded corner
    Bitmap bm = BitmapFactory
      .decodeResource(getResources(), R.drawable.striped);
    BitmapDrawable bmDrawable = new BitmapDrawable(getResources(), bm);
    dialogHeader.setBackground(new CurvedAndTiled(bmDrawable.getBitmap(), 5));
    // Set dialog title and main EditText
    dialogTitle.setText("Sort Method");

    AlertDialog.Builder builder = new AlertDialog.Builder(ActivitySearch.this);
    builder.setView(dialogAddMoreView);
    // actions now links to the dialog
    actions = builder.create();

    // Below is to build the listener for items listed inside the poped up
    // "addmorebutton dialog"
    ListView listViewInDialog = (ListView) dialogAddMoreView
      .findViewById(R.id.dialog_listview);
    listViewInDialog.setAdapter(new MySimpleListViewAdapterForSearch(
      ActivitySearch.this, sortMethodArray));
    listViewInDialog.setOnItemClickListener(new OnItemClickListener() {

      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position,
        long id) {
        switch (position) {
        case (0):
          adapter.reSortName(true);
          actions.dismiss();
          break;
        case (1):
          adapter.reSortName(false);
          actions.dismiss();
          break;
        case (2):
          adapter.reSortDate(false);
          actions.dismiss();
          break;
        case (3):
          adapter.reSortDate(true);
          actions.dismiss();
          break;
        default:
          Toast.makeText(getApplicationContext(), "Placeholder: Default",
            Toast.LENGTH_SHORT).show();
          actions.dismiss();
        }
        adapter.notifyDataSetChanged();
      }

    });
  }
}
