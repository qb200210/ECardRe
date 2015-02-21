package com.warpspace.ecardv4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import se.emilsjolander.stickylistheaders.StickyListHeadersListView;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
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
import com.warpspace.ecardv4.infrastructure.UserInfoNameComparator;
import com.warpspace.ecardv4.utils.AsyncTasks;
import com.warpspace.ecardv4.utils.CurvedAndTiled;
import com.warpspace.ecardv4.utils.ECardUtils;
import com.warpspace.ecardv4.utils.MySimpleListViewAdapter;
import com.warpspace.ecardv4.utils.MySimpleListViewAdapterForSearch;

public class ActivitySearch extends ActionBarActivity {

  AlertDialog actions;
  ParseUser currentUser;
  String[] sortMethodArray = { "A-Z", "Z-A", "New-Old", "Old-New" };
  private static final long NOTES_TIMEOUT = 10000;

  ArrayList<UserInfo> userNames;
  SearchListAdapter adapter;
  AlphaInAnimationAdapter animationAdapter;
  StickyListHeadersAdapterDecorator stickyListHeadersAdapterDecorator;
  StickyListHeadersListView listView;

  private ArrayList<String> ecardIds = new ArrayList<String>();
  private ArrayList<String> returnedIds = new ArrayList<String>();
  List<Integer> idsNote = Arrays.asList(R.id.query_event_met,
    R.id.query_where_met);
  List<Integer> idsCardEdit = Arrays.asList(R.id.query_name,
    R.id.query_job_title);
  List<Integer> idsCardView = Arrays.asList(R.id.query_company_name,
    R.id.query_where_work);
  List<String> fieldsNote = Arrays.asList("event_met_lc", "where_met_lc");

  @Override
  protected void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // show custom action bar (on top of standard action bar)
    showActionBar();
    setContentView(R.layout.activity_search);
    currentUser = ParseUser.getCurrentUser();

    userNames = new ArrayList<UserInfo>();

    userNames.add(new UserInfo(this, "abcdef", "Udayan", "Banerji", false,
      false, false));
    userNames
      .add(new UserInfo(this, "ghijkl", "Bo", "Qiu", false, false, false));
    userNames.add(new UserInfo(this, "mnopqr", "Peng", "Zhao", false, false,
      false));
    userNames.add(new UserInfo(this, "stuvwx", "Simontika", "Mukherjee", false,
      false, false));
    userNames.add(new UserInfo(this, "yzaabb", "Jianfang", "Zhu", false, false,
      false));
    userNames.add(new UserInfo(this, "iurtyi", "Johnson", "Johnson", false,
      false, false));
    userNames.add(new UserInfo(this, "ccddee", "Barack", "Obama", false, false,
      false));
    userNames.add(new UserInfo(this, "ffgghh", "Alan", "Turing", false, false,
      false));
    userNames.add(new UserInfo(this, "iijjkk", "Ray", "Romano", false, false,
      false));

    Collections.sort(userNames, new UserInfoNameComparator());

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
  }

  private void getContacts() {
    ecardIds.clear();
    returnedIds.clear();
    ParseQuery<ParseObject> query = ParseQuery.getQuery("ECardNote");
    query.fromLocalDatastore();
    query.whereEqualTo("userId", currentUser.getObjectId());
    // specifically for time search
    query.findInBackground(new FindCallback<ParseObject>() {

      @Override
      public void done(List<ParseObject> objectsNoteList, ParseException e) {
        if (e == null) {
          if (objectsNoteList.size() != 0) {
            System.out.println(objectsNoteList.size());
            for (Iterator<ParseObject> iter = objectsNoteList.iterator(); iter
              .hasNext();) {
              ParseObject objectNote = iter.next();
              // collect EcardInfo IDs satisfying Note searches
              // here object is Note object
              String objectIdString = (String) objectNote.get("ecardId");
              UserInfo contact = new UserInfo(getApplicationContext(),
                objectIdString);
              contact.setCreated((String) objectNote.get("createdAt"));
              userNames.add(contact);
            }
            adapter = new SearchListAdapter(getApplicationContext(), userNames);
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
    case R.id.search_options:
      Intent intent = new Intent(this, QueryActivity.class);
      startActivity(intent);
      return true;
    case R.id.download_cards:
    	if(ECardUtils.isNetworkAvailable(this)){
			// upon opening, pin online notes to local
			final AsyncTasks.SyncDataTaskNotes syncNotes = new AsyncTasks.SyncDataTaskNotes(this, currentUser);
			syncNotes.execute();
			Handler handlerNotes = new Handler();
			handlerNotes.postDelayed(new Runnable() {
		
				@Override
				public void run() {
					if (syncNotes.getStatus() == AsyncTask.Status.RUNNING) {
						Toast.makeText(getApplicationContext(), "Sync Notes Timed Out", Toast.LENGTH_SHORT).show();
						syncNotes.cancel(true);
					}
				}
			}, NOTES_TIMEOUT);
    	} else {
    		Toast.makeText(getApplicationContext(), "Network unavailable", Toast.LENGTH_SHORT).show();			
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

  @SuppressLint("NewApi")
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
          adapter.reSortDate(true);
          actions.dismiss();
          break;
        case (3):
          adapter.reSortDate(false);
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
