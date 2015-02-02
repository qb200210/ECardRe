package com.warpspace.ecardv4;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.nhaarman.listviewanimations.appearance.StickyListHeadersAdapterDecorator;
import com.nhaarman.listviewanimations.appearance.simple.AlphaInAnimationAdapter;
import com.nhaarman.listviewanimations.util.StickyListHeadersListViewWrapper;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.warpspace.ecardv4.infrastructure.SearchListNameAdapter;
import com.warpspace.ecardv4.infrastructure.UserInfo;
import com.warpspace.ecardv4.infrastructure.UserNameComparator;
import com.warpspace.ecardv4.utils.CurvedAndTiled;
import com.warpspace.ecardv4.utils.MySimpleListViewAdapter;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
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
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

public class ActivitySearch extends ActionBarActivity {

  AlertDialog actions;
  ParseUser currentUser;
  String[] sortMethodArray = { "A-Z", "Z-A", "New-Old", "Old-New" };

  ArrayList<UserInfo> userNames;
  SearchListNameAdapter adapter;
  AlphaInAnimationAdapter animationAdapter;

  @Override
  protected void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // show custom action bar (on top of standard action bar)
    showActionBar();
    setContentView(R.layout.activity_search);
    currentUser = ParseUser.getCurrentUser();

    userNames = new ArrayList<UserInfo>();

    userNames.add(new UserInfo(this, "abcdef", "Udayan", "Banerji", false,
      false));
    userNames.add(new UserInfo(this, "ghijkl", "Bo", "Qiu", false, false));
    userNames.add(new UserInfo(this, "mnopqr", "Peng", "Zhao", false, false));
    userNames.add(new UserInfo(this, "stuvwx", "Simontika", "Mukherjee", false,
      false));
    userNames
      .add(new UserInfo(this, "yzaabb", "Jianfang", "Zhu", false, false));
    userNames.add(new UserInfo(this, "iurtyi", "Johnson", "Johnson", false,
      false));
    userNames
      .add(new UserInfo(this, "ccddee", "Barack", "Obama", false, false));
    userNames.add(new UserInfo(this, "ffgghh", "Alan", "Turing", false, false));
    userNames.add(new UserInfo(this, "iijjkk", "Ray", "Romano", false, false));

    Collections.sort(userNames, new UserNameComparator());

    // build dialog for sorting selection options
    buildSortDialog();

    final StickyListHeadersListView listView = (StickyListHeadersListView) findViewById(R.id.activity_stickylistheaders_listview);

    adapter = new SearchListNameAdapter(this, userNames);
    animationAdapter = new AlphaInAnimationAdapter(adapter);
    StickyListHeadersAdapterDecorator stickyListHeadersAdapterDecorator = new StickyListHeadersAdapterDecorator(
      animationAdapter);
    stickyListHeadersAdapterDecorator
      .setListViewWrapper(new StickyListHeadersListViewWrapper(listView));

    assert animationAdapter.getViewAnimator() != null;
    animationAdapter.getViewAnimator().setInitialDelayMillis(500);

    assert stickyListHeadersAdapterDecorator.getViewAnimator() != null;
    stickyListHeadersAdapterDecorator.getViewAnimator().setInitialDelayMillis(
      500);

    listView.setAdapter(stickyListHeadersAdapterDecorator);
    
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

  private void pinAllCollectedEcardsAndNotes() {
    // first search all notes matching currentUser's id, pin all notes
    // then search all ecards matching ecardId in EcardInfo, pin all ecards
    ParseQuery<ParseObject> query = ParseQuery.getQuery("ECardNote");
    query.whereEqualTo("userId", currentUser.getObjectId());
    query.findInBackground(new FindCallback<ParseObject>() {

      @Override
      public void done(List<ParseObject> objects, ParseException e) {
        if (e == null) {
          if (objects != null) {
            // these objects are notes
            // download all notes to background
            ParseObject.pinAllInBackground(objects);
            ArrayList<String> ecardList = new ArrayList<String>();
            for (Iterator<ParseObject> iter = objects.iterator(); iter
              .hasNext();) {
              ParseObject obj = iter.next();
              // store the Id's of all collected ecards
              ecardList.add(obj.get("ecardId").toString());
            }
            ParseQuery<ParseObject> query = ParseQuery.getQuery("ECardInfo");
            query.whereContainedIn("objectId", ecardList);
            query.findInBackground(new FindCallback<ParseObject>() {

              @Override
              public void done(List<ParseObject> objects, ParseException e) {
                if (e == null) {
                  if (objects != null) {
                    // download all collected cards to background
                    ParseObject.pinAllInBackground(objects);
                  }
                } else {
                  Toast.makeText(getBaseContext(), e.getMessage(),
                    Toast.LENGTH_SHORT).show();
                }
              }

            });
          }
          Toast
            .makeText(getBaseContext(), "Sync completed", Toast.LENGTH_SHORT)
            .show();
        } else {
          Toast.makeText(getBaseContext(), e.getMessage(), Toast.LENGTH_SHORT)
            .show();
        }
      }

    });

  }

  //

  //
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
    listViewInDialog.setAdapter(new MySimpleListViewAdapter(
      ActivitySearch.this, sortMethodArray));
    listViewInDialog.setOnItemClickListener(new OnItemClickListener() {

      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position,
        long id) {
        switch (position) {
        case (0):
          Toast.makeText(getApplicationContext(), "Placeholder: Sort A to Z",
            Toast.LENGTH_SHORT).show();
          adapter.reSortName(true);
          actions.dismiss();
          break;
        case (1):
          Toast.makeText(getApplicationContext(), "Placeholder: Sort Z to A",
            Toast.LENGTH_SHORT).show();
          adapter.reSortName(false);
          actions.dismiss();
          break;
        case (2):
          Toast.makeText(getApplicationContext(),
            "Placeholder: Sort New to Old", Toast.LENGTH_SHORT).show();
          actions.dismiss();
          break;
        case (3):
          Toast.makeText(getApplicationContext(),
            "Placeholder: Sort Old to New", Toast.LENGTH_SHORT).show();
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
