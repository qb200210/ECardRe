package com.micklestudios.knowell;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.micklestudios.knowell.infrastructure.UserInfo;
import com.micklestudios.knowell.utils.AsyncResponse;
import com.micklestudios.knowell.utils.AsyncTasks;
import com.micklestudios.knowell.utils.CurvedAndTiled;
import com.micklestudios.knowell.utils.ECardSQLHelper;
import com.micklestudios.knowell.utils.ECardUtils;
import com.micklestudios.knowell.utils.ExpandableHeightGridView;
import com.micklestudios.knowell.utils.GeocoderHelper;
import com.micklestudios.knowell.utils.MyDetailsGridViewAdapter;
import com.micklestudios.knowell.utils.MyScrollView;
import com.micklestudios.knowell.utils.MyTag;
import com.micklestudios.knowell.utils.OfflineData;
import com.micklestudios.knowell.utils.SquareLayout;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseACL;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParsePush;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.micklestudios.knowell.ActivityScanned;
import com.micklestudios.knowell.R;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
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
	private MediaRecorder recorder = null;
	private MediaPlayer mp=null;
	private ImageView replayButtonBar; 
	private ImageView replayButtonPanel; 
	private ImageView recorderButton;
	private ImageView timerButton;
	private static final String AUDIO_RECORDER_FOLDER = "AudioRecorder";
	private static final long SAVENOTE_TIMEOUT = 5000;
	CountDownTimer t;
	private int flag=0;
	private String filepath;
	private String noteId = null;
	private int recordstatus= 0; //0 means not recording, 1 means in the process of recording

	// need to use this to hold the interface to be passed to GeocoderHelper
	// constructor, otherwise NullPoint
	AsyncResponse delegate = null;
	private String whereMet = null;
	private boolean flagOfflineMode;
	private String deletedNoteId = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// two possiblities of entering ActivityScanned:
		// 1. online, checked ecard exist and not collected
		// 2. offline, didn't check ecard exist or if collected
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_scanned);
		filepath=getFilename();
		currentUser = ParseUser.getCurrentUser();

		scrollView = (MyScrollView) findViewById(R.id.scroll_view_scanned);
		scrollView.setmScrollable(true);

		Bundle data = getIntent().getExtras();
		scannedUser = (UserInfo) data.getParcelable("userinfo");
		flagOfflineMode = (boolean) data.get("offlineMode");
		deletedNoteId  = (String) data.get("deletedNoteId");
		
		if(!flagOfflineMode){
			// get location only when online
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
				Toast.makeText(getBaseContext(), "Your GPS is off...", Toast.LENGTH_SHORT).show();
				whereMet = null;
			}
		}
		
		// setOnclickListener for note bar/panel switcher
		ImageView barNoteButton = (ImageView) findViewById(R.id.bar_note_button);
		barNoteButton.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				RelativeLayout notePanelLayout = (RelativeLayout) findViewById(R.id.note_panel);
				notePanelLayout.setVisibility(View.VISIBLE);
				RelativeLayout noteBarLayout = (RelativeLayout) findViewById(R.id.note_bar);
				noteBarLayout.setVisibility(View.GONE);
			}
			
		});
		ImageView panelNoteButton = (ImageView) findViewById(R.id.panel_note_button);		
		panelNoteButton.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				RelativeLayout notePanelLayout = (RelativeLayout) findViewById(R.id.note_panel);
				notePanelLayout.setVisibility(View.GONE);
				RelativeLayout noteBarLayout = (RelativeLayout) findViewById(R.id.note_bar);
				noteBarLayout.setVisibility(View.VISIBLE);
				TextView whereMet1 = (TextView) findViewById(R.id.PlaceAdded1);
				EditText whereMet2 = (EditText) findViewById(R.id.PlaceAdded2);				
				if(whereMet2.getText() != null) {
					whereMet1.setText(whereMet2.getText().toString());
				}
			}
			
		});

		// display the main card
		displayCard(scannedUser);
		// display extra info
		infoIcon = scannedUser.getInfoIcon();
		infoLink = scannedUser.getInfoLink();
		shownArrayList = scannedUser.getShownArrayList();

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
		
		// replay related
		replayButtonBar = (ImageView) findViewById(R.id.bar_play_button);
		replayButtonPanel = (ImageView) findViewById(R.id.panel_play_button);
		recorderButton = (ImageView) findViewById(R.id.panel_recorder_button);
		timerButton = (ImageView) findViewById(R.id.stop_recording);
		
		// display note if the note existed but deleted
		if(deletedNoteId != null){
			ParseQuery<ParseObject> query = ParseQuery.getQuery("ECardNote");
			query.whereEqualTo("userId", currentUser.getObjectId().toString());
			query.whereEqualTo("ecardId", scannedUser.getObjId());
			query.findInBackground(new FindCallback<ParseObject>(){

				@Override
				public void done(List<ParseObject> objects, ParseException e) {
					if(e== null){
						if(objects != null){
							noteId = objects.get(0).getObjectId().toString();
							displayNote(objects.get(0));						
						}
					} else {
						e.printStackTrace();
					}
				}

				private void displayNote(final ParseObject object) {
					TextView whenMet1 = (TextView) findViewById(R.id.DateAdded1);
					TextView whenMet2 = (TextView) findViewById(R.id.DateAdded2);
					TextView updatedAt = (TextView) findViewById(R.id.LastUpdated);
					whenMet1.setText(android.text.format.DateFormat.format("MMM", object.getCreatedAt()) + " " + 
							android.text.format.DateFormat.format("dd", object.getCreatedAt()) + ", " +
							android.text.format.DateFormat.format("yyyy", object.getCreatedAt()));
					whenMet2.setText(android.text.format.DateFormat.format("MMM", object.getCreatedAt()) + " " + 
							android.text.format.DateFormat.format("dd", object.getCreatedAt()) + ", " +
							android.text.format.DateFormat.format("yyyy", object.getCreatedAt()));
					updatedAt.setText(android.text.format.DateFormat.format("MMM", object.getUpdatedAt()) + " " + 
							android.text.format.DateFormat.format("dd", object.getUpdatedAt()) + ", " +
							android.text.format.DateFormat.format("yyyy", object.getUpdatedAt()));
					TextView whereMet1 = (TextView) findViewById(R.id.PlaceAdded1);
					EditText whereMet2 = (EditText) findViewById(R.id.PlaceAdded2);
					String cityName = object.getString("where_met");
					if(cityName != null) {
						whereMet1.setText(cityName);
						whereMet2.setText(cityName);
					}
					EditText eventMet = (EditText) findViewById(R.id.EventAdded2);
					String eventName = object.getString("event_met");
					if(eventName != null) {
						eventMet.setText(eventName);
					}
					EditText notes = (EditText) findViewById(R.id.EditNotes);
					String notesContent = object.getString("notes");
					if(notesContent != null) {
						notes.setText(notesContent);
					}				
					byte[] tmpVoiceData = (byte[]) object.get("tmpVoiceByteArray");
					if (tmpVoiceData != null) {
						// save as parseFile then clean the array if there is network
						// this is necessary as user can switch on network without restarting app
						if(ECardUtils.isNetworkAvailable(ActivityScanned.this)){
							final ParseFile voiceFile = new ParseFile("voicenote.mp4", tmpVoiceData);
						    voiceFile.saveInBackground(new SaveCallback(){

								@Override
								public void done(ParseException arg0) {
									object.put("voiceNotes", voiceFile);
									object.remove("tmpVoiceByteArray");
									object.saveEventually();
								}
						    	
						    });
						}
						// use the array to populate replay no matter with network or not
						FileOutputStream out;
						try {
							out = new FileOutputStream(filepath);
							out.write(tmpVoiceData);
				            out.close();
						} catch (FileNotFoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}					
						 
					} else {
						// only check the voiceNotes when tmpVoice data is empty
						// that is always use tmpArray to overwrite voiceNotes if there is conflict
						ParseFile audioFile = (ParseFile) object.get("voiceNotes");
			            if (audioFile != null) {
			              byte[] audioData;
						  try {
							audioData = audioFile.getData();
							FileOutputStream out = new FileOutputStream(filepath);
				            out.write(audioData);
				            out.close();
						  } catch (ParseException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						  } catch (FileNotFoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						  } catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						  }
			            } else {
			            	// if both tmparray and voiceNotes are null, then it means no voice note at all
			            	replayButtonBar.setVisibility(View.GONE);
			            	replayButtonPanel.setVisibility(View.GONE);
			            }
					}
				}
				
			});		
		} else {
			// this is a new note
			TextView whenMet1 = (TextView) findViewById(R.id.DateAdded1);
			TextView whenMet2 = (TextView) findViewById(R.id.DateAdded2);
			TextView updatedAt = (TextView) findViewById(R.id.LastUpdated);					
			Date today = new Date();
			whenMet1.setText(android.text.format.DateFormat.format("MMM", today) + " " + 
					android.text.format.DateFormat.format("dd", today) + ", " +
					android.text.format.DateFormat.format("yyyy", today));
			whenMet2.setText(android.text.format.DateFormat.format("MMM", today) + " " + 
					android.text.format.DateFormat.format("dd", today) + ", " +
					android.text.format.DateFormat.format("yyyy", today));
			updatedAt.setText(android.text.format.DateFormat.format("MMM", today) + " " + 
					android.text.format.DateFormat.format("dd", today) + ", " +
					android.text.format.DateFormat.format("yyyy", today));
			// disable replay button!
			replayButtonBar.setVisibility(View.GONE);
	    	replayButtonPanel.setVisibility(View.GONE);
		}
				
		// recorder-related begins
		
		
		
		recorderButton.setOnClickListener(new OnClickListener() {
		    @Override
		    public void onClick(View v) {
		    	if(recordstatus==0) {
					Toast.makeText(ActivityScanned.this, "Recording...", Toast.LENGTH_SHORT).show();
					// changebuttontext(R.id.recordButton,"Recording...");
					replayButtonBar.setVisibility(View.GONE);
                	replayButtonPanel.setVisibility(View.GONE);
		            startRecording();
		            recordstatus=1;
		            recorderButton.setImageResource(R.drawable.ic_action_stop);
		            
		            findViewById(R.id.timer).setVisibility(View.VISIBLE);
		    		scrollView = (MyScrollView) findViewById(R.id.scroll_view_scanned);
		    		scrollView.setmScrollable(false);
		    		disableViewElements((ViewGroup) findViewById(R.id.backlayer));
		    		gridView.setEnabled(false);
					
		             t = new CountDownTimer( 30000, 1000) {           //30 seconds recording time
		            	 TextView counter=(TextView) findViewById(R.id.time_left);
		            	 
		                    @Override
		                    public void onTick(long millisUntilFinished) {
		                    	counter.setText(millisUntilFinished / 1000 +" seconds remaining.");
		                    }
		                    @Override
		                    public void onFinish() {   
		                    	stopRecording();
		                    	recordstatus=0;
		                    	Toast.makeText(ActivityScanned.this, "Max Recording Length Reached.", Toast.LENGTH_SHORT).show();
		                    	recorderButton.setImageResource(R.drawable.recorder);
		                    	replayButtonBar.setVisibility(View.VISIBLE);
		                    	replayButtonPanel.setVisibility(View.VISIBLE);
		    		            findViewById(R.id.timer).setVisibility(View.GONE);
		    		    		scrollView = (MyScrollView) findViewById(R.id.scroll_view_scanned);
		    		    		scrollView.setmScrollable(true);
		    		    		gridView.setEnabled(true);
		    		    		enableViewElements((ViewGroup) findViewById(R.id.backlayer));
		                    }
		                }.start();
					
		        } else if (recordstatus==1) {
		            stopRecording();
		            t.cancel();
		            recordstatus=0;
		            recorderButton.setImageResource(R.drawable.recorder);
		            replayButtonBar.setVisibility(View.VISIBLE);
                	replayButtonPanel.setVisibility(View.VISIBLE);
		            findViewById(R.id.timer).setVisibility(View.GONE);
		    		scrollView = (MyScrollView) findViewById(R.id.scroll_view_scanned);
		    		scrollView.setmScrollable(true);
		    		gridView.setEnabled(true);
		    		enableViewElements((ViewGroup) findViewById(R.id.backlayer));
		        }
		    }
		});
		
		timerButton.setOnClickListener(new OnClickListener() {
		    @Override
		    public void onClick(View v) {

		            stopRecording();
		            t.cancel();
		            recordstatus=0;
		            recorderButton.setImageResource(R.drawable.recorder);
		            replayButtonBar.setVisibility(View.VISIBLE);
                	replayButtonPanel.setVisibility(View.VISIBLE);
		            findViewById(R.id.timer).setVisibility(View.GONE);
		            enableViewElements((ViewGroup) findViewById(R.id.backlayer));
		    		scrollView = (MyScrollView) findViewById(R.id.scroll_view_scanned);
		    		scrollView.setmScrollable(true);
		    		gridView.setEnabled(true);
		    }
		});
		replayButtonBar.setOnClickListener(new OnClickListener() {
		    @Override
		    public void onClick(View v) {
		    		if (null!=mp && mp.isPlaying()) {
		                mp.pause();
		                flag=1;
		                replayButtonBar.setImageResource(R.drawable.play);
		                replayButtonPanel.setImageResource(R.drawable.play);
		            } else if (null!=mp && flag==1){
		            	mp.start(); 
		            	flag=0;
		            } else {
			    		stopRecording();
			    		mp = new MediaPlayer();
		            	replayButtonBar.setImageResource(R.drawable.pause);
		                replayButtonPanel.setImageResource(R.drawable.pause);
			    		try {
							mp.setDataSource(filepath);
						} catch (IllegalArgumentException e) {
							e.printStackTrace();
						} catch (SecurityException e) {
							e.printStackTrace();
						} catch (IllegalStateException e) {
							e.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						}
			    		try {
							mp.prepare();
						} catch (IllegalStateException | IOException e) {
							e.printStackTrace();
						}
			    		mp.start();
			    		
			    		mp.setOnCompletionListener(new OnCompletionListener() {        
					        //@Override
					        public void onCompletion(MediaPlayer mp) {
				            	replayButtonBar.setImageResource(R.drawable.play);
				                replayButtonPanel.setImageResource(R.drawable.play);
						    }
						});
		            }
		    	} 
		});
		replayButtonPanel.setOnClickListener(new OnClickListener() {
		    @Override
		    public void onClick(View v) {
		    		if (null!=mp && mp.isPlaying()) {
		                mp.pause();
		                flag=1;
		                replayButtonBar.setImageResource(R.drawable.play);
		                replayButtonPanel.setImageResource(R.drawable.play);
		            } else if (null!=mp && flag==1){
		            	mp.start(); 
		            	flag=0;
		            	replayButtonBar.setImageResource(R.drawable.pause);
		                replayButtonPanel.setImageResource(R.drawable.pause);
		            } else {
			    		stopRecording();
			    		mp = new MediaPlayer();
			    		try {
							mp.setDataSource(filepath);
						} catch (IllegalArgumentException e) {
							e.printStackTrace();
						} catch (SecurityException e) {
							e.printStackTrace();
						} catch (IllegalStateException e) {
							e.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						}
			    		try {
							mp.prepare();
						} catch (IllegalStateException | IOException e) {
							e.printStackTrace();
						}
			    		mp.start();
		            	replayButtonBar.setImageResource(R.drawable.pause);
		                replayButtonPanel.setImageResource(R.drawable.pause);
			    		
			    		mp.setOnCompletionListener(new OnCompletionListener() {        
					        //@Override
					        public void onCompletion(MediaPlayer mp) {
				            	replayButtonBar.setImageResource(R.drawable.play);
				                replayButtonPanel.setImageResource(R.drawable.play);
						    }
						});
		            }
		    }
		});
		// recorder-related ends

		// This is the life-saver! It fixes the bug that scrollView will go to the
		// bottom of GridView upon open
		// below is to re-scroll to the first view in the LinearLayout
		SquareLayout mainCardContainer = (SquareLayout) findViewById(R.id.main_card_container);
		scrollView.requestChildFocus(mainCardContainer, null);

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

	private void deleteLocalVoiceNote() {
		File myFile = new File(filepath);
		if(myFile.exists())
		    myFile.delete();
	}
	
	public void onBackPressed(){
		super.onBackPressed();
		deleteLocalVoiceNote();
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
			deleteLocalVoiceNote();
			this.finish();
			return true;
		case R.id.scanned_save:
			// save scanned card either online or cache it offline
			if (ECardUtils.isNetworkAvailable(this)) {
				// if ActivityScanned started out as online and now it's still online, go ahead and add
				final AsyncTasks.AddCardNetworkAvailable addNewCard = new AsyncTasks.AddCardNetworkAvailable(this, currentUser, scannedUser.getObjId(), deletedNoteId);
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
				// if ActivityScanned started out as offline, it means there was no check on ecard existence or collected, cache it for later check
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
		EditText whereMet = (EditText) findViewById(R.id.PlaceAdded2);
		EditText eventMet = (EditText) findViewById(R.id.EventAdded2);
		EditText notes = (EditText) findViewById(R.id.EditNotes);
		if (olDatas.size() == 0) {
			// if EcardID is not among local db records, cache it
			
			File file = new File(filepath);			
			if(file.exists()){
				// if there is voice note to be cached, rename it for later use
				String newFilepath = getUniqueFilename();
				File newFile = new File(newFilepath);
			    file.renameTo(newFile);
			    file.delete();		
				db.addData(new OfflineData(scannedId, whereMet.getText().toString(), eventMet.getText().toString(), notes.getText().toString(), newFilepath));
			} else{
				// no voice note to save
				db.addData(new OfflineData(scannedId, whereMet.getText().toString(), eventMet.getText().toString(), notes.getText().toString(), "null"));
			}			
			Toast.makeText(getBaseContext(), "Ecard cached, will add when next time connect to internet", Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(getBaseContext(), "Already in local queue, but still cached!", Toast.LENGTH_SHORT).show();
			// flip the flag, give it a chance to be revisited
			olDatas.get(0).setStored(0);
			db.updataData(olDatas.get(0));
		}
	}

	public void displayCard(UserInfo newUser) {

		TextView name = (TextView) findViewById(R.id.my_name);
		String tmpString = newUser.getFirstName();
		String nameString = null;
		if (tmpString != null)
			nameString = tmpString;
		tmpString = newUser.getLastName();
		if (tmpString != null)
			nameString = nameString + " " + tmpString;
		if(nameString != null)
			name.setText(nameString);
		name = (TextView) findViewById(R.id.my_com);
		tmpString = newUser.getCompany();
		if (tmpString != null)
			name.setText(tmpString);
		name = (TextView) findViewById(R.id.my_job_title);
		tmpString = newUser.getTitle();
		if (tmpString != null)
			name.setText(tmpString);
		name = (TextView) findViewById(R.id.my_add);
		tmpString = newUser.getCity();
		if (tmpString != null)
			name.setText(tmpString);
		ImageView portraitImg = (ImageView) findViewById(R.id.my_portrait);
		if (newUser.getPortrait() != null) {
			portraitImg.setImageBitmap(newUser.getPortrait());
		}
		ImageView logoImg = (ImageView) findViewById(R.id.my_logo);
		if (newUser.getPortrait() != null){
			logoImg.setImageResource(R.drawable.testlogo1);
		}

	}
		
	
	
	private void stopRecording() {
	    if (null != recorder) {
	    	 try{ 
					recorder.stop();
				 } 
	    	 catch (IllegalStateException e) {
	    		 File mfile= new File(filepath);
	    		 mfile.delete();
	    		 Toast.makeText(ActivityScanned.this, "Recording failed, please try again.", Toast.LENGTH_SHORT).show();
				 }
	    	 catch (RuntimeException e){
	    		 File mfile= new File(filepath);
	    		 mfile.delete();
	    		 Toast.makeText(ActivityScanned.this, "Recording failed, please try again.", Toast.LENGTH_SHORT).show();
	    	 }
	    	 finally{
		        recorder.reset();
		        recorder.release();
		        recorder = null;
	    	 }
	    }
	}
	private void startRecording() {
		 if (null != mp) {
			 try{ 
				mp.stop();

			 } catch (IllegalStateException e) {
				 e.printStackTrace();
			 }
				mp.reset();
				mp.release();
				mp=null;

		 }
	    recorder = new MediaRecorder();
	    recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
	    recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
	    recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
	
	    recorder.setOutputFile(filepath);
	    recorder.setOnErrorListener(errorListener);
	    recorder.setOnInfoListener(infoListener);
	    try {
	        recorder.prepare();
	        recorder.start();
	    } catch (IllegalStateException e) {
	        e.printStackTrace();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}
	
	private String getFilename() {
	    String filepath = Environment.getExternalStorageDirectory().getPath();
	    File file = new File(filepath, AUDIO_RECORDER_FOLDER);
	    if (!file.exists()) {
	        file.mkdirs();
	    }
	    return (file.getAbsolutePath() + "/voicenote.mp4");
	}
	
	private String getUniqueFilename() {
	    String filepath = Environment.getExternalStorageDirectory().getPath();
	    File file = new File(filepath, AUDIO_RECORDER_FOLDER);
	    if (!file.exists()) {
	        file.mkdirs();
	    }
	    return (file.getAbsolutePath() + "/" + System.currentTimeMillis() + ".mp4");
	}
	
	private MediaRecorder.OnErrorListener errorListener = new MediaRecorder.OnErrorListener() {
	    @Override
	    public void onError(MediaRecorder mr, int what, int extra) {
	        Toast.makeText(ActivityScanned.this, "Error: " + what + ", " + extra, Toast.LENGTH_SHORT).show();
	    }
	};
	
	private MediaRecorder.OnInfoListener infoListener = new MediaRecorder.OnInfoListener() {
	    @Override
	    public void onInfo(MediaRecorder mr, int what, int extra) {
	        Toast.makeText(ActivityScanned.this, "Warning: " + what + ", " + extra, Toast.LENGTH_SHORT).show();
	    }
	};
	  protected void disableViewElements(ViewGroup container) {
		   for (int i = 0; i < container.getChildCount();  i++) {
		     if(container.getChildAt(i) instanceof ViewGroup ) {
		         disableViewElements((ViewGroup) container.getChildAt(i));
		     }
		     else {
		       View view = container.getChildAt(i);
		       view.setEnabled(false);
		     }
		   }
		}
	  protected void enableViewElements(ViewGroup container) {
		   for (int i = 0; i < container.getChildCount();  i++) {
		     if(container.getChildAt(i) instanceof ViewGroup ) {
		         enableViewElements((ViewGroup) container.getChildAt(i));
		     }
		     else {
		       View view = container.getChildAt(i);
		       view.setEnabled(true);
		     }
		   }
		}
	  
	@Override
	public void onPause( ) {
		super.onPause();
		 if (null != mp) {
			 	mp.pause();
		 }
	}
	@Override
	public void onDestroy( ) {
		super.onDestroy();
		 if (null != mp) {
		mp.stop();
        mp.release();

    
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
		Toast.makeText(this, output, Toast.LENGTH_SHORT).show();
		// save the obtained cityName to global variable to be passed to ActivityNotes
		whereMet = output;
		TextView whereMet1 = (TextView) findViewById(R.id.PlaceAdded1);
		EditText whereMet2 = (EditText) findViewById(R.id.PlaceAdded2);
		if(whereMet != null) {
			whereMet1.setText(whereMet);
			whereMet2.setText(whereMet);
		}
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
