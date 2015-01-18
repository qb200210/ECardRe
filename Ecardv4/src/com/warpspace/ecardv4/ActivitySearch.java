package com.warpspace.ecardv4;

import java.util.ArrayList;
import java.util.Arrays;

import com.nhaarman.listviewanimations.appearance.StickyListHeadersAdapterDecorator;
import com.nhaarman.listviewanimations.appearance.simple.AlphaInAnimationAdapter;
import com.nhaarman.listviewanimations.util.StickyListHeadersListViewWrapper;
import com.parse.ParseUser;
import com.warpspace.ecardv4.infrastructure.SearchListNameAdapter;
import com.warpspace.ecardv4.utils.CurvedAndTiled;
import com.warpspace.ecardv4.utils.MySimpleListViewAdapter;
import com.warpspace.ecardv4.utils.MyTag;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

public class ActivitySearch extends ActionBarActivity {
	
	AlertDialog actions;
	String[] sortMethodArray = { "A-Z", "Z-A", "New-Old", "Old-New" };
	
  @Override
  protected void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_search);

    // build dialog for sorting selection options
    buildSortDialog();

    StickyListHeadersListView listView = (StickyListHeadersListView) findViewById(R.id.activity_stickylistheaders_listview);

    SearchListNameAdapter adapter = new SearchListNameAdapter(this);
    AlphaInAnimationAdapter animationAdapter = new AlphaInAnimationAdapter(
      adapter);
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
    case R.id.log_out:
      ParseUser.logOut();
      Intent intent = new Intent(this, ActivityPreLogin.class);
      startActivity(intent);
      this.finish();
      return true;
    default:
      return super.onOptionsItemSelected(item);
    }
  }
  
  //
  @SuppressLint("NewApi")
private void buildSortDialog() {
		// Get the layout inflater
	    LayoutInflater inflater = getLayoutInflater();
	    View dialogAddMoreView = inflater.inflate(R.layout.layout_dialog_addmore, null);
	    LinearLayout dialogHeader = (LinearLayout) dialogAddMoreView.findViewById(R.id.dialog_header);
	    TextView dialogTitle = (TextView) dialogAddMoreView.findViewById(R.id.dialog_title);
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
		
		// Below is to build the listener for items listed inside the poped up "addmorebutton dialog"
		ListView listViewInDialog = (ListView)dialogAddMoreView.findViewById(R.id.dialog_listview);
	    listViewInDialog.setAdapter(new MySimpleListViewAdapter(ActivitySearch.this, sortMethodArray));
		listViewInDialog.setOnItemClickListener(new OnItemClickListener(){

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				switch(position){
					case(0):
						Toast.makeText(getApplicationContext(), "Placeholder: Sort A to Z", Toast.LENGTH_SHORT).show();
						actions.dismiss();
						break;
					case(1):
						Toast.makeText(getApplicationContext(), "Placeholder: Sort Z to A", Toast.LENGTH_SHORT).show();
						actions.dismiss();	
						break;
					case(2):
						Toast.makeText(getApplicationContext(), "Placeholder: Sort New to Old", Toast.LENGTH_SHORT).show();
						actions.dismiss();	
						break;
					case(3):
						Toast.makeText(getApplicationContext(), "Placeholder: Sort Old to New", Toast.LENGTH_SHORT).show();
						actions.dismiss();	
						break;
					default:
						Toast.makeText(getApplicationContext(), "Placeholder: Default", Toast.LENGTH_SHORT).show();
						actions.dismiss();	
				}
			}
			
		});
	}
}
