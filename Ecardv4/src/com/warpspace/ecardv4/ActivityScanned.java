package com.warpspace.ecardv4;

import java.util.ArrayList;
import java.util.List;

import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseACL;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.warpspace.ecardv4.R;
import com.warpspace.ecardv4.infrastructure.UserInfo;
import com.warpspace.ecardv4.utils.AsyncResponse;
import com.warpspace.ecardv4.utils.CurvedAndTiled;
import com.warpspace.ecardv4.utils.ECardSQLHelper;
import com.warpspace.ecardv4.utils.ECardUtils;
import com.warpspace.ecardv4.utils.ExpandableHeightGridView;
import com.warpspace.ecardv4.utils.GeocoderHelper;
import com.warpspace.ecardv4.utils.MyDetailsGridViewAdapter;
import com.warpspace.ecardv4.utils.MyGridViewAdapter;
import com.warpspace.ecardv4.utils.MyScrollView;
import com.warpspace.ecardv4.utils.MyTag;
import com.warpspace.ecardv4.utils.OfflineData;
import com.warpspace.ecardv4.utils.SquareLayout;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class ActivityScanned extends ActionBarActivity implements AsyncResponse {

	private MyScrollView scrollView;
	ArrayList<String> shownArrayList = new ArrayList<String>();
	ArrayList<Integer> infoIcon = new ArrayList<Integer>();
	ArrayList<String> infoLink = new ArrayList<String>();

	ExpandableHeightGridView gridView;
	private ECardSQLHelper db = null;
	ParseUser currentUser;
	private UserInfo scannedUser;
	
	// need to use this to hold the interface to be passed to GeocoderHelper
	// constructor, otherwise NullPoint
	AsyncResponse delegate = null;
	private String whereMet = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_scanned);
		
		db = new ECardSQLHelper(this);
		currentUser = ParseUser.getCurrentUser();

		scrollView = (MyScrollView) findViewById(R.id.scroll_view_scanned);
		scrollView.setmScrollable(true);

		Bundle data = getIntent().getExtras();
		scannedUser = (UserInfo) data.getParcelable("userinfo");
		
		// getting "where met" city info
		// this will be used later -- where "this" is ambiguous, so directly
	    // storing delegate for later use
	    delegate = this;
		// if there is network, start a thread to get location name
    	Location location = getLocation();
    	if(location != null){
    	  Log.i("ActScan", "location not null");
          new GeocoderHelper(delegate).fetchCityName(getBaseContext(),location);
    	} else {
    		// if getting location fails, will bypass the processFinish() function
    		Toast.makeText(getBaseContext(), "Cannot determine location...", Toast.LENGTH_SHORT).show();
    		whereMet = null;
    	}		

		// display the main card
		displayCard(scannedUser);
		// display extra info
		infoIcon = scannedUser.getInfoIcon();
		infoLink = scannedUser.getInfoLink();
		shownArrayList = scannedUser.getShownArrayList();
		addNoteButton();

		gridView = (ExpandableHeightGridView) findViewById(R.id.gridView1);
		gridView.setAdapter(new MyDetailsGridViewAdapter(ActivityScanned.this, shownArrayList, infoLink, infoIcon));
		gridView.setOnItemClickListener(new OnItemClickListener() {

			@SuppressLint("NewApi")
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

				MyTag tag = (MyTag) view.getTag();
				if (tag != null) {
					Intent intent;
					switch (((MyTag) view.getTag()).getKey().toString()) {
					case "phone":
						intent = new Intent(Intent.ACTION_VIEW, Uri.parse("tel:" + ((MyTag) view.getTag()).getValue().toString()));
						startActivity(intent);
						break;
					case "message":
						intent = new Intent(Intent.ACTION_VIEW, Uri.parse("smsto:" + ((MyTag) view.getTag()).getValue().toString()));
						startActivity(intent);
						break;
					case "email":
						intent = new Intent(Intent.ACTION_VIEW, Uri.parse("mailto:" + ((MyTag) view.getTag()).getValue().toString()));
						startActivity(intent);
						break;
					case "about":
						buildAboutMeDialog(view);
						break;
					case "note":
						intent = new Intent(ActivityScanned.this, ActivityNotes.class);
						intent.putExtra("whereMet",	whereMet);
						startActivity(intent);
						break;
					default:
						String url = ((MyTag) view.getTag()).getValue().toString();
						if (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("ftp://")) {
							url = "http://www.google.com/#q=" + url;
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
		Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.striped);
		BitmapDrawable bmDrawable = new BitmapDrawable(getResources(), bm);
		dialogHeader.setBackground(new CurvedAndTiled(bmDrawable.getBitmap(), 5));
		// Set dialog title and main EditText
		dialogTitle.setText("About Me");
		dialogText.setText(((MyTag) view.getTag()).getValue().toString());

		new AlertDialog.Builder(ActivityScanned.this).setView(dialogView).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {

			}
		}).show();

	}

	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.design_actionbar, menu);
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
			if(ECardUtils.isNetworkAvailable(this)){
				// If there is network connection, pull from server
				addNewCard(scannedUser.getObjId());
			} else{
				// if no network, cache the scannedID to local db, wait till
				// network comes back to add Ecard
				cacheScannedIds(scannedUser.getObjId());								
			}
			
			this.finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	private void cacheScannedIds(String scannedId) {
	    List<OfflineData> olDatas = db.getData("ecardID", scannedId);
	    if (olDatas.size() == 0) {
	      // if EcardID is not among local db records, cache it
	      db.addData(new OfflineData(scannedId, "/sdcard/aaa/"));
	      Toast.makeText(getBaseContext(),
	          "Ecard cached, will add when next time connect to internet",
	          Toast.LENGTH_SHORT).show();
	    } else {
	      Toast.makeText(getBaseContext(),
	          "Already in local queue, but still cached!", Toast.LENGTH_SHORT)
	          .show();
	      // flip the flag, give it a chance to be revisited
	      olDatas.get(0).setStored(0);
	      db.updataData(olDatas.get(0));
	    }
	  }

	public void addNewCard(final String scannedId) {

		ParseQuery<ParseObject> query = ParseQuery.getQuery("ECardInfo");
		// get the Ecardinfo from server
		query.getInBackground(scannedId, new GetCallback<ParseObject>() {

			@Override
			public void done(ParseObject object, ParseException e) {
				if (e == null) {
					if (object == null) {
						// If ecard non-exist
						Toast.makeText(getBaseContext(), "No such Ecard with ID: " + scannedId, Toast.LENGTH_SHORT).show();
					} else {
						// The ecard is valid, now check if it's collected already
						ParseQuery<ParseObject> query = ParseQuery.getQuery("ECardNote");
						query.fromLocalDatastore();
						query.whereEqualTo("userId", currentUser.getObjectId());
						query.whereEqualTo("ecardId", object.getObjectId());
						query.findInBackground(new FindCallback<ParseObject>() {

							@Override
							public void done(List<ParseObject> objects, ParseException e) {
								if (e == null) {
									if (objects.size() == 0) {
										// If Ecard not collected yet, add it to EcardNote
										ParseObject ecardNote = new ParseObject("ECardNote");
										ecardNote.setACL(new ParseACL(currentUser));
										ecardNote.put("userId", currentUser.getObjectId());
										ecardNote.put("ecardId", scannedId);
										// if(cityName != null){
										// ecardNote.put("where", cityName);
										// }
										// fetch the EcardInfo to be added to extract some info that needs to be placed into EcardNote
										ParseQuery<ParseObject> query = ParseQuery.getQuery("ECardInfo");
										ParseObject object = null;
										try {
											object = query.get(scannedId);
										} catch (ParseException e1) {
											// TODO Auto-generated catch block
											Toast.makeText(getBaseContext(), "General Parse Error", Toast.LENGTH_SHORT).show();
											e1.printStackTrace();
										}
										if (object != null) {
											// if null, no worries, these info can be filled in later in "refresh"
											ecardNote.put("EcardUpdatedAt", object.getUpdatedAt());
											object.pinInBackground();
											Toast.makeText(getBaseContext(), "Ecard " + scannedId + " added!", Toast.LENGTH_SHORT).show();
										}
										ecardNote.saveInBackground();
										ecardNote.pinInBackground();
									} else {
										Toast.makeText(getBaseContext(), "Ecard " + scannedId + " exists!", Toast.LENGTH_SHORT).show();
									}
								} else {
									Toast.makeText(getBaseContext(), "General Parse Error", Toast.LENGTH_SHORT).show();
								}
							}

						});
					}
				} else {
					Toast.makeText(getBaseContext(), "General Parse query error", Toast.LENGTH_SHORT).show();
				}
			}

		});
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
	
	private Location getLocation() {
	    LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
	    List<String> providers = lm.getProviders(true);

	    Location location = null;

	    for (int i = providers.size() - 1; i >= 0; i--) {
	      location = lm.getLastKnownLocation(providers.get(i));
	      if (location != null)
	        break;
	    }

	    return location;
	  }
	
	@Override
	  public void processFinish(String output) {
	    Log.i("GeocoderHelperAdd", output);
	    // save the obtained cityName to global variable to be passed to ActivityNotes
	    whereMet = output;
	  }
}
