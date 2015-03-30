package com.warpspace.ecardv4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

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
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
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
import com.warpspace.ecardv4.infrastructure.UserIndexStringType;
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
  Button searchButton;
  LinearLayout sv;

  TextView filterTextWhereMet;
  TextView filterTextEventMet;
  TextView filterTextCity;
  TextView filterTextName;
  TextView filterTextCompany;
  TextView filterTextNotes;

  Button btnClearFilter;

  boolean filterSelected = false;

  public static ArrayList<UserInfo> filteredUsers;
  public static ArrayList<UserInfo> allUsers;
  static ArrayList<String> autoCompleteList;
  SearchListAdapter adapter;
  AlphaInAnimationAdapter animationAdapter;
  StickyListHeadersAdapterDecorator stickyListHeadersAdapterDecorator;
  StickyListHeadersListView listView;
  LinearLayout layoutNoResults;
  RelativeLayout searchPullDown;
  LinearLayout searchFilterWidget;

  RelativeLayout rLayoutName;
  RelativeLayout rLayoutCity;
  RelativeLayout rLayoutNotes;
  RelativeLayout rLayoutEventMet;
  RelativeLayout rLayoutWhereMet;
  RelativeLayout rLayoutCompany;

  Button btnPullDown;

  private int searchMenuRetractedHeight = 0;

  // Possible modes of sorting.
  public static final int SORT_MODE_NAME_ASC = 1;
  public static final int SORT_MODE_NAME_DSC = 2;
  public static final int SORT_MODE_DATE_ASC = 3;
  public static final int SORT_MODE_DATE_DSC = 4;

  public static int currentSortMode = SORT_MODE_DATE_ASC;

  // Possible modes of searching.
  public static int SEARCH_MODE_ALL = -1;
  public static int currentSearchMode = SEARCH_MODE_ALL;

  ArrayAdapter<String> autoCompleteAdapter;
  HashMap<String, ArrayList<UserIndexStringType>> searchEntries;
  AutoCompleteTextView searchBox;

  SparseIntArray searchListToUserListMap;

  boolean droppedDown = false;

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

    searchEntries = new HashMap<String, ArrayList<UserIndexStringType>>();

    // show custom action bar (on top of standard action bar)
    showActionBar();
    mainView = getLayoutInflater().inflate(R.layout.activity_search, null);
    setContentView(mainView);

    currentUser = ParseUser.getCurrentUser();

    filteredUsers = new ArrayList<UserInfo>();
    filteredUsers.addAll(allUsers);
    autoCompleteList = new ArrayList<String>();
    searchListToUserListMap = new SparseIntArray();

    searchWidget = (LinearLayout) findViewById(R.id.lnlayout_search_widget);
    layoutNoResults = (LinearLayout) findViewById(R.id.lnlayout_no_results);
    layoutNoResults.setVisibility(View.INVISIBLE);

    getWindow().setSoftInputMode(
      WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

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

    handleSearchDropDown();

    searchBox = (AutoCompleteTextView) findViewById(R.id.txt_autocomplete_search);
    searchBox.setOnEditorActionListener(new TextView.OnEditorActionListener() {

      @Override
      public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
          performSearch();
          return true;
        }
        return false;
      }
    });

    searchButton = (Button) findViewById(R.id.btn_search_inside);
    searchButton.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {
        performSearch();
      }
    });

    // Retrieve all the filters and set the onclick listener
    FilterSelectedListener filterSelectedListener = new FilterSelectedListener();
    filterTextName = (TextView) findViewById(R.id.txt_name);
    filterTextCity = (TextView) findViewById(R.id.txt_city);
    filterTextWhereMet = (TextView) findViewById(R.id.txt_where_met);
    filterTextEventMet = (TextView) findViewById(R.id.txt_event_met);
    filterTextCompany = (TextView) findViewById(R.id.txt_company);
    filterTextNotes = (TextView) findViewById(R.id.txt_notes);

    filterTextName.setOnClickListener(filterSelectedListener);
    filterTextCity.setOnClickListener(filterSelectedListener);
    filterTextWhereMet.setOnClickListener(filterSelectedListener);
    filterTextEventMet.setOnClickListener(filterSelectedListener);
    filterTextCompany.setOnClickListener(filterSelectedListener);
    filterTextNotes.setOnClickListener(filterSelectedListener);

    rLayoutName = (RelativeLayout) findViewById(R.id.rlayout_name);
    rLayoutCompany = (RelativeLayout) findViewById(R.id.rlayout_company);
    rLayoutWhereMet = (RelativeLayout) findViewById(R.id.rlayout_where_met);
    rLayoutEventMet = (RelativeLayout) findViewById(R.id.rlayout_event_met);
    rLayoutCity = (RelativeLayout) findViewById(R.id.rlayout_city);
    rLayoutNotes = (RelativeLayout) findViewById(R.id.rlayout_notes);

    toggleFiltersVisibility(false);

    btnClearFilter = (Button) findViewById(R.id.btn_clear_filter);
    btnClearFilter.setOnClickListener(new OnClickListener() {

      @SuppressWarnings("deprecation")
      @SuppressLint("NewApi")
      @Override
      public void onClick(View v) {
        currentSearchMode = SEARCH_MODE_ALL;
        v.setVisibility(View.GONE);
        int sdk = android.os.Build.VERSION.SDK_INT;
        if (sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
          searchButton.setBackgroundDrawable(getResources().getDrawable(
            R.drawable.ic_search_blue));
        } else {
          searchButton.setBackground(getResources().getDrawable(
            R.drawable.ic_search_blue));
        }

        performSearch();
      }
    });

    btnClearFilter.setVisibility(View.GONE);
  }

  // Hide all the filter text views
  private void toggleFiltersVisibility(boolean show) {
    int visibility = show ? View.VISIBLE : View.GONE;

    rLayoutName.setVisibility(visibility);
    rLayoutCity.setVisibility(visibility);
    rLayoutWhereMet.setVisibility(visibility);
    rLayoutEventMet.setVisibility(visibility);
    rLayoutCompany.setVisibility(visibility);
    rLayoutNotes.setVisibility(visibility);
  }

  // Create a new onClickListener to be used for all the search filters
  class FilterSelectedListener implements OnClickListener {
    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    @Override
    public void onClick(View view) {
      TextView v = (TextView) view;
      int bgDrawable = 0;

      String hintText = null;
      if (v == filterTextName) {
        currentSearchMode = UserInfo.FIELD_TYPE.TYPE_FNAME;
        bgDrawable = R.drawable.ic_filter_person;
        hintText = "Search Names ...";
      } else if (v == filterTextCity) {
        currentSearchMode = UserInfo.FIELD_TYPE.TYPE_CITY;
        bgDrawable = R.drawable.ic_filter_city;
        hintText = "Search City ...";
      } else if (v == filterTextWhereMet) {
        currentSearchMode = UserInfo.FIELD_TYPE.TYPE_WHERE_MET;
        bgDrawable = R.drawable.ic_filter_place;
        hintText = "Search where met ...";
      } else if (v == filterTextEventMet) {
        currentSearchMode = UserInfo.FIELD_TYPE.TYPE_EVENT_MET;
        bgDrawable = R.drawable.ic_filter_event;
        hintText = "Search event met ...";
      } else if (v == filterTextCompany) {
        currentSearchMode = UserInfo.FIELD_TYPE.TYPE_COMPANY;
        bgDrawable = R.drawable.ic_filter_company;
        hintText = "Search company ...";
      } else if (v == filterTextNotes) {
        currentSearchMode = UserInfo.FIELD_TYPE.TYPE_NOTES;
        bgDrawable = R.drawable.ic_filter_notes;
        hintText = "Search notes ...";
      }

      int sdk = android.os.Build.VERSION.SDK_INT;
      if (sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
        searchButton.setBackgroundDrawable(getResources().getDrawable(
          bgDrawable));
      } else {
        searchButton.setBackground(getResources().getDrawable(bgDrawable));
      }

      moveSearchMenu();

      performSearch();

      searchBox.setHint(hintText);

      btnClearFilter.setVisibility(View.VISIBLE);
    }
  }

  private void performSearch() {
    String key = searchBox.getText().toString();
    filteredUsers.clear();
    if (key.matches("")) {
      filteredUsers.addAll(allUsers);
    } else {
      String keyLower = key.toLowerCase(Locale.ENGLISH);
      ArrayList<UserIndexStringType> searchHits = (ArrayList<UserIndexStringType>) searchEntries
        .get(keyLower);
      if (searchHits != null) {
        for (UserIndexStringType result : searchHits) {
          if (currentSearchMode == SEARCH_MODE_ALL
            || currentSearchMode == result.type) {
            filteredUsers.add(allUsers.get(result.userIndex));
          }
        }
      }

      if (currentSearchMode == SEARCH_MODE_ALL
        || currentSearchMode == UserInfo.FIELD_TYPE.TYPE_NOTES) {
        for (UserInfo info : allUsers) {
          if (info.getNotes().toLowerCase(Locale.ENGLISH).contains(keyLower)) {
            filteredUsers.add(info);
          }
        }
      }
    }

    adapter.reSort();
    adapter.refreshData(filteredUsers);
    adapter.notifyDataSetChanged();
    listView.invalidateViews();

    if (filteredUsers.size() == 0) {
      layoutNoResults.setVisibility(View.VISIBLE);
    } else {
      layoutNoResults.setVisibility(View.INVISIBLE);
    }
  }

  private void populateAutoComplete(AutoCompleteTextView textBox) {
    autoCompleteAdapter = new ArrayAdapter<String>(this,
      android.R.layout.simple_dropdown_item_1line, autoCompleteList);
    textBox.setAdapter(autoCompleteAdapter);
  }

  // Drop down the search Menu
  @SuppressLint("NewApi")
  @SuppressWarnings("deprecation")
  private void moveSearchMenu() {
    int sdk = android.os.Build.VERSION.SDK_INT;
    if (!droppedDown) {
      // Drop the shade down.
      // First enable the filters.
      toggleFiltersVisibility(true);

      droppedDown = true;
      if (sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
        btnPullDown.setBackgroundDrawable(getResources().getDrawable(
          R.drawable.semi_rounded_up_empty));
      } else {
        btnPullDown.setBackground(getResources().getDrawable(
          R.drawable.semi_rounded_up_empty));
      }

      sv.animate().translationY(0)
        .setDuration(SCROLL_ANIMATION_SPEED_MS_NORMAL)
        .setInterpolator(new OvershootInterpolator()).start();

      // Hide the keyboard
      InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
      imm.hideSoftInputFromWindow(btnPullDown.getWindowToken(), 0);

    } else {
      droppedDown = false;
      if (sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
        btnPullDown.setBackgroundDrawable(getResources().getDrawable(
          R.drawable.semi_rounded_down_empty));
      } else {
        btnPullDown.setBackground(getResources().getDrawable(
          R.drawable.semi_rounded_down_empty));
      }
      sv.animate().translationY(searchMenuRetractedHeight)
        .setDuration(SCROLL_ANIMATION_SPEED_MS_FAST)
        .setInterpolator(new LinearInterpolator()).start();
      toggleFiltersVisibility(false);
    }
  }

  private void handleSearchDropDown() {
    // Intercept all touch events to the drop down.
    sv = (LinearLayout) findViewById(R.id.lnlayout_search_menu);
    sv.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
      }
    });

    // Set the dropdown animation.
    btnPullDown = (Button) findViewById(R.id.btn_pull_down);
    btnPullDown.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        moveSearchMenu();
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
        searchMenuRetractedHeight = SEARCH_MENU_OVERHANG
          - sv.getMeasuredHeight();
        sv.setTranslationY(searchMenuRetractedHeight);
      }
    });

    // Handle the search queries.
    OnClickListener filterTouchListener = new OnClickListener() {
      @Override
      public void onClick(View v) {
      }
    };

    TextView tvWhereMet = (TextView) findViewById(R.id.txt_where_met);
    tvWhereMet.setOnClickListener(filterTouchListener);

  }

  class SearchLayoutRunnable implements Runnable {
    @Override
    public void run() {
      searchPullDown = (RelativeLayout) findViewById(R.id.rllayout_search_pull_down);
      int searchWidgetTotalHeight = searchWidget.getMeasuredHeight()
        + searchWidget.getPaddingTop();
      int filterWidgetTotalHeight = searchFilterWidget.getMeasuredHeight();
      if (filterWidgetTotalHeight != 0) {
        filterWidgetTotalHeight += searchFilterWidget.getPaddingBottom()
          + searchFilterWidget.getPaddingTop();
      }
      SEARCH_MENU_OVERHANG = searchWidgetTotalHeight + filterWidgetTotalHeight
        + +searchPullDown.getMeasuredHeight();
      listView.setPadding(0, searchWidgetTotalHeight + filterWidgetTotalHeight,
        0, 0);
      sv.setTranslationY(SEARCH_MENU_OVERHANG - sv.getMeasuredHeight());
    }
  }

  // Add each field of the UserInfo to the search map.
  private void addFieldToSearchStructure(String key, UserIndexStringType field) {
    ArrayList<UserIndexStringType> fieldList = searchEntries.get(key
      .toLowerCase(Locale.ENGLISH));
    if (fieldList == null) {
      fieldList = new ArrayList<UserIndexStringType>();
      // Add all the strings to the autocomplete list
      autoCompleteList.add(key);
      searchEntries.put(key.toLowerCase(Locale.ENGLISH), fieldList);
    }

    fieldList.add(field);
  }

  // Add the UserInfo type object to the search map.
  private void addToSearchStructures(UserInfo contact, int userIndex) {
    // Add first names.
    if (contact.getFirstName() != "") {
      // Add this field to search map.
      addFieldToSearchStructure(contact.getFirstName(),
        new UserIndexStringType(userIndex, UserInfo.FIELD_TYPE.TYPE_FNAME));
    }

    // Add last names.
    if (contact.getLastName() != "") {
      // Add this field to search map.
      addFieldToSearchStructure(contact.getLastName(), new UserIndexStringType(
        userIndex, UserInfo.FIELD_TYPE.TYPE_LNAME));
    }

    // Add Company.
    if (contact.getCompany() != "") {
      // Add this field to search map.
      addFieldToSearchStructure(contact.getCompany(), new UserIndexStringType(
        userIndex, UserInfo.FIELD_TYPE.TYPE_COMPANY));
    }

    // Add City.
    if (contact.getCity() != "") {
      // Add this field to search map.
      addFieldToSearchStructure(contact.getCity(), new UserIndexStringType(
        userIndex, UserInfo.FIELD_TYPE.TYPE_CITY));
    }

    // Add Title.
    if (contact.getTitle() != "") {
      // Add this field to search map.
      addFieldToSearchStructure(contact.getTitle(), new UserIndexStringType(
        userIndex, UserInfo.FIELD_TYPE.TYPE_TITLE));
    }

    if (contact.getWhereMet() != "") {
      // Add this field to search map.
      addFieldToSearchStructure(contact.getWhereMet(), new UserIndexStringType(
        userIndex, UserInfo.FIELD_TYPE.TYPE_WHERE_MET));
    }

    if (contact.getEventMet() != "") {
      // Add this field to search map.
      addFieldToSearchStructure(contact.getEventMet(), new UserIndexStringType(
        userIndex, UserInfo.FIELD_TYPE.TYPE_EVENT_MET));
    }
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
              currentSortMode = SORT_MODE_DATE_ASC;
              adapter.reSort();
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
          currentSortMode = SORT_MODE_NAME_ASC;
          break;
        case (1):
          currentSortMode = SORT_MODE_NAME_DSC;
          break;
        case (2):
          currentSortMode = SORT_MODE_DATE_DSC;
          break;
        case (3):
          currentSortMode = SORT_MODE_DATE_ASC;
          break;
        default:
          Toast.makeText(getApplicationContext(), "Placeholder: Default",
            Toast.LENGTH_SHORT).show();
          actions.dismiss();
          return;
        }
        adapter.reSort();
        actions.dismiss();
        adapter.notifyDataSetChanged();
      }

    });
  }
}
