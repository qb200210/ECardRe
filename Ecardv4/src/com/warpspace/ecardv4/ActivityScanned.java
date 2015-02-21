package com.warpspace.ecardv4;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParsePush;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.warpspace.ecardv4.R;
import com.warpspace.ecardv4.infrastructure.UserInfo;
import com.warpspace.ecardv4.utils.AsyncResponse;
import com.warpspace.ecardv4.utils.AsyncTasks;
import com.warpspace.ecardv4.utils.CurvedAndTiled;
import com.warpspace.ecardv4.utils.ECardSQLHelper;
import com.warpspace.ecardv4.utils.ECardUtils;
import com.warpspace.ecardv4.utils.ExpandableHeightGridView;
import com.warpspace.ecardv4.utils.GeocoderHelper;
import com.warpspace.ecardv4.utils.MyDetailsGridViewAdapter;
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
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class ActivityScanned extends ActionBarActivity implements AsyncResponse {

	private static final long ADDCARD_TIMEOUT = 10000;
	private MyScrollView scrollView;
	ArrayList<String> shownArrayList = new ArrayList<String>();
	ArrayList<Integer> infoIcon = new ArrayList<Integer>();
	ArrayList<String> infoLink = new ArrayList<String>();

	ExpandableHeightGridView gridView;
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
		if (location != null) {
			Log.i("ActScan", "location not null");
			new GeocoderHelper(delegate).fetchCityName(getBaseContext(), location);
		} else {
			// if getting location fails, will bypass the processFinish() function
			Toast.makeText(getBaseContext(), "Cannot determine location for now...", Toast.LENGTH_SHORT).show();
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
						intent.putExtra("whereMet", whereMet);
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
		getMenuInflater().inflate(R.menu.scanned_actionbar, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// this function is called when either action bar icon is tapped
		switch (item.getItemId()) {
		case R.id.scanned_discard:
			Toast.makeText(this, "Discarded Ecard!", Toast.LENGTH_SHORT).show();
			setResult(RESULT_CANCELED);
			this.finish();
			return true;
		case R.id.scanned_save:
			// save scanned card either online or cache it offline
			if (ECardUtils.isNetworkAvailable(this)) {
				final AsyncTasks.AddCardNetworkAvailable addNewCard = new AsyncTasks.AddCardNetworkAvailable(this, currentUser, scannedUser.getObjId());
				addNewCard.execute();
				Handler handlerAddNewCard = new Handler();
				handlerAddNewCard.postDelayed(new Runnable() {

					@Override
					public void run() {
						if (addNewCard.getStatus() == AsyncTask.Status.RUNNING) {
							Toast.makeText(getApplicationContext(), "Adding New Card Timed Out", Toast.LENGTH_SHORT).show();
							// if poor network, cache the scannedID to local db, wait till
							// network comes back to add Ecard
							cacheScannedIds(scannedUser.getObjId());
							addNewCard.cancel(true);
						}
					}
				}, ADDCARD_TIMEOUT);
			} else {
				// no network, cache to local database
				cacheScannedIds(scannedUser.getObjId());
			}

			askIfShareBack();

			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@SuppressLint("NewApi")
	private void askIfShareBack() {
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
		dialogTitle.setText("Share back?");

		new AlertDialog.Builder(ActivityScanned.this).setView(dialogView).setPositiveButton("Sure", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				sendPush(scannedUser.getObjId());
				setResult(RESULT_OK);
				ActivityScanned.this.finish();
			}
		}).setNegativeButton("Nope", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				setResult(RESULT_OK);
				ActivityScanned.this.finish();
			}
		}).show();

	}

	private void cacheScannedIds(String scannedId) {

		ECardSQLHelper db = new ECardSQLHelper(this);
		List<OfflineData> olDatas = db.getData("ecardID", scannedId);
		if (olDatas.size() == 0) {
			// if EcardID is not among local db records, cache it
			db.addData(new OfflineData(scannedId, "/sdcard/aaa/"));
			Toast.makeText(getBaseContext(), "Ecard cached, will add when next time connect to internet", Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(getBaseContext(), "Already in local queue, but still cached!", Toast.LENGTH_SHORT).show();
			// flip the flag, give it a chance to be revisited
			olDatas.get(0).setStored(0);
			db.updataData(olDatas.get(0));
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
		ImageView portraitImg = (ImageView) findViewById(R.id.my_portrait);
		if (newUser.getPortrait() != null) {
			portraitImg.setImageBitmap(newUser.getPortrait());
		}

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

		if (location == null) {
			lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 3600000, 1000, onLocationChange);
		}

		return location;
	}

	LocationListener onLocationChange = new LocationListener() {
		public void onLocationChanged(Location fix) {

			Log.i("onLocationChange", "Location found");
			new GeocoderHelper(delegate).fetchCityName(getBaseContext(), fix);

		}

		public void onProviderDisabled(String provider) {
			// required for interface, not used
		}

		public void onProviderEnabled(String provider) {
			// required for interface, not used
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
			// required for interface, not used
		}
	};

	@Override
	public void processFinish(String output) {
		Log.i("GeocoderHelperAdd", output);
		// save the obtained cityName to global variable to be passed to ActivityNotes
		whereMet = output;
	}

	public void sendPush(final String targetEcardId) {

		// Meanwhile, create a record in conversations -- so web app can check since it cannot receive notification
		// need to see how to fix ACL so only both parties can access conversation
		ParseObject object = new ParseObject("Conversations");
		object.put("partyA", currentUser.get("ecardId").toString());
		object.put("partyB", targetEcardId);
		object.put("read", false);
		object.saveEventually(new SaveCallback() {

			@Override
			public void done(ParseException arg0) {
				// what if offline? so far so good... no notification, but will create conversations records
				// make sure the conversation record is created before a notification is sent
				// Send push to the other party according to their ecardId recorded in an installation
				ParseQuery pushQuery = ParseInstallation.getQuery();
				pushQuery.whereEqualTo("ecardId", targetEcardId);
				JSONObject jsonObject = new JSONObject();
				try {
					jsonObject.put("alert", "Hi, I'm " + currentUser.get("ecardId").toString() + ", save my card now");
					jsonObject.put("link", "https://ecard.parseapp.com/search?id=" + currentUser.get("ecardId").toString() + "&fn=Udayan&ln=Banerji");
					jsonObject.put("action", "EcardOpenConversations");
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				ParsePush push = new ParsePush();
				push.setQuery(pushQuery);
				push.setData(jsonObject);
				push.sendInBackground();
			}

		});
	}
}
