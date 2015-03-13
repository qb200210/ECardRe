package com.warpspace.ecardv4;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
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
import com.warpspace.ecardv4.utils.AsyncTasks;
import com.warpspace.ecardv4.utils.ECardUtils;
import com.warpspace.ecardv4.utils.MyTag;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class ActivityNotesSearch extends ActionBarActivity {

	ParseUser currentUser;
	private MediaRecorder recorder = null;
	private MediaPlayer mp=null;
	private Button replayButton; 
	private Button recorderButton;
	private static final String AUDIO_RECORDER_FOLDER = "AudioRecorder";
	private static final long SAVENOTE_TIMEOUT = 5000;
	CountDownTimer t;
	private String ecardId;
	private String noteId;
	private String filepath;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.layout_note);
		currentUser = ParseUser.getCurrentUser();
		Bundle b = getIntent().getExtras();
		ecardId = (String) b.get("ecardId");
		replayButton = (Button) findViewById(R.id.replayButton);
		recorderButton = (Button) findViewById(R.id.recordButton);
		filepath=getFilename();
		
		ParseQuery<ParseObject> query = ParseQuery.getQuery("ECardNote");
		query.fromLocalDatastore();
		query.whereEqualTo("userId", currentUser.getObjectId().toString());
		query.whereEqualTo("ecardId", ecardId);
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
				TextView whenMet = (TextView) findViewById(R.id.DateAdded);
				whenMet.setText(android.text.format.DateFormat.format("MMM", object.getCreatedAt()) + " " + 
						android.text.format.DateFormat.format("dd", object.getCreatedAt()) + ", " +
						android.text.format.DateFormat.format("yyyy", object.getCreatedAt()));
				EditText whereMet = (EditText) findViewById(R.id.PlaceAdded);
				String cityName = object.getString("where_met");
				if(cityName != null) {
					whereMet.setText(cityName);
				}
				EditText eventMet = (EditText) findViewById(R.id.EventAdded);
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
					if(ECardUtils.isNetworkAvailable(ActivityNotesSearch.this)){
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
		            	replayButton.setEnabled(false);
		            }
				}
			}
			
		});		
		
		recorderButton.setOnTouchListener(new OnTouchListener() {
		    @Override
		    public boolean onTouch(View v, MotionEvent event) {
		    	if(event.getAction() == MotionEvent.ACTION_DOWN) {
					Toast.makeText(ActivityNotesSearch.this, "Recording...", Toast.LENGTH_LONG).show();
					changebuttontext(R.id.recordButton,"Recording...");
					
		            startRecording();
					
		             t = new CountDownTimer( 10000, 1000) {
		            	 TextView counter=(TextView) findViewById(R.id.Timer);
		                    @Override
		                    public void onTick(long millisUntilFinished) {
		                    	counter.setText("seconds remaining: " + millisUntilFinished / 1000);
		                    }
		                    @Override
		                    public void onFinish() {   
		                    	stopRecording();
		                    	Toast.makeText(ActivityNotesSearch.this, "Max Recording Length Reached.", Toast.LENGTH_SHORT).show();
		    		            changebuttontext(R.id.recordButton,"Hold to speak.");
		    		            counter.setText("10");
		    		            enableButton(R.id.recordButton,false);
		    		            enableButton(R.id.replayButton, true);
		                    }
		                }.start();
					
		            return true;
		        } else if (event.getAction() == MotionEvent.ACTION_UP) {
		            stopRecording();
		            t.cancel();
		            TextView counter=(TextView) findViewById(R.id.Timer);
		            counter.setText("10");
		            changebuttontext(R.id.recordButton,"Hold to speak.");
		            enableButton(R.id.replayButton, true);
		            return true;
		        }
		        else 
				return false;
		    }
		});
		replayButton.setOnTouchListener(new OnTouchListener() {
		    @Override
		    public boolean onTouch(View v, MotionEvent event) {
		    	if(event.getAction() == MotionEvent.ACTION_DOWN) {
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
		    		enableButton(R.id.recordButton,true);
		            
		            return true;
		    	}
		        else 
				return false;
		    }
		});
		
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
						if(ECardUtils.isNetworkAvailable(ActivityNotesSearch.this)){
						    final ParseFile voiceFile = new ParseFile("voicenote.mp4", bFile);
						    voiceFile.saveInBackground(new SaveCallback(){
	
								@Override
								public void done(ParseException arg0) {
									object.put("voiceNotes", voiceFile);
									saveChangesToParse(object);
									Toast.makeText(ActivityNotesSearch.this, "Changes saved!", Toast.LENGTH_SHORT).show();
								}
						    	
						    });
						} else {
							// if network not available, save voicenote with unique name then record in local database
							Toast.makeText(ActivityNotesSearch.this, "No network, caching voice note", Toast.LENGTH_SHORT).show(); 
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
		EditText whereMet = (EditText) findViewById(R.id.PlaceAdded);
		EditText eventMet = (EditText) findViewById(R.id.EventAdded);
		EditText notes = (EditText) findViewById(R.id.EditNotes);
		object.put("where_met", whereMet.getText().toString());
		object.put("event_met", eventMet.getText().toString());
		object.put("notes", notes.getText().toString());		
		
		object.saveEventually();
		Toast.makeText(getBaseContext(), "Save successful", Toast.LENGTH_SHORT).show();
		
	}
	
	private void stopRecording() {
	    if (null != recorder) {
	        recorder.stop();
	        recorder.reset();
	        recorder.release();
	        recorder = null;
	    }
	}
	private void startRecording() {
		 if (null != mp) {
			 mp.stop();
			 mp.reset();
			 mp.release();
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
	
	private void enableButton(int id, boolean status ){
		((Button) findViewById(id)).setEnabled(status);
	}
	
	private String getFilename() {
	    String filepath = Environment.getExternalStorageDirectory().getPath();
	    File file = new File(filepath, AUDIO_RECORDER_FOLDER);
	    if (!file.exists()) {
	        file.mkdirs();
	    }
	    return (file.getAbsolutePath() + "/voicenote.mp4");
	}
		
	private void changebuttontext(int id, String text) {
	    ((Button) findViewById(id)).setText(text);
	}
	
	private MediaRecorder.OnErrorListener errorListener = new MediaRecorder.OnErrorListener() {
	    @Override
	    public void onError(MediaRecorder mr, int what, int extra) {
	        Toast.makeText(ActivityNotesSearch.this, "Error: " + what + ", " + extra, Toast.LENGTH_SHORT).show();
	    }
	};
	
	private MediaRecorder.OnInfoListener infoListener = new MediaRecorder.OnInfoListener() {
	    @Override
	    public void onInfo(MediaRecorder mr, int what, int extra) {
	        Toast.makeText(ActivityNotesSearch.this, "Warning: " + what + ", " + extra, Toast.LENGTH_SHORT).show();
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
