package com.micklestudios.knowell;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import se.emilsjolander.stickylistheaders.StickyListHeadersListView;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
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
import android.view.View.OnLongClickListener;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.micklestudios.knowell.infrastructure.SearchListAdapter;
import com.micklestudios.knowell.infrastructure.UserInfo;
import com.micklestudios.knowell.utils.AsyncTasks;
import com.micklestudios.knowell.utils.CurvedAndTiled;
import com.micklestudios.knowell.utils.ECardUtils;
import com.micklestudios.knowell.utils.MySimpleListViewAdapterForSearch;
import com.nhaarman.listviewanimations.appearance.StickyListHeadersAdapterDecorator;
import com.nhaarman.listviewanimations.appearance.simple.AlphaInAnimationAdapter;
import com.nhaarman.listviewanimations.util.StickyListHeadersListViewWrapper;
import com.parse.ParseUser;

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
  LinearLayout searchPanel;

  AutoCompleteTextView filterTextWhereMet;
  AutoCompleteTextView filterTextEventMet;
  AutoCompleteTextView filterTextCompany;

  // List of users filtered after search.
  public static ArrayList<UserInfo> filteredUsers;

  // List of all the users in the system.
  public static ArrayList<UserInfo> allUsers;

  // List of users selected by the user.
  public static HashSet<UserInfo> selectedUsers;
  static ArrayList<String> autoCompleteList;
  SearchListAdapter adapter;
  AlphaInAnimationAdapter animationAdapter;
  StickyListHeadersAdapterDecorator stickyListHeadersAdapterDecorator;
  StickyListHeadersListView listView;
  LinearLayout layoutNoResults;
  RelativeLayout searchPullDown;
  LinearLayout searchFilterWidget;

  public static ArrayList<String> autoCompleteListAll;
  public static ArrayList<String> autoCompleteListCompany;
  public static ArrayList<String> autoCompleteListWhere;
  public static ArrayList<String> autoCompleteListEvent;

  LinearLayout lLayoutEventMet;
  LinearLayout lLayoutWhereMet;
  LinearLayout lLayoutCompany;

  private int searchMenuRetractedHeight = 0;

  // Possible modes of sorting.
  public static final int SORT_MODE_NAME_ASC = 1;
  public static final int SORT_MODE_NAME_DSC = 2;
  public static final int SORT_MODE_DATE_ASC = 3;
  public static final int SORT_MODE_DATE_DSC = 4;

  public static int currentSortMode = SORT_MODE_DATE_ASC;

  // Whether in selection mode.
  public static boolean isSelectionMode = false;

  ArrayAdapter<String> autoCompleteAdapter;
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
    // Inflate the layout.
    super.onCreate(savedInstanceState);
    mainView = getLayoutInflater().inflate(R.layout.activity_search, null);
    setContentView(mainView);

    // Get all the views from the layout.
    retrieveAllViews();

    // show custom action bar (on top of standard action bar)
    showActionBar();

    currentUser = ParseUser.getCurrentUser();

    filteredUsers = new ArrayList<UserInfo>();
    selectedUsers = new HashSet<UserInfo>();
    autoCompleteList = new ArrayList<String>();
    searchListToUserListMap = new SparseIntArray();

    layoutNoResults.setVisibility(View.INVISIBLE);

    getWindow().setSoftInputMode(
      WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

    // build dialog for sorting selection options
    buildSortDialog();

    // Initialize the search panel views.
    initializeSearchPanel();

    // Initialize the contact list.
    initializeContactList();

    toggleFiltersVisibility(false);

    // Do some measurements once the Activity is loaded
    mainView.post(new Runnable() {
      @Override
      public void run() {
        int searchWidgetTotalHeight = searchWidget.getMeasuredHeight()
          + searchWidget.getPaddingTop();
        SEARCH_MENU_OVERHANG = searchWidgetTotalHeight;
        listView.setPadding(0, searchWidgetTotalHeight, 0, 0);
        searchMenuRetractedHeight = 0 - searchPanel.getMeasuredHeight();
        searchPanel.setTranslationY(searchMenuRetractedHeight);
      }
    });

    // Finally, load the contacts.
    performSearch();
  }

  private void retrieveAllViews() {
    // Retrieve all the filters and layouts.
    filterTextWhereMet = (AutoCompleteTextView) findViewById(R.id.txt_where_met);
    filterTextEventMet = (AutoCompleteTextView) findViewById(R.id.txt_event_met);
    filterTextCompany = (AutoCompleteTextView) findViewById(R.id.txt_company);

    // The layouts we need to hide when drop down goes up.
    lLayoutCompany = (LinearLayout) findViewById(R.id.llayout_company);
    lLayoutWhereMet = (LinearLayout) findViewById(R.id.llayout_where_met);
    lLayoutEventMet = (LinearLayout) findViewById(R.id.llayout_event_met);

    // The three frames in the layout.
    searchPanel = (LinearLayout) findViewById(R.id.lnlayout_search_menu);
    layoutNoResults = (LinearLayout) findViewById(R.id.lnlayout_no_results);
    listView = (StickyListHeadersListView) findViewById(R.id.activity_stickylistheaders_listview);

    searchWidget = (LinearLayout) findViewById(R.id.lnlayout_search_widget);
    searchBox = (AutoCompleteTextView) findViewById(R.id.txt_autocomplete_search);
    searchButton = (Button) findViewById(R.id.btn_search_inside);
  }

  private void showSelectionMenu() {

  }

  private void initializeContactList() {
    listView.setOnItemClickListener(new OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position,
        long id) {
        UserInfo clickedUser = (UserInfo) listView.getItemAtPosition(position);

        if (isSelectionMode == false) {
          // We are simply browsing through the contacts.
          Intent intent = new Intent(getBaseContext(), ActivityDetails.class);
          // passing UserInfo is made possible through Parcelable
          intent.putExtra("userinfo", clickedUser);
          startActivity(intent);
        } else {
          // Now we are selecting stuff. Get the check box
          CheckBox selectionBox = (CheckBox) view
            .findViewById(R.id.chk_contact_select);

          // If this is already selected, remove it.
          if (selectedUsers.contains(clickedUser)) {
            selectedUsers.remove(clickedUser);
            selectionBox.setSelected(false);
            // If nothing is left selected, stop selection mode.
            if (selectedUsers.size() == 0) {
              isSelectionMode = false;
              searchBox.setHint("Start Searching ...");
            }
          } else {
            selectedUsers.add(clickedUser);
            selectionBox.setSelected(true);
          }

          adapter.notifyDataSetChanged();
        }
      }
    });

    listView.setOnItemLongClickListener(new OnItemLongClickListener() {
      @Override
      public boolean onItemLongClick(AdapterView<?> parent, View view,
        int position, long id) {
        if (isSelectionMode == false) {
          isSelectionMode = true;
          searchBox.setHint("SELECTION MODE");
        }
        return false;
      }
    });

    adapter = new SearchListAdapter(getApplicationContext(), filteredUsers);
    animationAdapter = new AlphaInAnimationAdapter(adapter);
    stickyListHeadersAdapterDecorator = new StickyListHeadersAdapterDecorator(
      animationAdapter);
    stickyListHeadersAdapterDecorator
      .setListViewWrapper(new StickyListHeadersListViewWrapper(listView));

    assert animationAdapter.getViewAnimator() != null;
    animationAdapter.getViewAnimator().setInitialDelayMillis(500);

    assert stickyListHeadersAdapterDecorator.getViewAnimator() != null;
    stickyListHeadersAdapterDecorator.getViewAnimator().setInitialDelayMillis(
      500);

    listView.setAdapter(stickyListHeadersAdapterDecorator);
    currentSortMode = SORT_MODE_NAME_ASC;
    adapter.reSort();
    stickyListHeadersAdapterDecorator.notifyDataSetChanged();
  }

  private void initializeSearchPanel() {
    // Intercept all touch events to the drop down.
    searchPanel.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
      }
    });

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

    searchBox.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        moveSearchMenuUp();
      }
    });

    searchButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        moveSearchMenuUp();

        AlphaAnimation anim = new AlphaAnimation(1.0f, 0.0f);
        anim.setDuration(250);
        anim.setAnimationListener(new AnimationListener() {
          @Override
          public void onAnimationStart(Animation animation) {
          }

          @Override
          public void onAnimationRepeat(Animation animation) {
          }

          @Override
          public void onAnimationEnd(Animation animation) {
            performSearch();
            AlphaAnimation animRev = new AlphaAnimation(0.0f, 1.0f);
            animRev.setDuration(250);
            listView.startAnimation(animRev);
          }
        });
        listView.startAnimation(anim);

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
      }
    });

    // Set the autocomplete lists.
    ArrayAdapter<String> adapterCompany = new ArrayAdapter<String>(this,
      android.R.layout.select_dialog_item, autoCompleteListCompany);
    filterTextCompany.setAdapter(adapterCompany);

    ArrayAdapter<String> adapterWhere = new ArrayAdapter<String>(this,
      android.R.layout.select_dialog_item, autoCompleteListWhere);
    filterTextWhereMet.setAdapter(adapterWhere);

    ArrayAdapter<String> adapterEvent = new ArrayAdapter<String>(this,
      android.R.layout.select_dialog_item, autoCompleteListEvent);
    filterTextEventMet.setAdapter(adapterEvent);

    ArrayAdapter<String> adapterAll = new ArrayAdapter<String>(this,
      android.R.layout.select_dialog_item, autoCompleteListAll);
    searchBox.setAdapter(adapterAll);
  }

  // Hide all the filter text views
  private void toggleFiltersVisibility(boolean show) {
    int visibility = show ? View.VISIBLE : View.GONE;

    lLayoutWhereMet.setVisibility(visibility);
    lLayoutEventMet.setVisibility(visibility);
    lLayoutCompany.setVisibility(visibility);
  }

  private void performSearch() {
    filteredUsers.clear();

    // Create a temporary list so that we can iterate over one of them.
    ArrayList<UserInfo> tempUserInfoList = new ArrayList<UserInfo>();

    /*
     * First, let's go through all the filters. Let's assume that all the users
     * will be selected.
     */
    filteredUsers.addAll(allUsers);
    {
      // Start with the Where Met filter.
      String filterKey = filterTextWhereMet.getText().toString()
        .toLowerCase(Locale.ENGLISH);
      if (filterKey != "") {
        for (UserInfo uInfo : filteredUsers) {
          if (uInfo.getWhereMet().toLowerCase(Locale.ENGLISH)
            .contains(filterKey)) {
            tempUserInfoList.add(uInfo);
          }
        }
        filteredUsers.clear();
        filteredUsers.addAll(tempUserInfoList);
        tempUserInfoList.clear();
      }

      // Then look at the Company filter.
      filterKey = filterTextCompany.getText().toString()
        .toLowerCase(Locale.ENGLISH);
      if (filterKey != "") {
        for (UserInfo uInfo : filteredUsers) {
          if (uInfo.getCompany().toLowerCase(Locale.ENGLISH)
            .contains(filterKey)) {
            tempUserInfoList.add(uInfo);
          }
        }
        filteredUsers.clear();
        filteredUsers.addAll(tempUserInfoList);
        tempUserInfoList.clear();
      }

      // Then look at the Event Met filter.
      filterKey = filterTextEventMet.getText().toString()
        .toLowerCase(Locale.ENGLISH);
      if (filterKey != "") {
        for (UserInfo uInfo : filteredUsers) {
          if (uInfo.getEventMet().toLowerCase(Locale.ENGLISH)
            .contains(filterKey)) {
            tempUserInfoList.add(uInfo);
          }
        }
        filteredUsers.clear();
        filteredUsers.addAll(tempUserInfoList);
        tempUserInfoList.clear();
      }
    }

    /*
     * Now that we have filtered the users, let's look at the actual search
     * keywords.
     */
    String keyWords = searchBox.getText().toString();
    // Modified by Jian 04/12
    StringTokenizer keyWordsTokens = new StringTokenizer(keyWords);
    while (keyWordsTokens.hasMoreTokens()) {
      String token = keyWordsTokens.nextToken().toLowerCase(Locale.ENGLISH);
      String regex_str = ".*";
      for (char c : token.toCharArray()) {
        regex_str = regex_str + c + ".*";
      }
      Pattern pattern = Pattern.compile(regex_str);
      for (UserInfo uInfo : filteredUsers) {
        String user_str = uInfo.getFirstName().toLowerCase(Locale.ENGLISH)
          + " " + uInfo.getLastName().toLowerCase(Locale.ENGLISH) + " "
          + uInfo.getCompany().toLowerCase(Locale.ENGLISH);
        // Log.v("search_user_str", user_str);
        Matcher matcher = pattern.matcher(user_str);
        if (user_str.contains(token)) {
          tempUserInfoList.add(uInfo);
        } else if (matcher.matches()) {
          tempUserInfoList.add(uInfo);
        }
      }
      filteredUsers.clear();
      filteredUsers.addAll(tempUserInfoList);
      tempUserInfoList.clear();
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

  private void setViewBackground(View v, int resId) {
    v.setBackgroundResource(resId);
  }

  @SuppressWarnings("deprecation")
  @SuppressLint("NewApi")
  private void setViewBackground(View v, Drawable drawable) {
    int sdk = android.os.Build.VERSION.SDK_INT;
    if (sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
      v.setBackgroundDrawable(drawable);
    } else {
      v.setBackground(drawable);
    }
  }

  private void moveSearchMenuUp() {
    if (droppedDown) {
      droppedDown = false;
      searchPanel.animate().translationY(searchMenuRetractedHeight)
        .setDuration(SCROLL_ANIMATION_SPEED_MS_FAST)
        .setInterpolator(new LinearInterpolator()).start();
      toggleFiltersVisibility(false);
    }
  }

  private void moveSearchMenuDown() {
    if (!droppedDown) {
      toggleFiltersVisibility(true);
      // Drop the shade down.
      droppedDown = true;
      searchPanel.animate().translationY(SEARCH_MENU_OVERHANG)
        .setDuration(SCROLL_ANIMATION_SPEED_MS_NORMAL)
        .setInterpolator(new OvershootInterpolator()).start();

      // Hide the keyboard
      InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
      imm.hideSoftInputFromWindow(searchPanel.getWindowToken(), 0);
    }
  }

  // Move the search menu up or down
  private void moveSearchMenu() {
    if (!droppedDown) {
      moveSearchMenuDown();
    } else {
      moveSearchMenuUp();
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
    case R.id.filter_results:
      moveSearchMenu();
      return true;
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
        prefEditor.commit();
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
    setViewBackground(dialogHeader, new CurvedAndTiled(bmDrawable.getBitmap(),
      5));
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
