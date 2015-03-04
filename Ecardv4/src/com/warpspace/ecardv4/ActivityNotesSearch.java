package com.warpspace.ecardv4;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;

import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.warpspace.ecardv4.R;
import com.warpspace.ecardv4.utils.AsyncTasks;
import com.warpspace.ecardv4.utils.ECardUtils;

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
	private String mostrecentfile;
	private static final String AUDIO_RECORDER_FOLDER = "AudioRecorder";
	private static final long SAVENOTE_TIMEOUT = 5000;
	CountDownTimer t;
	private String ecardId;
	private String noteId;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.layout_note);
		currentUser = ParseUser.getCurrentUser();
		Bundle b = getIntent().getExtras();
		ecardId = (String) b.get("ecardId");
		
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

			private void displayNote(ParseObject object) {
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
//				ParseFile portraitFile = (ParseFile) objects.get(0).get("voiceNotes");
//	            if (portraitFile != null) {
//	              byte[] data = portraitFile.getData();
//	              portrait = BitmapFactory.decodeByteArray(data, 0, data.length);
//	            }
			}
			
		});
		
		
		
		
		
		Button recorderButton = (Button) findViewById(R.id.recordButton);
		Button replayButton = (Button) findViewById(R.id.replayButton);
		replayButton.setEnabled(false);

		recorderButton.setOnTouchListener(new OnTouchListener() {
		    @Override
		    public boolean onTouch(View v, MotionEvent event) {
		    	if(event.getAction() == MotionEvent.ACTION_DOWN) {
					Toast.makeText(ActivityNotesSearch.this, "Recording...", Toast.LENGTH_LONG).show();
					changebuttontext(R.id.recordButton,"Recording...");
					
		            startRecording();
					
		             t = new CountDownTimer( 30000, 1000) {
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
		    		            counter.setText("30");
		    		            enableButton(R.id.recordButton,false);
		    		            enableButton(R.id.replayButton, true);
		                    }
		                }.start();
					
		            return true;
		        } else if (event.getAction() == MotionEvent.ACTION_UP) {
		            stopRecording();
		            t.cancel();
		            TextView counter=(TextView) findViewById(R.id.Timer);
		            counter.setText("30");
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
						mp.setDataSource(mostrecentfile);
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
	
	//Bo, need your help to save those info into our database, when user clicks on "save" button in the menu
	
	private void saveNoteChanges(String noteId) {
		ParseQuery<ParseObject> query = ParseQuery.getQuery("ECardNote");
		query.fromLocalDatastore();
		query.getInBackground(noteId, new GetCallback<ParseObject>(){

			@Override
			public void done(ParseObject object, ParseException e) {
				if(e==null){
					if(object != null){						
						EditText whereMet = (EditText) findViewById(R.id.PlaceAdded);
						EditText eventMet = (EditText) findViewById(R.id.EventAdded);
						EditText notes = (EditText) findViewById(R.id.EditNotes);
						object.put("where_met", whereMet.getText().toString());
						object.put("event_met", eventMet.getText().toString());
						object.put("notes", notes.getText().toString());
						object.saveEventually();
					}
				} else {
					e.printStackTrace();
				}
			}
			
		});
		
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
	    mostrecentfile=getFilename();
	    recorder.setOutputFile(mostrecentfile);
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
	    return (file.getAbsolutePath() + "/" + System.currentTimeMillis() + ".mp4");
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
