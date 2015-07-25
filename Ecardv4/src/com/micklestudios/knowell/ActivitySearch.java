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
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.micklestudios.knowell.infrastructure.SearchListAdapter;
import com.micklestudios.knowell.infrastructure.UserInfo;
import com.micklestudios.knowell.utils.AppGlobals;
import com.micklestudios.knowell.utils.AsyncTasks;
import com.micklestudios.knowell.utils.CurvedAndTiled;
import com.micklestudios.knowell.utils.ECardUtils;
import com.micklestudios.knowell.utils.MySimpleListViewAdapterForSearch;
import com.nhaarman.listviewanimations.appearance.StickyListHeadersAdapterDecorator;
import com.nhaarman.listviewanimations.appearance.simple.AlphaInAnimationAdapter;
import com.nhaarman.listviewanimations.util.StickyListHeadersListViewWrapper;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
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
  RelativeLayout searchWidget;
  ImageView searchButton;
  LinearLayout searchPanel;
  LinearLayout contactListContainerLayout;

  AutoCompleteTextView filterTextWhereMet;
  AutoCompleteTextView filterTextEventMet;
  AutoCompleteTextView filterTextCompany;

  // List of users filtered after search.
  public static ArrayList<Integer> filteredUsers;

  // List of users selected by the user.
  public static HashSet<UserInfo> selectedUsers;
  static ArrayList<String> autoCompleteList;
  static SearchListAdapter adapter;
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

  private int searchMenuRetractedHeight = 0;

  // Possible modes of sorting.
  public static final int SORT_MODE_NAME_ASC = 1;
  public static final int SORT_MODE_NAME_DSC = 2;
  public static final int SORT_MODE_DATE_ASC = 3;
  public static final int SORT_MODE_DATE_DSC = 4;

  // search filter flags
  private boolean flagMainClean = true;
  private boolean flagWhereMetClean = true;
  private boolean flagCompanyClean = true;
  private boolean flagEventMetClean = true;

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
  private LinearLayout searchBar;
  private LinearLayout selectionBar;
  private ImageView advSearchToggle;
  private ImageView btnClearMain;
  private ImageView btnClearWhereMet;
  private ImageView btnClearCompany;
  private ImageView btnClearEventMet;

  private ImageView btnEmailSel;
  private ImageView btnDeleteSel;
  private boolean isSelectAllChecked;

  // Needed for static functions requiring context
  private static Context currentContext;

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
    currentContext = this;

    currentUser = ParseUser.getCurrentUser();

    filteredUsers = new ArrayList<Integer>();
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
        contactListContainerLayout.setPadding(0, searchWidgetTotalHeight, 0, 0);
        searchMenuRetractedHeight = 0 - searchPanel.getMeasuredHeight();
        searchPanel.setTranslationY(searchMenuRetractedHeight);
      }
    });
    
    AppGlobals.ensureNonNullUponResume();

    // Finally, load the contacts.
    performSearch();
  }


  private void retrieveAllViews() {
    // Retrieve all the filters and layouts.
    filterTextWhereMet = (AutoCompleteTextView) findViewById(R.id.txt_where_met);
    filterTextCompany = (AutoCompleteTextView) findViewById(R.id.txt_company);
    filterTextEventMet = (AutoCompleteTextView) findViewById(R.id.txt_event_met);

    // The three frames in the layout.
    searchPanel = (LinearLayout) findViewById(R.id.lnlayout_search_menu);
    searchBar = (LinearLayout) findViewById(R.id.lnlayout_search_menu_main);
    selectionBar = (LinearLayout) findViewById(R.id.lnlayout_selection_menu_main);
    layoutNoResults = (LinearLayout) findViewById(R.id.lnlayout_no_results);
    listView = (StickyListHeadersListView) findViewById(R.id.activity_stickylistheaders_listview);
    contactListContainerLayout = (LinearLayout) findViewById(R.id.lnlayout_list_container);

    searchWidget = (RelativeLayout) findViewById(R.id.lnlayout_search_widget);
    searchBox = (AutoCompleteTextView) findViewById(R.id.txt_autocomplete_search);
    searchButton = (ImageView) findViewById(R.id.btn_search_inside);

    advSearchToggle = (ImageView) findViewById(R.id.btn_toggle_advsearch);
    btnClearMain = (ImageView) findViewById(R.id.clear_all);
    btnClearWhereMet = (ImageView) findViewById(R.id.clear_wheremet);
    btnClearCompany = (ImageView) findViewById(R.id.clear_company);
    btnClearEventMet = (ImageView) findViewById(R.id.clear_eventmet);

    btnEmailSel = (ImageView) findViewById(R.id.btn_email_sel);
    btnDeleteSel = (ImageView) findViewById(R.id.btn_delete_sel);
  }

  private void setSelectionMode(boolean set) {
    // Selection mode is already as what is needed.
    if (isSelectionMode == set) {
      return;
    }

    isSelectionMode = set;

    if (set) {
      selectionBar.setVisibility(View.VISIBLE);
      searchBar.setVisibility(View.INVISIBLE);
      isSelectAllChecked = true;
    } else {
      selectedUsers.clear();
      selectionBar.setVisibility(View.INVISIBLE);
      searchBar.setVisibility(View.VISIBLE);
    }

    adapter.notifyDataSetChanged();
  }

  public static void showClickedUser(UserInfo uInfo) {
    // We are simply browsing through the contacts.
    Intent intent = new Intent(currentContext, ActivityDetails.class);
    // passing UserInfo is made possible through Parcelable
    intent.putExtra("userinfo", uInfo);
    currentContext.startActivity(intent);
  }

  private void initializeContactList() {
    listView.setOnItemClickListener(new OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position,
        long id) {
      }
    });

    listView.setOnItemLongClickListener(new OnItemLongClickListener() {
      @Override
      public boolean onItemLongClick(AdapterView<?> parent, View view,
        int position, long id) {
        setSelectionMode(true);
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
    animationAdapter.getViewAnimator().setInitialDelayMillis(100);

    assert stickyListHeadersAdapterDecorator.getViewAnimator() != null;
    stickyListHeadersAdapterDecorator.getViewAnimator().setInitialDelayMillis(
      100);

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

    // Intercept all touch events to the drop down.
    searchBar.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
      }
    });

    searchBox.addTextChangedListener(new TextWatcher() {

      @Override
      public void beforeTextChanged(CharSequence s, int start, int count,
        int after) {
        // TODO Auto-generated method stub

      }

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
        // TODO Auto-generated method stub

      }

      @Override
      public void afterTextChanged(Editable s) {
        if (!searchBox.getText().toString().isEmpty()) {
          btnClearMain.setVisibility(View.VISIBLE);
          flagMainClean = false;
          if (droppedDown) {
            advSearchToggle.setImageResource(R.drawable.ic_tri_up_filled);
          } else {
            advSearchToggle.setImageResource(R.drawable.ic_tri_down_filled);
          }
        } else {
          flagMainClean = true;
          if (flagMainClean && flagWhereMetClean && flagCompanyClean
            && flagEventMetClean) {
            btnClearMain.setVisibility(View.GONE);
            if (droppedDown) {
              advSearchToggle.setImageResource(R.drawable.ic_tri_up_open);
            } else {
              advSearchToggle.setImageResource(R.drawable.ic_tri_down_open);
            }
          }
        }
      }
    });

    filterTextWhereMet.addTextChangedListener(new TextWatcher() {

      @Override
      public void beforeTextChanged(CharSequence s, int start, int count,
        int after) {
      }

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
      }

      @Override
      public void afterTextChanged(Editable s) {
        if (!filterTextWhereMet.getText().toString().isEmpty()) {
          btnClearMain.setVisibility(View.VISIBLE);
          btnClearWhereMet.setVisibility(View.VISIBLE);
          flagWhereMetClean = false;
          if (droppedDown) {
            advSearchToggle.setImageResource(R.drawable.ic_tri_up_filled);
          } else {
            advSearchToggle.setImageResource(R.drawable.ic_tri_down_filled);
          }
        } else {
          btnClearWhereMet.setVisibility(View.GONE);
          flagWhereMetClean = true;
          if (flagMainClean && flagWhereMetClean && flagCompanyClean
            && flagEventMetClean) {
            btnClearMain.setVisibility(View.GONE);
            if (droppedDown) {
              advSearchToggle.setImageResource(R.drawable.ic_tri_up_open);
            } else {
              advSearchToggle.setImageResource(R.drawable.ic_tri_down_open);
            }
          }
        }
      }

    });

    filterTextCompany.addTextChangedListener(new TextWatcher() {

      @Override
      public void beforeTextChanged(CharSequence s, int start, int count,
        int after) {
        // TODO Auto-generated method stub

      }

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
        // TODO Auto-generated method stub

      }

      @Override
      public void afterTextChanged(Editable s) {
        if (!filterTextCompany.getText().toString().isEmpty()) {
          btnClearMain.setVisibility(View.VISIBLE);
          btnClearCompany.setVisibility(View.VISIBLE);
          flagCompanyClean = false;
          if (droppedDown) {
            advSearchToggle.setImageResource(R.drawable.ic_tri_up_filled);
          } else {
            advSearchToggle.setImageResource(R.drawable.ic_tri_down_filled);
          }
        } else {
          btnClearCompany.setVisibility(View.GONE);
          flagCompanyClean = true;
          if (flagMainClean && flagWhereMetClean && flagCompanyClean
            && flagEventMetClean) {
            btnClearMain.setVisibility(View.GONE);
            if (droppedDown) {
              advSearchToggle.setImageResource(R.drawable.ic_tri_up_open);
            } else {
              advSearchToggle.setImageResource(R.drawable.ic_tri_down_open);
            }
          }
        }
      }

    });

    filterTextEventMet.addTextChangedListener(new TextWatcher() {

      @Override
      public void beforeTextChanged(CharSequence s, int start, int count,
        int after) {
      }

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
      }

      @Override
      public void afterTextChanged(Editable s) {
        if (!filterTextEventMet.getText().toString().isEmpty()) {
          btnClearMain.setVisibility(View.VISIBLE);
          btnClearEventMet.setVisibility(View.VISIBLE);
          flagEventMetClean = false;
          if (droppedDown) {
            advSearchToggle.setImageResource(R.drawable.ic_tri_up_filled);
          } else {
            advSearchToggle.setImageResource(R.drawable.ic_tri_down_filled);
          }
        } else {
          btnClearEventMet.setVisibility(View.GONE);
          flagEventMetClean = true;
          if (flagMainClean && flagWhereMetClean && flagCompanyClean
            && flagEventMetClean) {
            btnClearMain.setVisibility(View.GONE);
            if (droppedDown) {
              advSearchToggle.setImageResource(R.drawable.ic_tri_up_open);
            } else {
              advSearchToggle.setImageResource(R.drawable.ic_tri_down_open);
            }
          }
        }
      }

    });

    btnClearMain.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {
        if (droppedDown) {
          searchBox.setText("");
          flagMainClean = true;
          if (flagMainClean && flagWhereMetClean && flagCompanyClean
            && flagEventMetClean) {
            advSearchToggle.setImageResource(R.drawable.ic_tri_up_open);
          }
        } else {
          searchBox.setText("");
          filterTextWhereMet.setText("");
          filterTextCompany.setText("");
          filterTextEventMet.setText("");
          flagWhereMetClean = true;
          flagCompanyClean = true;
          flagEventMetClean = true;
          flagMainClean = true;
          advSearchToggle.setImageResource(R.drawable.ic_tri_down_open);
          performSearch();
          AlphaAnimation animRev = new AlphaAnimation(0.0f, 1.0f);
          animRev.setDuration(SCROLL_ANIMATION_SPEED_MS_NORMAL);
          listView.startAnimation(animRev);
        }
      }

    });

    btnClearWhereMet.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {
        filterTextWhereMet.setText("");
        flagWhereMetClean = true;
        if (flagMainClean && flagWhereMetClean && flagCompanyClean
          && flagEventMetClean) {
          advSearchToggle.setImageResource(R.drawable.ic_tri_up_open);
        }
      }

    });

    btnClearCompany.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {
        filterTextCompany.setText("");
        flagCompanyClean = true;
        if (flagMainClean && flagWhereMetClean && flagCompanyClean
          && flagEventMetClean) {
          advSearchToggle.setImageResource(R.drawable.ic_tri_up_open);
        }
      }

    });

    btnClearEventMet.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {
        filterTextEventMet.setText("");
        flagEventMetClean = true;
        if (flagMainClean && flagWhereMetClean && flagCompanyClean
          && flagEventMetClean) {
          advSearchToggle.setImageResource(R.drawable.ic_tri_up_open);
        }
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

    advSearchToggle.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {
        moveSearchMenu();
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

    ImageView chkSelectAll = (ImageView) findViewById(R.id.chk_select_all);
    chkSelectAll.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {
        selectedUsers.clear();
        if (isSelectAllChecked) {
          for (int i = 0; i < filteredUsers.size() - 1; i++, selectedUsers
            .add(AppGlobals.allUsers.get(filteredUsers.get(i))))
            ;
          isSelectAllChecked = false;
        } else {
          isSelectAllChecked = true;
        }
        adapter.notifyDataSetChanged();
      }

    });

    btnEmailSel.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {
        int failedEmailCount = 0;

        ArrayList<String> TO = new ArrayList<String>();

        // Get the selected users.
        for (UserInfo uInfoSelected : selectedUsers) {
          String email = uInfoSelected.getEmail();
          if (email == null || ECardUtils.isValidEmail(email) == false) {
            failedEmailCount++;
            Log.e("Knowell", "The failed email is " + email);
          } else {
            TO.add(email);
          }
        }

        if (failedEmailCount != 0) {
          Toast.makeText(getApplicationContext(),
            failedEmailCount + " selected users have invalid emails",
            Toast.LENGTH_LONG).show();
        }

        if (TO.size() == 0) {
          return;
        }

        // Send e-mail to selected users.
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setData(Uri.parse("mailto:"));
        emailIntent.setType("message/rfc822");
        emailIntent.putExtra(Intent.EXTRA_EMAIL,
          TO.toArray(new String[TO.size()]));
        String msgSubject;
        if (ActivityMain.currentUser.get("emailSubject") != null
          && !ActivityMain.currentUser.get("emailSubject").toString().isEmpty()) {
          msgSubject = ActivityMain.currentUser.get("emailSubject").toString();
          String processedSubject = msgSubject
            .replaceAll("#r[a-zA-Z0-9]*#", "");
          processedSubject = processedSubject.replaceAll("#m[a-zA-Z0-9]*#",
            ActivityMain.myselfUserInfo.getFirstName() + " "
              + ActivityMain.myselfUserInfo.getLastName());
          processedSubject = processedSubject.replaceAll("#c[a-zA-Z0-9]*#",
            ActivityMain.myselfUserInfo.getCompany());
          msgSubject = processedSubject
            .replaceAll("#k[a-zA-Z0-9]*#", getLink());

        } else {
          msgSubject = "Greetings from "
            + ActivityMain.myselfUserInfo.getFirstName() + " "
            + ActivityMain.myselfUserInfo.getLastName();
        }

        emailIntent.putExtra(Intent.EXTRA_SUBJECT, msgSubject);

        String msgBody;

        if (ActivityMain.currentUser.get("emailBody") != null
          && !ActivityMain.currentUser.get("emailBody").toString().isEmpty()) {
          msgBody = ActivityMain.currentUser.get("emailBody").toString();
          String processedBody = msgBody.replaceAll("#r[a-zA-Z0-9]*#", "");
          processedBody = processedBody.replaceAll("#m[a-zA-Z0-9]*#",
            ActivityMain.myselfUserInfo.getFirstName() + " "
              + ActivityMain.myselfUserInfo.getLastName());
          processedBody = processedBody.replaceAll("#c[a-zA-Z0-9]*#",
            ActivityMain.myselfUserInfo.getCompany());
          msgBody = processedBody.replaceAll("#k[a-zA-Z0-9]*#", getLink());
        } else {
          msgBody = "Hi " + ",\n\nThis is "
            + ActivityMain.myselfUserInfo.getFirstName() + " "
            + ActivityMain.myselfUserInfo.getLastName() + " from "
            + ActivityMain.myselfUserInfo.getCompany()
            + ".\n\nIt was great to meet you! Keep in touch! \n\nBest,\n"
            + ActivityMain.myselfUserInfo.getFirstName()
            + "\n\nPlease accept my business card here: " + getLink();
        }
        emailIntent.putExtra(Intent.EXTRA_TEXT, msgBody);

        try {
          startActivity(emailIntent);
        } catch (android.content.ActivityNotFoundException ex) {
          Toast.makeText(ActivitySearch.this,
            "There is no email client installed.", Toast.LENGTH_SHORT).show();
        }
      }
    });

    btnDeleteSel.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {
        AppGlobals.allUsers.removeAll(selectedUsers);
        List<String> toBeDeleted = new ArrayList<String>();
        for (UserInfo selectedUser : selectedUsers) {
          toBeDeleted.add(selectedUser.getObjId());
        }
        ParseQuery<ParseObject> queryNotes = ParseQuery.getQuery("ECardNote");
        queryNotes.whereEqualTo("userId", currentUser.getObjectId());
        queryNotes.whereContainedIn("ecardId", toBeDeleted);
        queryNotes.fromLocalDatastore();
        queryNotes.findInBackground(new FindCallback<ParseObject>() {

          @Override
          public void done(List<ParseObject> objects, ParseException e) {
            if (e == null) {
              for (ParseObject obj : objects) {
                obj.put("isDeleted", true);
                obj.saveEventually();
              }
              performSearch();// clear current selection
            }
          }

        });
      }
    });

  }

  // Hide all the filter text views
  private void toggleFiltersVisibility(boolean show) {
    int visibility = show ? View.VISIBLE : View.GONE;

    LinearLayout lLayoutCompany = (LinearLayout) findViewById(R.id.llayout_company);
    LinearLayout lLayoutWhereMet = (LinearLayout) findViewById(R.id.llayout_where_met);
    LinearLayout lLayoutEventMet = (LinearLayout) findViewById(R.id.llayout_event_met);

    lLayoutWhereMet.setVisibility(visibility);
    lLayoutEventMet.setVisibility(visibility);
    lLayoutCompany.setVisibility(visibility);
  }

  private void performSearch() {
    filteredUsers.clear();
    
    // Create a temporary list so that we can iterate over one of them.
    ArrayList<Integer> tempUserInfoList = new ArrayList<Integer>();

    /*
     * First, let's go through all the filters. Let's assume that all the users
     * will be selected.
     */
    for (int i = 0; i < AppGlobals.allUsers.size() - 1; i++, filteredUsers
      .add(i))
      ;
    {
      // Start with the Where Met filter.
      String filterKey = filterTextWhereMet.getText().toString()
        .toLowerCase(Locale.ENGLISH);
      if (filterKey != "") {
        for (Integer uInfoIndex : filteredUsers) {
          UserInfo uInfo = AppGlobals.allUsers.get(uInfoIndex);
          if (uInfo.getWhereMet().toLowerCase(Locale.ENGLISH)
            .contains(filterKey)) {
            tempUserInfoList.add(uInfoIndex);
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
        for (Integer uInfoIndex : filteredUsers) {
          UserInfo uInfo = AppGlobals.allUsers.get(uInfoIndex);
          if (uInfo.getCompany().toLowerCase(Locale.ENGLISH)
            .contains(filterKey)) {
            tempUserInfoList.add(uInfoIndex);
          }
        }
        filteredUsers.clear();
        filteredUsers.addAll(tempUserInfoList);
        tempUserInfoList.clear();
      }

      // Then look at the Event filter.
      filterKey = filterTextEventMet.getText().toString()
        .toLowerCase(Locale.ENGLISH);
      if (filterKey != "") {
        for (Integer uInfoIndex : filteredUsers) {
          UserInfo uInfo = AppGlobals.allUsers.get(uInfoIndex);
          if (uInfo.getEventMet().toLowerCase(Locale.ENGLISH)
            .contains(filterKey)) {
            tempUserInfoList.add(uInfoIndex);
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
      for (Integer uInfoIndex : filteredUsers) {
        UserInfo uInfo = AppGlobals.allUsers.get(uInfoIndex);
        String name_str = uInfo.getFirstName().toLowerCase(Locale.ENGLISH)
          + " " + uInfo.getLastName().toLowerCase(Locale.ENGLISH);
        String company_str = uInfo.getCompany().toLowerCase(Locale.ENGLISH);
        String title_str = uInfo.getTitle().toLowerCase(Locale.ENGLISH);
        String city_str = uInfo.getCity().toLowerCase(Locale.ENGLISH);
        String user_str = name_str + " " + company_str + " " + title_str + " "
          + city_str;
        // Log.v("search_user_str", user_str);
        Matcher name_matcher = pattern.matcher(name_str);
        Matcher company_matcher = pattern.matcher(company_str);
        Matcher title_matcher = pattern.matcher(title_str);
        Matcher city_matcher = pattern.matcher(city_str);
        if (user_str.contains(token)) {
          tempUserInfoList.add(uInfoIndex);
        } else if (name_matcher.matches() || company_matcher.matches()
          || title_matcher.matches() || city_matcher.matches()) {
          tempUserInfoList.add(uInfoIndex);
        }
      }

      if (tempUserInfoList.isEmpty()) {
        for (Integer uInfoIndex : filteredUsers) {
          UserInfo uInfo = AppGlobals.allUsers.get(uInfoIndex);
          String name_str = uInfo.getFirstName().toLowerCase(Locale.ENGLISH)
            + " " + uInfo.getLastName().toLowerCase(Locale.ENGLISH);
          String company_str = uInfo.getCompany().toLowerCase(Locale.ENGLISH);
          String title_str = uInfo.getTitle().toLowerCase(Locale.ENGLISH);
          String city_str = uInfo.getCity().toLowerCase(Locale.ENGLISH);
          int name_mismatch_flag = 0;
          int company_mismatch_flag = 0;
          int title_mismatch_flag = 0;
          int city_mismatch_flag = 0;
          // Log.v("name_str", name_str);
          // Log.v("company ", company_str);
          // Log.v("city ", city_str);
          // Log.v("title ", title_str);
          for (char c : token.toCharArray()) {
            if (!name_str.contains(String.valueOf(c))) {
              name_mismatch_flag = 1;
            }
            if (!company_str.contains(String.valueOf(c))) {
              company_mismatch_flag = 1;
            }
            if (!title_str.contains(String.valueOf(c))) {
              title_mismatch_flag = 1;
            }
            if (!city_str.contains(String.valueOf(c))) {
              city_mismatch_flag = 1;
            }
          }

          if (name_mismatch_flag * company_mismatch_flag * title_mismatch_flag
            * city_mismatch_flag == 0) {
            tempUserInfoList.add(uInfoIndex);
          }
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
      if (flagMainClean && flagWhereMetClean && flagCompanyClean
        && flagEventMetClean) {
        advSearchToggle.setImageResource(R.drawable.ic_tri_down_open);
      } else {
        advSearchToggle.setImageResource(R.drawable.ic_tri_down_filled);
      }
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
      if (flagMainClean && flagWhereMetClean && flagCompanyClean
        && flagEventMetClean) {
        advSearchToggle.setImageResource(R.drawable.ic_tri_up_open);
      } else {
        advSearchToggle.setImageResource(R.drawable.ic_tri_up_filled);
      }
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
    LinearLayout btnBack = (LinearLayout) v.findViewById(R.id.btn_back);
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
  public void onBackPressed() {
    if (isSelectionMode != false) {
      setSelectionMode(false);
    } else {
      super.onBackPressed();
    }
    return;
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
          AppGlobals.MY_PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor prefEditor = prefs.edit();
        final AsyncTasks.SyncDataTaskNotes syncNotes = new AsyncTasks.SyncDataTaskNotes(
          this, currentUser, prefs, prefEditor, true);
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

        Thread timerThread = new Thread() {

          public void run() {
            while (syncNotes.getStatus() == AsyncTask.Status.RUNNING) {
              try {
                sleep(500);
              } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
              }
            }
            // TO-DO: Refresh current mini-cards list!
            AppGlobals.initializeAllContacts();
            Message myMessage = new Message();
            handlerJump.sendMessage(myMessage);
          }
        };
        timerThread.start();
      } else {
        Toast.makeText(getApplicationContext(), "Network unavailable",
          Toast.LENGTH_SHORT).show();
      }
      return true;
    default:
      return super.onOptionsItemSelected(item);
    }
  }

  Handler handlerJump = new Handler() {
    @Override
    public void handleMessage(Message msg) {
      performSearch();
    }
  };

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

  private String getLink() {
    String website = ActivityMain.applicationContext
      .getString(R.string.base_website_user);
    StringBuffer qrString = new StringBuffer(website);
    qrString.append("id=");
    qrString.append(ActivityMain.myselfUserInfo.getObjId());
    qrString.append("&fn=");
    qrString.append(ActivityMain.myselfUserInfo.getFirstName());
    qrString.append("&ln=");
    qrString.append(ActivityMain.myselfUserInfo.getLastName());
    return qrString.toString();
  }

  private String getShortLink() {
    String website = ActivityMain.applicationContext
      .getString(R.string.base_website_user);
    StringBuffer qrString = new StringBuffer(website);
    qrString.append("id=");
    qrString.append(ActivityMain.myselfUserInfo.getObjId());
    return qrString.toString();
  }
}
