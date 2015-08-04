package com.micklestudios.knowell;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ToxicBakery.viewpager.transforms.FlipHorizontalTransformer;
import com.micklestudios.knowell.infrastructure.UserInfo;
import com.micklestudios.knowell.utils.AppGlobals;
import com.micklestudios.knowell.utils.DetailsPagerAdapter;
import com.micklestudios.knowell.utils.ECardUtils;
import com.micklestudios.knowell.utils.MyScrollView;
import com.micklestudios.knowell.utils.MyViewPager;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

public class ActivityDetails extends ActionBarActivity {

  private MyScrollView scrollView;
  ParseUser currentUser;
  private MediaRecorder recorder = null;
  private MediaPlayer mp = null;
  ImageView replayButtonPanel;
  ImageView recorderButton;
  ImageView timerButton;
  static final String AUDIO_RECORDER_FOLDER = "AudioRecorder";
  static final long SAVENOTE_TIMEOUT = 5000;
  CountDownTimer t;
  int flag = 0;
  String filepath;
  String noteId;
  int recordstatus1 = 0;
  public Date newDate;
  private boolean flagVoiceNoteChanged = false;
  private UserInfo newUser;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    currentUser = ParseUser.getCurrentUser();
    // show custom action bar (on top of standard action bar)
    showActionBar();
    setContentView(R.layout.activity_scanned);

    replayButtonPanel = (ImageView) findViewById(R.id.panel_play_button);
    recorderButton = (ImageView) findViewById(R.id.panel_recorder_button);
    timerButton = (ImageView) findViewById(R.id.stop_recording);
    filepath = getFilename();

    scrollView = (MyScrollView) findViewById(R.id.scroll_view_scanned);
    scrollView.setmScrollable(true);

    Bundle data = getIntent().getExtras();
    if (data != null) {
      newUser = (UserInfo) data.getParcelable("userinfo");
    }

    // This fixes the lost data/ crash issues upon restoring from resume
    if (savedInstanceState != null) {
      newUser = savedInstanceState.getParcelable("newUser");
    }

    TextView motto = (TextView) findViewById(R.id.motto);
    String tmpString = newUser.getMotto();
    if (tmpString != null)
      motto.setText(tmpString);

    DetailsPagerAdapter mAdapter = new DetailsPagerAdapter(
      getSupportFragmentManager(), newUser, this);

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
          if (objects != null && objects.size() != 0) {
            noteId = objects.get(0).getObjectId().toString();
            displayNote(objects.get(0));
          }
        } else {
          e.printStackTrace();
        }
      }

    });

    // recorder-related begins
    recorderButton.setOnClickListener(recorderListener);
    timerButton.setOnClickListener(timerListener);
    replayButtonPanel.setOnClickListener(replayListener);
    // recorder-related ends

    // This is the life-saver! It fixes the bug that scrollView will go to the
    // bottom of GridView upon open
    // below is to re-scroll to the first view in the LinearLayout
    // SquareLayout mainCardContainer = (SquareLayout)
    // findViewById(R.id.details_maincard_container);
    // scrollView.requestChildFocus(mainCardContainer, null);

  }

  OnClickListener replayListener = new OnClickListener() {
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
  };

  OnClickListener timerListener = new OnClickListener() {
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
  };

  OnClickListener recorderListener = new OnClickListener() {
    @Override
    public void onClick(View v) {
      if (recordstatus1 == 0) {
        flagVoiceNoteChanged = true;
        Toast
          .makeText(ActivityDetails.this, "Recording...", Toast.LENGTH_SHORT)
          .show();
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
            counter.setText(millisUntilFinished / 1000 + " seconds remaining.");
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
  };

  @Override
  public void onSaveInstanceState(Bundle outState) {
    outState.putParcelable("newUser", newUser);
    super.onSaveInstanceState(outState);
  }

  void showActionBar() {
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

  void displayNote(final ParseObject object) {
    TextView updatedAt = (TextView) findViewById(R.id.LastUpdated);
    updatedAt.setText(android.text.format.DateFormat.format("MMM",
      object.getUpdatedAt())
      + " "
      + android.text.format.DateFormat.format("dd", object.getUpdatedAt())
      + ", "
      + android.text.format.DateFormat.format("yyyy", object.getUpdatedAt()));

    TextView whenMet2 = (TextView) findViewById(R.id.DateAdded2);
    whenMet2.setText(android.text.format.DateFormat.format("MMM",
      (Date) object.get("whenMet"))
      + " "
      + android.text.format.DateFormat.format("dd",
        (Date) object.get("whenMet"))
      + ", "
      + android.text.format.DateFormat.format("yyyy",
        (Date) object.get("whenMet")));
    newDate = (Date) object.get("whenMet");
    whenMet2.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {
        Calendar newCalendar = Calendar.getInstance();
        newCalendar.setTime(newDate);
        DatePickerDialog dialog = new DatePickerDialog(ActivityDetails.this,
          new mDateSetListener(), newCalendar.get(Calendar.YEAR), newCalendar
            .get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));
        dialog.setTitle("When did we meet?");
        dialog.show();
      }

    });

    final EditText whereMet2 = (EditText) findViewById(R.id.PlaceAdded2);
    whereMet2.setOnFocusChangeListener(new OnFocusChangeListener() {
      @Override
      public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus) {
          whereMet2.setHint("Eg: Portland, OR");
        } else {
          whereMet2.setHint("Where did we meet?");
        }
      }
    });
    String cityName = object.getString("where_met");
    if (cityName != null && !cityName.isEmpty()) {
      whereMet2.setText(cityName);
    }

    final EditText eventMet = (EditText) findViewById(R.id.EventAdded2);
    eventMet.setOnFocusChangeListener(new OnFocusChangeListener() {
      @Override
      public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus) {
          eventMet.setHint("Eg: Portland Beer Festival");
        } else {
          eventMet.setHint("We met in which event?");
        }
      }
    });
    String eventName = object.getString("event_met");
    if (eventName != null && !eventName.isEmpty()) {
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
        final ParseFile voiceFile = new ParseFile("voicenote.mp4", tmpVoiceData);
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

  class mDateSetListener implements DatePickerDialog.OnDateSetListener {

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear,
      int dayOfMonth) {
      // TODO Auto-generated method stub
      // getCalender();
      int mYear = year;
      int mMonth = monthOfYear;
      int mDay = dayOfMonth;
      newDate = getDate(year, monthOfYear, dayOfMonth);

      TextView whenMet2 = (TextView) findViewById(R.id.DateAdded2);
      whenMet2.setText(android.text.format.DateFormat.format("MMM", newDate)
        + " " + android.text.format.DateFormat.format("dd", newDate) + ", "
        + android.text.format.DateFormat.format("yyyy", newDate));
    }
  }

  public static Date getDate(int year, int month, int day) {
    Calendar cal = Calendar.getInstance();
    cal.set(Calendar.YEAR, year);
    cal.set(Calendar.MONTH, month);
    cal.set(Calendar.DAY_OF_MONTH, day);
    cal.set(Calendar.HOUR_OF_DAY, 0);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);
    return cal.getTime();
  }

  void deleteLocalVoiceNote() {
    File myFile = new File(filepath);
    if (myFile.exists())
      myFile.delete();
  }

  public void onBackPressed() {
    super.onBackPressed();
    deleteLocalVoiceNote();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.details_actionbar, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // this function is called when either action bar icon is tapped
    switch (item.getItemId()) {
    case R.id.details_discard:
      Toast.makeText(this, "Discarded changes in Note!", Toast.LENGTH_SHORT)
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

  private void saveNoteChanges(String noteId) {
    ParseQuery<ParseObject> query = ParseQuery.getQuery("ECardNote");
    query.fromLocalDatastore();
    query.getInBackground(noteId, new GetCallback<ParseObject>() {

      @Override
      public void done(final ParseObject object, ParseException e) {
        if (e == null) {
          if (object != null) {
            if (flagVoiceNoteChanged) {
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
                final ParseFile voiceFile = new ParseFile("voicenote.mp4",
                  bFile);
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
                // if network not available, save voicenote with unique name
                // then
                // record in local database
                Toast.makeText(ActivityDetails.this,
                  "No network, caching voice note", Toast.LENGTH_SHORT).show();
                object.put("tmpVoiceByteArray", bFile);
                // flush sharedpreferences to 1969 so next time app opens with
                // internet, convert the file
                Date currentDate = new Date(0);
                SharedPreferences prefs = getSharedPreferences(
                  AppGlobals.MY_PREFS_NAME, MODE_PRIVATE);
                SharedPreferences.Editor prefEditor = prefs.edit();
                prefEditor.putLong("DateNoteSynced", currentDate.getTime());
                prefEditor.commit();
                saveChangesToParse(object);
              }
            } else {
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
    object.put("whenMet", newDate);
    object.put("where_met", whereMet.getText().toString());
    object.put("event_met", eventMet.getText().toString());
    object.put("notes", notes.getText().toString());

    object.saveEventually();
    Toast.makeText(getBaseContext(), "Save successful", Toast.LENGTH_SHORT)
      .show();
    deleteLocalVoiceNote();
  }

  void stopRecording() {
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

  void startRecording() {
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

  String getFilename() {
    String filepath = Environment.getExternalStorageDirectory().getPath();
    File file = new File(filepath, AUDIO_RECORDER_FOLDER);
    if (!file.exists()) {
      file.mkdirs();
    }
    return (file.getAbsolutePath() + "/voicenote.mp4");
  }

  String getUniqueFilename() {
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
