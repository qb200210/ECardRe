package com.micklestudios.knowell;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.ToxicBakery.viewpager.transforms.FlipHorizontalTransformer;
import com.micklestudios.knowell.infrastructure.UserInfo;
import com.micklestudios.knowell.utils.CurvedAndTiled;
import com.micklestudios.knowell.utils.DetailsPagerAdapter;
import com.micklestudios.knowell.utils.ECardUtils;
import com.micklestudios.knowell.utils.ExpandableHeightGridView;
import com.micklestudios.knowell.utils.MyDetailsGridViewAdapter;
import com.micklestudios.knowell.utils.MyGridViewAdapter;
import com.micklestudios.knowell.utils.MyPagerAdapter;
import com.micklestudios.knowell.utils.MyScrollView;
import com.micklestudios.knowell.utils.MyTag;
import com.micklestudios.knowell.utils.MyViewPager;
import com.micklestudios.knowell.utils.SquareLayout;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.micklestudios.knowell.ActivityDetails;
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
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
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
  ParseUser currentUser;
  private MediaRecorder recorder = null;
  private MediaPlayer mp = null;
  private ImageView replayButtonPanel;
  private ImageView recorderButton;
  private ImageView timerButton;
  private static final String AUDIO_RECORDER_FOLDER = "AudioRecorder";
  private static final long SAVENOTE_TIMEOUT = 5000;
  CountDownTimer t;
  private int flag = 0;
  private String filepath;
  private String noteId;
  private int recordstatus1 = 0;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // show custom action bar (on top of standard action bar)
    showActionBar();
    setContentView(R.layout.activity_scanned);
    currentUser = ParseUser.getCurrentUser();

    replayButtonPanel = (ImageView) findViewById(R.id.panel_play_button);
    recorderButton = (ImageView) findViewById(R.id.panel_recorder_button);
    timerButton = (ImageView) findViewById(R.id.stop_recording);
    filepath = getFilename();

    scrollView = (MyScrollView) findViewById(R.id.scroll_view_scanned);
    scrollView.setmScrollable(true);

    Bundle data = getIntent().getExtras();
    final UserInfo newUser = (UserInfo) data.getParcelable("userinfo");
    
    DetailsPagerAdapter mAdapter = new DetailsPagerAdapter(getSupportFragmentManager(), newUser, this);

    final MyViewPager mPager = (MyViewPager) findViewById(R.id.pager22);
    mPager.setAdapter(mAdapter);
    mPager.setCurrentItem(0x40000000);
    mPager.setPageTransformer(true, new FlipHorizontalTransformer());
    
    // display note
    ParseQuery<ParseObject> query = ParseQuery.getQuery("ECardNote");
    query.fromLocalDatastore();
    query.whereEqualTo("userId", currentUser.getObjectId().toString());
    query.whereEqualTo("ecardId", newUser.getObjId());
    query.findInBackground(new FindCallback<ParseObject>() {

      @Override
      public void done(List<ParseObject> objects, ParseException e) {
        if (e == null) {
          if (objects != null) {
            noteId = objects.get(0).getObjectId().toString();
            displayNote(objects.get(0));
          }
        } else {
          e.printStackTrace();
        }
      }

      private void displayNote(final ParseObject object) {
        TextView updatedAt = (TextView) findViewById(R.id.LastUpdated);
        updatedAt.setText(android.text.format.DateFormat.format("MMM",
          object.getUpdatedAt())
          + " "
          + android.text.format.DateFormat.format("dd", object.getUpdatedAt())
          + ", "
          + android.text.format.DateFormat.format("yyyy", object.getUpdatedAt()));
        TextView whenMet2 = (TextView) findViewById(R.id.DateAdded2);
        whenMet2.setText(android.text.format.DateFormat.format("MMM",
          object.getCreatedAt())
          + " "
          + android.text.format.DateFormat.format("dd", object.getCreatedAt())
          + ", "
          + android.text.format.DateFormat.format("yyyy", object.getCreatedAt()));
        EditText whereMet2 = (EditText) findViewById(R.id.PlaceAdded2);
        String cityName = object.getString("where_met");
        if (cityName != null) {
          whereMet2.setText(cityName);
        }
        EditText eventMet = (EditText) findViewById(R.id.EventAdded2);
        String eventName = object.getString("event_met");
        if (eventName != null) {
          eventMet.setText(eventName);
        }
        EditText notes = (EditText) findViewById(R.id.EditNotes);
        String notesContent = object.getString("notes");
        if (notesContent != null) {
          notes.setText(notesContent);
        }
        byte[] tmpVoiceData = (byte[]) object.get("tmpVoiceByteArray");
        if (tmpVoiceData != null) {
          // save as parseFile then clean the array if there is network
          // this is necessary as user can switch on network without restarting
          // app
          if (ECardUtils.isNetworkAvailable(ActivityDetails.this)) {
            final ParseFile voiceFile = new ParseFile("voicenote.mp4",
              tmpVoiceData);
            voiceFile.saveInBackground(new SaveCallback() {

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
          // that is always use tmpArray to overwrite voiceNotes if there is
          // conflict
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
            // if both tmparray and voiceNotes are null, then it means no voice
            // note at all
            replayButtonPanel.setVisibility(View.GONE);
          }
        }
      }

    });

    // recorder-related begins

    recorderButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        if (recordstatus1 == 0) {
          Toast.makeText(ActivityDetails.this, "Recording...",
            Toast.LENGTH_SHORT).show();
          // changebuttontext(R.id.recordButton,"Recording...");
          replayButtonPanel.setVisibility(View.GONE);
          startRecording();
          recordstatus1 = 1;
          recorderButton.setImageResource(R.drawable.ic_action_stop);

          findViewById(R.id.timer).setVisibility(View.VISIBLE);
          scrollView = (MyScrollView) findViewById(R.id.scroll_view_scanned);
          scrollView.setmScrollable(false);

          disableViewElements((ViewGroup) findViewById(R.id.backlayer));

          t = new CountDownTimer(30000, 1000) { // 30 seconds recording time
            TextView counter = (TextView) findViewById(R.id.time_left);

            @Override
            public void onTick(long millisUntilFinished) {
              counter.setText(millisUntilFinished / 1000
                + " seconds remaining.");
            }

            @Override
            public void onFinish() {
              stopRecording();
              recordstatus1 = 0;
              Toast.makeText(ActivityDetails.this,
                "Max Recording Length Reached.", Toast.LENGTH_SHORT).show();
              recorderButton.setImageResource(R.drawable.recorder);
              replayButtonPanel.setVisibility(View.VISIBLE);
              findViewById(R.id.timer).setVisibility(View.GONE);
              enableViewElements((ViewGroup) findViewById(R.id.backlayer));
              scrollView = (MyScrollView) findViewById(R.id.scroll_view_scanned);
              scrollView.setmScrollable(true);

            }
          }.start();

        } else if (recordstatus1 == 1) {
          stopRecording();
          t.cancel();
          recordstatus1 = 0;
          recorderButton.setImageResource(R.drawable.recorder);
          replayButtonPanel.setVisibility(View.VISIBLE);
          findViewById(R.id.timer).setVisibility(View.GONE);
          enableViewElements((ViewGroup) findViewById(R.id.backlayer));
          scrollView = (MyScrollView) findViewById(R.id.scroll_view_scanned);
          scrollView.setmScrollable(true);
        }
      }
    });

    timerButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {

        stopRecording();
        t.cancel();
        recordstatus1 = 0;
        recorderButton.setImageResource(R.drawable.recorder);
        replayButtonPanel.setVisibility(View.VISIBLE);
        findViewById(R.id.timer).setVisibility(View.GONE);
        enableViewElements((ViewGroup) findViewById(R.id.backlayer));
        scrollView = (MyScrollView) findViewById(R.id.scroll_view_scanned);
        scrollView.setmScrollable(true);
      }
    });
    
    replayButtonPanel.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        if (null != mp && mp.isPlaying()) {
          mp.pause();
          flag = 1;
          replayButtonPanel.setImageResource(R.drawable.play);
        } else if (null != mp && flag == 1) {
          mp.start();
          flag = 0;
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
          replayButtonPanel.setImageResource(R.drawable.pause);

          mp.setOnCompletionListener(new OnCompletionListener() {
            // @Override
            public void onCompletion(MediaPlayer mp) {
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
//    SquareLayout mainCardContainer = (SquareLayout) findViewById(R.id.details_maincard_container);
//    scrollView.requestChildFocus(mainCardContainer, null);

  }

  private void showActionBar() {
    LayoutInflater inflator = (LayoutInflater) this
      .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    View v = inflator.inflate(R.layout.layout_actionbar_search, null);
    LinearLayout btnBack = (LinearLayout) v.findViewById(R.id.btn_back);
    TextView title = (TextView) v.findViewById(R.id.search_actionbar_title);
    title.setText("Notes");
    btnBack.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {
        onBackPressed();
      }

    });
    if (getSupportActionBar() != null) {
      ActionBar actionBar = getSupportActionBar();
      actionBar.setDisplayHomeAsUpEnabled(false);
      actionBar.setDisplayShowHomeEnabled(false);
      actionBar.setDisplayShowCustomEnabled(true);
      actionBar.setDisplayShowTitleEnabled(false);
      actionBar.setCustomView(v);
    }
  }

  

  private void deleteLocalVoiceNote() {
    File myFile = new File(filepath);
    if (myFile.exists())
      myFile.delete();
  }

  public void onBackPressed() {
    super.onBackPressed();
    deleteLocalVoiceNote();
  }

  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.details_actionbar, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // this function is called when either action bar icon is tapped
    switch (item.getItemId()) {
    case R.id.details_discard:
      Toast.makeText(this, "Discarded Notes changes!", Toast.LENGTH_SHORT)
        .show();
      setResult(RESULT_CANCELED);
      deleteLocalVoiceNote();
      this.finish();
      return true;
    case R.id.details_save:
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
    if (newUser.getPortrait() != null) {
      portraitImg.setImageBitmap(newUser.getPortrait());
    }
    
    TextView name = (TextView) findViewById(R.id.my_name);
    String tmpString = newUser.getFirstName();
    String nameString = null;
    if (tmpString != null)
      nameString = tmpString;
    tmpString = newUser.getLastName();
    if (tmpString != null)
      nameString = nameString + " " + tmpString;
    if (nameString != null)
      name.setText(nameString);
    name = (TextView) findViewById(R.id.my_com);
    tmpString = newUser.getCompany();    
    if (tmpString != null) {
      name.setText(tmpString);
      ImageView logoImg = (ImageView) findViewById(R.id.my_logo);
      // display logo
      ECardUtils.findAndSetLogo(this, logoImg, tmpString, false);
    }
    
    name = (TextView) findViewById(R.id.my_job_title);
    tmpString = newUser.getTitle();
    if (tmpString != null)
      name.setText(tmpString);
    name = (TextView) findViewById(R.id.my_add);
    tmpString = newUser.getCity();
    if (tmpString != null)
      name.setText(tmpString);

  }

  private void saveNoteChanges(String noteId) {
    ParseQuery<ParseObject> query = ParseQuery.getQuery("ECardNote");
    query.fromLocalDatastore();
    query.getInBackground(noteId, new GetCallback<ParseObject>() {

      @Override
      public void done(final ParseObject object, ParseException e) {
        if (e == null) {
          if (object != null) {
            FileInputStream fileInputStream = null;
            File file = new File(filepath);
            byte[] bFile = new byte[(int) file.length()];
            // convert file into array of bytes
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
            if (ECardUtils.isNetworkAvailable(ActivityDetails.this)) {
              final ParseFile voiceFile = new ParseFile("voicenote.mp4", bFile);
              voiceFile.saveInBackground(new SaveCallback() {

                @Override
                public void done(ParseException arg0) {
                  object.put("voiceNotes", voiceFile);
                  saveChangesToParse(object);
                  Toast.makeText(ActivityDetails.this, "Changes saved!",
                    Toast.LENGTH_SHORT).show();
                }

              });
            } else {
              // if network not available, save voicenote with unique name then
              // record in local database
              Toast.makeText(ActivityDetails.this,
                "No network, caching voice note", Toast.LENGTH_SHORT).show();
              object.put("tmpVoiceByteArray", bFile);
              // flush sharedpreferences to 1969 so next time app opens with
              // internet, convert the file
              Date currentDate = new Date(0);
              SharedPreferences prefs = getSharedPreferences(
                ActivityBufferOpening.MY_PREFS_NAME, MODE_PRIVATE);
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
    Toast.makeText(getBaseContext(), "Save successful", Toast.LENGTH_SHORT)
      .show();
    deleteLocalVoiceNote();
  }

  private void stopRecording() {
    if (null != recorder) {
      try {
        recorder.stop();
      } catch (IllegalStateException e) {
        File mfile = new File(filepath);
        mfile.delete();
        Toast.makeText(ActivityDetails.this,
          "Recording failed, please try again.", Toast.LENGTH_SHORT).show();
      } catch (RuntimeException e) {
        File mfile = new File(filepath);
        mfile.delete();
        Toast.makeText(ActivityDetails.this,
          "Recording failed, please try again.", Toast.LENGTH_SHORT).show();
      } finally {
        recorder.reset();
        recorder.release();
        recorder = null;
      }
    }
  }

  private void startRecording() {
    if (null != mp) {
      try {
        mp.stop();

      } catch (IllegalStateException e) {
        e.printStackTrace();
      }
      mp.reset();
      mp.release();
      mp = null;

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
      Toast.makeText(ActivityDetails.this, "Error: " + what + ", " + extra,
        Toast.LENGTH_SHORT).show();
    }
  };

  private MediaRecorder.OnInfoListener infoListener = new MediaRecorder.OnInfoListener() {
    @Override
    public void onInfo(MediaRecorder mr, int what, int extra) {
      Toast.makeText(ActivityDetails.this, "Warning: " + what + ", " + extra,
        Toast.LENGTH_SHORT).show();
    }
  };

  protected void disableViewElements(ViewGroup container) {
    for (int i = 0; i < container.getChildCount(); i++) {
      if (container.getChildAt(i) instanceof ViewGroup) {
        disableViewElements((ViewGroup) container.getChildAt(i));
      } else {
        View view = container.getChildAt(i);
        view.setEnabled(false);
      }
    }
  }

  protected void enableViewElements(ViewGroup container) {
    for (int i = 0; i < container.getChildCount(); i++) {
      if (container.getChildAt(i) instanceof ViewGroup) {
        enableViewElements((ViewGroup) container.getChildAt(i));
      } else {
        View view = container.getChildAt(i);
        view.setEnabled(true);
      }
    }
  }

  @Override
  public void onPause() {
    super.onPause();
    if (null != mp) {
      mp.pause();
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (null != mp) {
      mp.stop();
      mp.release();

    }

  }
}
