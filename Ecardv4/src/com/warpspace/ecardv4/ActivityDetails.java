package com.warpspace.ecardv4;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.warpspace.ecardv4.R;
import com.warpspace.ecardv4.infrastructure.UserInfo;
import com.warpspace.ecardv4.utils.CurvedAndTiled;
import com.warpspace.ecardv4.utils.ECardUtils;
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
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class ActivityDetails extends ActionBarActivity {

	private MyScrollView scrollView;
	ArrayList<String> shownArrayList = new ArrayList<String>();
	ArrayList<Integer> infoIcon = new ArrayList<Integer>();
	ArrayList<String> infoLink = new ArrayList<String>();

	ExpandableHeightGridView gridView;
	ParseUser currentUser;
	private MediaRecorder recorder = null;
	private MediaPlayer mp=null;
	private ImageView replayButtonBar; 
	private ImageView replayButtonPanel; 
	private ImageView recorderButton;
	private static final String AUDIO_RECORDER_FOLDER = "AudioRecorder";
	private static final long SAVENOTE_TIMEOUT = 5000;
	CountDownTimer t;
	private int flag=0;
	private String filepath;
	private String noteId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// show custom action bar (on top of standard action bar)
	    showActionBar();
		setContentView(R.layout.activity_scanned);
		currentUser = ParseUser.getCurrentUser();
		
		replayButtonBar = (ImageView) findViewById(R.id.bar_play_button);
		replayButtonPanel = (ImageView) findViewById(R.id.panel_play_button);
		recorderButton = (ImageView) findViewById(R.id.panel_recorder_button);
		filepath=getFilename();
		
		scrollView = (MyScrollView) findViewById(R.id.scroll_view_scanned);
		scrollView.setmScrollable(true);
		

		Bundle data = getIntent().getExtras();
		final UserInfo newUser = (UserInfo) data.getParcelable("userinfo");
		
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
		displayCard(newUser);
		
		// display extra info
		infoIcon = newUser.getInfoIcon();
		infoLink = newUser.getInfoLink();
		shownArrayList = newUser.getShownArrayList();
		
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
		
		// display note
		ParseQuery<ParseObject> query = ParseQuery.getQuery("ECardNote");
		query.fromLocalDatastore();
		query.whereEqualTo("userId", currentUser.getObjectId().toString());
		query.whereEqualTo("ecardId", newUser.getObjId());
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
				whenMet1.setText(android.text.format.DateFormat.format("MMM", object.getCreatedAt()) + " " + 
						android.text.format.DateFormat.format("dd", object.getCreatedAt()) + ", " +
						android.text.format.DateFormat.format("yyyy", object.getCreatedAt()));
				whenMet2.setText(android.text.format.DateFormat.format("MMM", object.getCreatedAt()) + " " + 
						android.text.format.DateFormat.format("dd", object.getCreatedAt()) + ", " +
						android.text.format.DateFormat.format("yyyy", object.getCreatedAt()));
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
					if(ECardUtils.isNetworkAvailable(ActivityDetails.this)){
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
		
		// recorder-related begins
				
				recorderButton.setOnTouchListener(new OnTouchListener() {
				    @Override
				    public boolean onTouch(View v, MotionEvent event) {
				    	if(event.getAction() == MotionEvent.ACTION_DOWN) {
							Toast.makeText(ActivityDetails.this, "Recording...", Toast.LENGTH_SHORT).show();
							// changebuttontext(R.id.recordButton,"Recording...");
							replayButtonBar.setVisibility(View.GONE);
		                	replayButtonPanel.setVisibility(View.GONE);
				            startRecording();
							
				             t = new CountDownTimer( 30000, 1000) {
				            	 //TextView counter=(TextView) findViewById(R.id.Timer);
				                    @Override
				                    public void onTick(long millisUntilFinished) {
				                    	//counter.setText("seconds remaining: " + millisUntilFinished / 1000);
				                    }
				                    @Override
				                    public void onFinish() {   
				                    	stopRecording();
				                    	Toast.makeText(ActivityDetails.this, "Max Recording Length Reached.", Toast.LENGTH_SHORT).show();
				    		            // changebuttontext(R.id.recordButton,"Hold to speak.");
				    		            //counter.setText("30");
				    		            //enableButton(R.id.recordButton,false);
				                    	replayButtonBar.setVisibility(View.VISIBLE);
				                    	replayButtonPanel.setVisibility(View.VISIBLE);
				                    }
				                }.start();
							
				            return true;
				        } else if (event.getAction() == MotionEvent.ACTION_UP) {
				            stopRecording();
				            t.cancel();
				            //TextView counter=(TextView) findViewById(R.id.Timer);
				            //counter.setText("30");
				            //changebuttontext(R.id.recordButton,"Hold to speak.");
				            replayButtonBar.setVisibility(View.VISIBLE);
		                	replayButtonPanel.setVisibility(View.VISIBLE);
				            return true;
				        }
				        else 
						return true;
				    }
				});
				replayButtonBar.setOnTouchListener(new OnTouchListener() {
				    @Override
				    public boolean onTouch(View v, MotionEvent event) {
				    	if(event.getAction() == MotionEvent.ACTION_DOWN) {
				    		if (null!=mp && mp.isPlaying()) {
				                mp.pause();
				                flag=1;
				                //changebuttontext(R.id.replayButton,"Paused");
				            } else if (null!=mp && flag==1){
				            	mp.start(); 
				            	flag=0;
				            	// changebuttontext(R.id.replayButton,"Playing");
				            } else {
					    		stopRecording();
					    		mp = new MediaPlayer();
					    		try {
									mp.setDataSource(filepath);
								} catch (IllegalArgumentException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								} catch (SecurityException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								} catch (IllegalStateException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
					    		try {
									mp.prepare();
								} catch (IllegalStateException | IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
					    		mp.start();
					    		// enableButton(R.id.recordButton,true);
					    		// changebuttontext(R.id.replayButton,"Playing");
					    		
					    		mp.setOnCompletionListener(new OnCompletionListener() {        
							        //@Override
							        public void onCompletion(MediaPlayer mp) {
							        	// changebuttontext(R.id.replayButton,"Replay");
								    }
								});
				            }
				            return true;
				    	} else {
				    		return false;
				    	}
				    }
				});
				replayButtonPanel.setOnTouchListener(new OnTouchListener() {
				    @Override
				    public boolean onTouch(View v, MotionEvent event) {
				    	if(event.getAction() == MotionEvent.ACTION_DOWN) {
				    		if (null!=mp && mp.isPlaying()) {
				                mp.pause();
				                flag=1;
				                //changebuttontext(R.id.replayButton,"Paused");
				            } else if (null!=mp && flag==1){
				            	mp.start(); 
				            	flag=0;
				            	// changebuttontext(R.id.replayButton,"Playing");
				            } else {
					    		stopRecording();
					    		mp = new MediaPlayer();
					    		try {
									mp.setDataSource(filepath);
								} catch (IllegalArgumentException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								} catch (SecurityException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								} catch (IllegalStateException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
					    		try {
									mp.prepare();
								} catch (IllegalStateException | IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
					    		mp.start();
					    		// enableButton(R.id.recordButton,true);
					    		// changebuttontext(R.id.replayButton,"Playing");
					    		
					    		mp.setOnCompletionListener(new OnCompletionListener() {        
							        //@Override
							        public void onCompletion(MediaPlayer mp) {
							        	// changebuttontext(R.id.replayButton,"Replay");
								    }
								});
				            }
				            return true;
				    	} else {
				    		return false;
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
		getMenuInflater().inflate(R.menu.notes_search_actionbar, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// this function is called when either action bar icon is tapped
		switch (item.getItemId()) {
		case R.id.notes_search_discard:
			Toast.makeText(this, "Discarded Notes changes!", Toast.LENGTH_SHORT).show();
			setResult(RESULT_CANCELED);
			deleteLocalVoiceNote();
			this.finish();
			return true;
		case R.id.notes_search_save:
			// save scanned card either online or cache it offline
			saveNoteChanges(noteId);
			setResult(RESULT_OK);
			this.finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public void displayCard(UserInfo newUser) {

		ImageView portraitImg = (ImageView) findViewById(R.id.my_portrait);
		if (newUser.getPortrait() != null){
			portraitImg.setImageBitmap(newUser.getPortrait());
		}
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
		name = (TextView) findViewById(R.id.my_address);
		tmpString = newUser.getCity();
		if (tmpString != null)
			name.setText(tmpString);

	}
	
	private void saveNoteChanges(String noteId) {
		ParseQuery<ParseObject> query = ParseQuery.getQuery("ECardNote");
		query.fromLocalDatastore();
		query.getInBackground(noteId, new GetCallback<ParseObject>(){

			@Override
			public void done(final ParseObject object, ParseException e) {
				if(e==null){
					if(object != null){										
						FileInputStream fileInputStream=null;						 
				        File file = new File(filepath);				 
				        byte[] bFile = new byte[(int) file.length()];				 				        
				        //convert file into array of bytes
					    try {
							fileInputStream = new FileInputStream(file);
							fileInputStream.read(bFile);
						    fileInputStream.close();
						} catch (FileNotFoundException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						if(ECardUtils.isNetworkAvailable(ActivityDetails.this)){
						    final ParseFile voiceFile = new ParseFile("voicenote.mp4", bFile);
						    voiceFile.saveInBackground(new SaveCallback(){
	
								@Override
								public void done(ParseException arg0) {
									object.put("voiceNotes", voiceFile);
									saveChangesToParse(object);
									Toast.makeText(ActivityDetails.this, "Changes saved!", Toast.LENGTH_SHORT).show();
								}
						    	
						    });
						} else {
							// if network not available, save voicenote with unique name then record in local database
							Toast.makeText(ActivityDetails.this, "No network, caching voice note", Toast.LENGTH_SHORT).show(); 
		                	object.put("tmpVoiceByteArray", bFile);
		                	// flush sharedpreferences to 1969 so next time app opens with internet, convert the file
		                	Date currentDate=new Date(0);
		        			SharedPreferences prefs = getSharedPreferences(ActivityBufferOpening.MY_PREFS_NAME, MODE_PRIVATE);
		        			SharedPreferences.Editor prefEditor = prefs.edit();
							prefEditor.putLong("DateNoteSynced", currentDate.getTime());
							prefEditor.commit();
		                	saveChangesToParse(object);
						}
					}
				} else {
					e.printStackTrace();
				}
			}
			
		});
		
	}
	
	private void saveChangesToParse(ParseObject object) {
		EditText whereMet = (EditText) findViewById(R.id.PlaceAdded2);
		EditText eventMet = (EditText) findViewById(R.id.EventAdded2);
		EditText notes = (EditText) findViewById(R.id.EditNotes);
		object.put("where_met", whereMet.getText().toString());
		object.put("event_met", eventMet.getText().toString());
		object.put("notes", notes.getText().toString());		
		
		object.saveEventually();
		Toast.makeText(getBaseContext(), "Save successful", Toast.LENGTH_SHORT).show();
		deleteLocalVoiceNote();
	}
	
	private void stopRecording() {
	    if (null != recorder) {
	    	 try{ 
					recorder.stop();
				 } 
	    	 catch (IllegalStateException e) {
	    		 File mfile= new File(filepath);
	    		 mfile.delete();
	    		 Toast.makeText(ActivityDetails.this, "Recording failed, please try again.", Toast.LENGTH_SHORT).show();
				 }
	    	 catch (RuntimeException e){
	    		 File mfile= new File(filepath);
	    		 mfile.delete();
	    		 Toast.makeText(ActivityDetails.this, "Recording failed, please try again.", Toast.LENGTH_SHORT).show();
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
	
	private MediaRecorder.OnErrorListener errorListener = new MediaRecorder.OnErrorListener() {
	    @Override
	    public void onError(MediaRecorder mr, int what, int extra) {
	        Toast.makeText(ActivityDetails.this, "Error: " + what + ", " + extra, Toast.LENGTH_SHORT).show();
	    }
	};
	
	private MediaRecorder.OnInfoListener infoListener = new MediaRecorder.OnInfoListener() {
	    @Override
	    public void onInfo(MediaRecorder mr, int what, int extra) {
	        Toast.makeText(ActivityDetails.this, "Warning: " + what + ", " + extra, Toast.LENGTH_SHORT).show();
	    }
	};
	
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
}
