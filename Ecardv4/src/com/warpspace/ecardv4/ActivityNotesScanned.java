package com.warpspace.ecardv4;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

import com.parse.ParseUser;
import com.warpspace.ecardv4.R;

import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class ActivityNotesScanned extends ActionBarActivity {

	ParseUser currentUser;
	private MediaRecorder recorder = null;
	private MediaPlayer mp=null;
	private String mostrecentfile;
	private static final String AUDIO_RECORDER_FOLDER = "AudioRecorder";
	private int flag=0;
	CountDownTimer t;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.layout_note);
		currentUser = ParseUser.getCurrentUser();
		
		Bundle data = getIntent().getExtras();
		String whereMet =(String) data.get("whereMet");
		if(whereMet !=null && whereMet !="nolocation"){
		  Toast.makeText(getBaseContext(), "Location: "+whereMet, Toast.LENGTH_SHORT).show();
		}
		
		TextView dateAdded = (TextView) findViewById(R.id.DateAdded);
		TextView cityAdded = (TextView) findViewById(R.id.PlaceAdded);
		String mydate = java.text.DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime());
		dateAdded.setText(mydate);
		cityAdded.setText(whereMet);
		
		Button recorderButton = (Button) findViewById(R.id.recordButton);
		Button replayButton = (Button) findViewById(R.id.replayButton);
		replayButton.setEnabled(false);

		recorderButton.setOnTouchListener(new OnTouchListener() {
		    @Override
		    public boolean onTouch(View v, MotionEvent event) {
		    	if(event.getAction() == MotionEvent.ACTION_DOWN) {
					Toast.makeText(ActivityNotesScanned.this, "Recording...", Toast.LENGTH_SHORT).show();
					changebuttontext(R.id.recordButton,"Recording...");
					enableButton(R.id.replayButton,false);
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
		                    	Toast.makeText(ActivityNotesScanned.this, "Max Recording Length Reached.", Toast.LENGTH_SHORT).show();
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
				return true;
		    }
		});
		replayButton.setOnTouchListener(new OnTouchListener() {
		    @Override
		    public boolean onTouch(View v, MotionEvent event) {
		    	if(event.getAction() == MotionEvent.ACTION_DOWN) {
		    		if (null!=mp && mp.isPlaying()) {
		                mp.pause();
		                flag=1;
		                changebuttontext(R.id.replayButton,"Paused");
		            } 
		    		else if (null!=mp && flag==1){mp.start(); flag=0;changebuttontext(R.id.replayButton,"Playing");}
		            else {
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
		    		changebuttontext(R.id.replayButton,"Playing");
		    		
		    		mp.setOnCompletionListener(new OnCompletionListener() {        
				        //@Override
				        public void onCompletion(MediaPlayer mp) {
				        	changebuttontext(R.id.replayButton,"Replay");
				    }
				});
		            }
		            return true;
		    	}
		    	
		        else 
				return false;
		    }
		});
		
	}
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.notes_scanned_actionbar, menu);
		return true;
	}
	
	//Bo, need your help to save those info into our database, when user clicks on "save" button in the menu
	
	private void stopRecording() {
	    if (null != recorder) {
	    	 try{ 
					recorder.stop();
				 } 
	    	 catch (IllegalStateException e) {
	    		 File mfile= new File(mostrecentfile);
	    		 mfile.delete();
	    		 Toast.makeText(ActivityNotesScanned.this, "Recording failed, please try again.", Toast.LENGTH_SHORT).show();
				 }
	    	 catch (RuntimeException e){
	    		 File mfile= new File(mostrecentfile);
	    		 mfile.delete();
	    		 Toast.makeText(ActivityNotesScanned.this, "Recording failed, please try again.", Toast.LENGTH_SHORT).show();
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
	        Toast.makeText(ActivityNotesScanned.this, "Error: " + what + ", " + extra, Toast.LENGTH_SHORT).show();
	    }
	};
	
	private MediaRecorder.OnInfoListener infoListener = new MediaRecorder.OnInfoListener() {
	    @Override
	    public void onInfo(MediaRecorder mr, int what, int extra) {
	        Toast.makeText(ActivityNotesScanned.this, "Warning: " + what + ", " + extra, Toast.LENGTH_SHORT).show();
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
