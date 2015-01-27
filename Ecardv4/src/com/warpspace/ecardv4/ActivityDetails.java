package com.warpspace.ecardv4;

import java.util.ArrayList;

import com.parse.ParseUser;
import com.warpspace.ecardv4.R;
import com.warpspace.ecardv4.infrastructure.UserInfo;
import com.warpspace.ecardv4.utils.CurvedAndTiled;
import com.warpspace.ecardv4.utils.ExpandableHeightGridView;
import com.warpspace.ecardv4.utils.MyDetailsGridViewAdapter;
import com.warpspace.ecardv4.utils.MyGridViewAdapter;
import com.warpspace.ecardv4.utils.MyScrollView;
import com.warpspace.ecardv4.utils.MyTag;
import com.warpspace.ecardv4.utils.SquareLayout;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class ActivityDetails extends ActionBarActivity {

	private MyScrollView scrollView;
	ArrayList<String> shownArrayList = new ArrayList<String>();
	ArrayList<Integer> infoIcon = new ArrayList<Integer>();
	ArrayList<String> infoLink = new ArrayList<String>();

	ExpandableHeightGridView gridView;
	ParseUser currentUser;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// show custom action bar (on top of standard action bar)
	    showActionBar();
		setContentView(R.layout.activity_scanned);
	    
		scrollView = (MyScrollView) findViewById(R.id.scroll_view_scanned);
		scrollView.setmScrollable(true);

		Bundle data = getIntent().getExtras();
		UserInfo newUser = (UserInfo) data.getParcelable("userinfo");

		// display the main card
		displayCard(newUser);
		// display extra info
		infoIcon = newUser.getInfoIcon();
		infoLink = newUser.getInfoLink();
		shownArrayList = newUser.getShownArrayList();
		
		addNoteButton();

		gridView = (ExpandableHeightGridView) findViewById(R.id.gridView1);
		gridView.setAdapter(new MyDetailsGridViewAdapter(ActivityDetails.this, shownArrayList, infoLink, infoIcon));
		gridView.setOnItemClickListener(new OnItemClickListener() {

			@SuppressLint("NewApi")
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

				MyTag tag = (MyTag) view.getTag();
				if (tag != null) {
					Intent intent;
					switch(((MyTag) view.getTag()).getKey().toString()){
						case "phone":
							intent = new Intent(Intent.ACTION_VIEW, Uri.parse("tel:"+((MyTag) view.getTag()).getValue().toString()));
							startActivity(intent);
							break;
						case "message":
							intent = new Intent(Intent.ACTION_VIEW, Uri.parse("smsto:"+((MyTag) view.getTag()).getValue().toString()));
							startActivity(intent);
							break;
						case "email":
							intent = new Intent(Intent.ACTION_VIEW, Uri.parse("mailto:"+((MyTag) view.getTag()).getValue().toString()));
							startActivity(intent);
							break;
						case "about":							
							buildAboutMeDialog(view);
							break;
						case "note":
							intent = new Intent(ActivityDetails.this, ActivityNote.class);
							startActivity(intent);
							break;
						default:
							String url = ((MyTag) view.getTag()).getValue().toString();
							if(! url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("ftp://")){	
								url= "http://www.google.com/#q="+url;
							}
							intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
							startActivity(intent);
					}
				}

			}

		});

		// This is the life-saver! It fixes the bug that scrollView will go to the
		// bottom of GridView upon open
		// below is to re-scroll to the first view in the LinearLayout
		SquareLayout mainCardContainer = (SquareLayout) findViewById(R.id.main_card_container);
		scrollView.requestChildFocus(mainCardContainer, null);
	}
	
	  private void showActionBar() {
	      LayoutInflater inflator = (LayoutInflater) this
	          .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		  View v = inflator.inflate(R.layout.layout_actionbar_search, null);
		  ImageView btnBack = (ImageView) v.findViewById(R.id.btn_back);
		  btnBack.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				onBackPressed();
			}
			  
		  });
		  if (getSupportActionBar() != null) {
			  ActionBar actionBar = getSupportActionBar();
			  actionBar.setDisplayHomeAsUpEnabled(false);
			  actionBar.setDisplayShowHomeEnabled (false);
			  actionBar.setDisplayShowCustomEnabled(true);
			  actionBar.setDisplayShowTitleEnabled(false);
			  actionBar.setCustomView(v);
		  }
	  }

	private void addNoteButton() {
		infoLink.add("");
		infoIcon.add(R.drawable.note);
		shownArrayList.add("note");
		
	}

	@SuppressLint("NewApi")
	protected void buildAboutMeDialog(View view) {
		// Get the layout inflater
	    LayoutInflater inflater = getLayoutInflater();
	    View dialogView = inflater.inflate(R.layout.layout_dialog_scanned_peritem, null);
	    LinearLayout dialogHeader = (LinearLayout) dialogView.findViewById(R.id.dialog_header);
	    final TextView dialogText = (TextView) dialogView.findViewById(R.id.dialog_text);
	    TextView dialogTitle = (TextView) dialogView.findViewById(R.id.dialog_title);
	    // Set dialog header background with rounded corner
	    Bitmap bm = BitmapFactory
	    	      .decodeResource(getResources(), R.drawable.striped);
	    BitmapDrawable bmDrawable = new BitmapDrawable(getResources(), bm);
	    dialogHeader.setBackground(new CurvedAndTiled(bmDrawable.getBitmap(), 5));
	    // Set dialog title and main EditText
	    dialogTitle.setText("About Me");
	    dialogText.setText(((MyTag) view.getTag()).getValue().toString());
	    
		new AlertDialog.Builder(ActivityDetails.this)
			.setView(dialogView)
			.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					
				}
			}).show();
		
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.details_actionbar, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// this function is called when either action bar icon is tapped
		switch (item.getItemId()) {
		case R.id.design_discard:
			Toast.makeText(this, "Discarded Ecard!", Toast.LENGTH_SHORT).show();
			this.finish();
			return true;
		case R.id.design_save:
			Toast.makeText(this, "Save Ecard to collection!", Toast.LENGTH_SHORT).show();
			this.finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public void displayCard(UserInfo newUser) {

		TextView name = (TextView) findViewById(R.id.my_first_name);
		String tmpString = newUser.getFirstName();
		if (tmpString != null)
			name.setText(tmpString);
		name = (TextView) findViewById(R.id.my_last_name);
		tmpString = newUser.getLastName();
		if (tmpString != null)
			name.setText(tmpString);
		name = (TextView) findViewById(R.id.my_company);
		tmpString = newUser.getCompany();
		if (tmpString != null)
			name.setText(tmpString);
		name = (TextView) findViewById(R.id.my_job_title);
		tmpString = newUser.getTitle();
		if (tmpString != null)
			name.setText(tmpString);

	}
}
