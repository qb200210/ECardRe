package com.warpspace.ecardv4;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class ActivityConversations extends ActionBarActivity {

	protected static final int SAVE_CARD = 0;
	private ParseUser currentUser;
	ArrayList<UserInfo> userNames = new ArrayList<UserInfo>();
	SearchListAdapter adapter;
	AlphaInAnimationAdapter animationAdapter;
	StickyListHeadersAdapterDecorator stickyListHeadersAdapterDecorator;
	StickyListHeadersListView listView;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_conversations);
		currentUser = ParseUser.getCurrentUser();
		
		listView = (StickyListHeadersListView) findViewById(R.id.activity_conversations_listview);
	    listView.setOnItemClickListener(new OnItemClickListener() {

	      @Override
	      public void onItemClick(AdapterView<?> parent, View view, int position,
	        long id) {
	        UserInfo selectedUser = (UserInfo) listView.getItemAtPosition(position);
	        
	        // How to set card to be read? Need two layouts in the listview
	        // selectedUser.setRead(true);
	        // flipMiniCard(position);

	        Intent intent = new Intent(getBaseContext(), ActivityScanned.class);
	        // passing UserInfo is made possible through Parcelable
	        intent.putExtra("userinfo", selectedUser);
	        startActivityForResult(intent, SAVE_CARD);
	      }
	    });
	    
		getContacts();
	}

	private void getContacts() {
		ParseQuery<ParseObject> query = ParseQuery.getQuery("Conversations");
		query.fromLocalDatastore();
		query.whereEqualTo("partyB", currentUser.get("ecardId").toString());
		query.findInBackground(new FindCallback<ParseObject>() {

			@Override
			public void done(List<ParseObject> conversationList, ParseException e) {
				if (e == null) {
					if (conversationList.size() != 0) {
						Toast.makeText(ActivityConversations.this, "conv: "+conversationList.size(), Toast.LENGTH_SHORT).show();
						for (Iterator<ParseObject> iter = conversationList.iterator(); iter.hasNext();) {
							ParseObject objectConversation = iter.next();
							// collect EcardInfo IDs satisfying Note searches
							// here object is Note object
							String ecardIdString = (String) objectConversation.get("partyA");
							// NEED FIX: If somehow the card wasn't cached to local, it will crash
							UserInfo contact = new UserInfo(getApplicationContext(), ecardIdString, "", "", true, false, false);
							userNames.add(contact);
						}
						
						adapter = new SearchListAdapter(getApplicationContext(), userNames);
						animationAdapter = new AlphaInAnimationAdapter(adapter);
						stickyListHeadersAdapterDecorator = new StickyListHeadersAdapterDecorator(animationAdapter);
						stickyListHeadersAdapterDecorator.setListViewWrapper(new StickyListHeadersListViewWrapper(listView));

						assert animationAdapter.getViewAnimator() != null;
						animationAdapter.getViewAnimator().setInitialDelayMillis(500);

						assert stickyListHeadersAdapterDecorator.getViewAnimator() != null;
						stickyListHeadersAdapterDecorator.getViewAnimator().setInitialDelayMillis(500);

						listView.setAdapter(stickyListHeadersAdapterDecorator);
						adapter.reSortName(true);
						stickyListHeadersAdapterDecorator.notifyDataSetChanged();
					}
				} else {
					Toast.makeText(getBaseContext(), "General parse error!", Toast.LENGTH_SHORT).show();
				}
			}
		});
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
	}
	
}
