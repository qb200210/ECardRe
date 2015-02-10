package com.warpspace.ecardv4;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.parse.ParseUser;
import com.warpspace.ecardv4.R;
import com.warpspace.ecardv4.infrastructure.UserInfo;
import com.warpspace.ecardv4.utils.AsyncResponse;
import com.warpspace.ecardv4.utils.CurvedAndTiled;
import com.warpspace.ecardv4.utils.ExpandableHeightGridView;
import com.warpspace.ecardv4.utils.GeocoderHelper;
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
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
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
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class ActivityNotes extends ActionBarActivity {

	ParseUser currentUser;
	private MediaRecorder recorder = null;
	private String mostrecentfile;
	private static final String AUDIO_RECORDER_FOLDER = "AudioRecorder";
	
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
		
		Button recorderButton = (Button) findViewById(R.id.recordButton);
		Button replayButton = (Button) findViewById(R.id.replayButton);
		replayButton.setEnabled(false);

		recorderButton.setOnTouchListener(new OnTouchListener() {
		    @Override
		    public boolean onTouch(View v, MotionEvent event) {
		    	if(event.getAction() == MotionEvent.ACTION_DOWN) {
					Toast.makeText(ActivityNotes.this, "Recording...", Toast.LENGTH_LONG).show();
					changebuttontext(R.id.recordButton,"Recording...");
		            startRecording();
		            return true;
		        } else if (event.getAction() == MotionEvent.ACTION_UP) {
		            stopRecording();
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
		    		MediaPlayer mp = new MediaPlayer();
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
		            
		            return true;
		    	}
		        else 
				return false;
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
	        Toast.makeText(ActivityNotes.this, "Error: " + what + ", " + extra, Toast.LENGTH_SHORT).show();
	    }
	};
	
	private MediaRecorder.OnInfoListener infoListener = new MediaRecorder.OnInfoListener() {
	    @Override
	    public void onInfo(MediaRecorder mr, int what, int extra) {
	        Toast.makeText(ActivityNotes.this, "Warning: " + what + ", " + extra, Toast.LENGTH_SHORT).show();
	    }
	};
	
	
}
