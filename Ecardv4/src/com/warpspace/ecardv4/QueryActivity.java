package com.warpspace.ecardv4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.text.DateFormatSymbols;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.warpspace.ecardv4.R;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class QueryActivity extends FragmentActivity {

  ParseUser currentUser;
  // These are the search criteria for Note and Info. Must match with server
  // side fields
  // Order must be consistent
  List<Integer> idsNote = Arrays.asList(R.id.query_event_met,
    R.id.query_where_met);
  List<Integer> idsCardEdit = Arrays.asList(R.id.query_name,
    R.id.query_job_title);
  List<Integer> idsCardView = Arrays.asList(R.id.query_company_name,
    R.id.query_where_work);
  List<String> fieldsNote = Arrays.asList("event_met_lc", "where_met_lc");
  List<String> fieldsCardEdit = Arrays.asList("fullName", "title_lc");
  List<String> fieldsCardView = Arrays.asList("company_lc", "city_lc");
  private ArrayList<String> ecardIds = new ArrayList<String>();
  private ArrayList<String> returnedIds = new ArrayList<String>();
  static Date date;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // This is to kill this activity if backbutton is pressed from main activity
    // when this activity is not opened yet
    // The reason this is needed is to explicitly kill this activity to avoid
    // infinite loop when pressing back button
    Bundle extras = getIntent().getExtras();
    if (extras != null) {
      boolean killKey = extras.getBoolean("quit");
      if (killKey == true) {
        finish();
      }
    }

    getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
    if (getActionBar() != null) {
      getActionBar().hide();
    }
    setContentView(R.layout.activity_query);

    currentUser = ParseUser.getCurrentUser();

    createCandidateList();

    ImageView backButton = (ImageView) findViewById(R.id.down_button);
    backButton.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {
        onBackPressed();
      }

    });

    ImageView clearButton = (ImageView) findViewById(R.id.clear_query);
    clearButton.setOnClickListener(clearButtonListener);

    Button searchButton = (Button) findViewById(R.id.search_button);
    searchButton.setOnClickListener(searchButtonListener);

    final GestureDetector gesture = new GestureDetector(this,
      new GestureDetector.SimpleOnGestureListener() {

        @Override
        public boolean onDown(MotionEvent e) {
          return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
          float velocityY) {
          if (velocityY > 2500) {
            Log.d("speed", String.valueOf(velocityY));
            onBackPressed();
          }
          return super.onFling(e1, e2, velocityX, velocityY);
        }
      });

    RelativeLayout rl = (RelativeLayout) findViewById(R.id.query_container);
    rl.setOnTouchListener(new OnTouchListener() {

      @Override
      public boolean onTouch(View v, MotionEvent event) {
        return gesture.onTouchEvent(event);
      }

    });
  }

  private void createCandidateList() {
    // Initial search to create list of event_met, where_met, company, and
    // where_work
    ParseQuery<ParseObject> query = ParseQuery.getQuery("ECardNote");
    query.fromLocalDatastore();
    query.whereEqualTo("userId", currentUser.getObjectId());
    query.findInBackground(new FindCallback<ParseObject>() {

      @Override
      public void done(List<ParseObject> objects, ParseException e) {
        if (e == null) {
          if (objects.size() != 0) {
            System.out.println(objects.size());
            ecardIds.clear();
            // The use of TreeSet removes duplicates and also order
            // alphabetically
            TreeSet<String> eventMetList = new TreeSet<String>();
            TreeSet<String> whereMetList = new TreeSet<String>();
            for (Iterator<ParseObject> iter = objects.iterator(); iter
              .hasNext();) {
              ParseObject object = iter.next();
              ecardIds.add((String) object.get("ecardId"));
              if (object.get("event_met") != null) {
                eventMetList.add(object.get("event_met").toString());
              }
              if (object.get("where_met") != null) {
                whereMetList.add(object.get("where_met").toString());
              }
            }
            String[] eventMetRecords = (String[]) eventMetList
              .toArray(new String[0]);
            String[] whereMetRecords = (String[]) whereMetList
              .toArray(new String[0]);

            TextView eventMetView = (TextView) findViewById(R.id.query_event_met);
            buildDiaglog("Event Met", eventMetRecords,
              listenerBuilder(eventMetView, eventMetRecords), eventMetView);
            TextView whereMetView = (TextView) findViewById(R.id.query_where_met);
            buildDiaglog("Where Met", whereMetRecords,
              listenerBuilder(whereMetView, whereMetRecords), whereMetView);

            // 2nd level search in EcardInfo for remaining
            // part of search criteria
            ParseQuery<ParseObject> query = ParseQuery.getQuery("ECardInfo");
            query.fromLocalDatastore();
            query.whereContainedIn("objectId", ecardIds);
            query.findInBackground(new FindCallback<ParseObject>() {

              @Override
              public void done(List<ParseObject> objects, ParseException e) {
                if (e == null) {
                  if (objects.size() != 0) {
                    TreeSet<String> companyList = new TreeSet<String>();
                    TreeSet<String> cityList = new TreeSet<String>();
                    for (Iterator<ParseObject> iter = objects.iterator(); iter
                      .hasNext();) {
                      ParseObject object = iter.next();
                      if (object.get("company") != null) {
                        companyList.add(object.get("company").toString());
                      }
                      if (object.get("city") != null) {
                        cityList.add(object.get("city").toString());
                      }

                      String[] companyNameRecords = (String[]) companyList
                        .toArray(new String[0]);
                      String[] whereWorkRecords = (String[]) cityList
                        .toArray(new String[0]);

                      TextView companyNameView = (TextView) findViewById(R.id.query_company_name);
                      buildDiaglog("Company", companyNameRecords,
                        listenerBuilder(companyNameView, companyNameRecords),
                        companyNameView);
                      TextView whereWorkView = (TextView) findViewById(R.id.query_where_work);
                      buildDiaglog("Where Work", whereWorkRecords,
                        listenerBuilder(whereWorkView, whereWorkRecords),
                        whereWorkView);
                    }
                  }
                }
              }
            });
          }
        }
      }

    });

  }

  private void buildDiaglog(String title, String[] records,
    android.content.DialogInterface.OnClickListener listener, TextView tv) {
    AlertDialog.Builder builder = new AlertDialog.Builder(QueryActivity.this);
    builder.setTitle(title);
    builder.setItems(records, listener);
    builder.setNegativeButton("Cancel", null);
    final AlertDialog actions = builder.create();

    tv.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        actions.show();
      }
    });

  }

  DialogInterface.OnClickListener listenerBuilder(final TextView tv,
    final String[] records) {
    DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        tv.setText(records[which].toString());
      }
    };
    return listener;
  }

  public void showDatePickerDialog(View v) {
    DialogFragment newFragment = new DatePickerFragment();
    newFragment.show(getFragmentManager(), "datePicker");
  }

  public static class DatePickerFragment extends DialogFragment implements
      DatePickerDialog.OnDateSetListener {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
      // Use the current date as the default date in the picker
      final Calendar c = Calendar.getInstance();
      int year = c.get(Calendar.YEAR);
      int month = c.get(Calendar.MONTH);
      int day = c.get(Calendar.DAY_OF_MONTH);

      // Create a new instance of DatePickerDialog and return it
      return new DatePickerDialog(getActivity(), this, year, month, day);
    }

    public void onDateSet(DatePicker view, int year, int month, int day) {
      Log.d("as", String.valueOf(year));
      TextView whenMet = (TextView) getActivity().findViewById(
        R.id.query_when_met);
      Calendar cal = Calendar.getInstance();
      cal.set(Calendar.YEAR, year);
      cal.set(Calendar.MONTH, month);
      cal.set(Calendar.DATE, day);
      date = cal.getTime();
      DateFormatSymbols dfs = new DateFormatSymbols(Locale.getDefault());
      whenMet.setText(dfs.getMonths()[month - 1] + " " + String.valueOf(day)
        + ", " + String.valueOf(year));
    }
  }

  @Override
  public void onBackPressed() {
    // This function has to be overwritten, otherwise QueryActivity will be
    // destroyed instead of being saved to deeper in stack
    Intent intent = new Intent(this, ActivityMain.class);
    // FLAG_ACTIVITY_REORDER_TO_FRONT has to be set, otherwise the Main activity
    // will be restarted, losing all previous state
    intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
    startActivity(intent);
    // still confused by the order of animation. But trial and error gives this
    // working combination
    overridePendingTransition(R.anim.slide_stay, R.anim.slide_out);
  }

  @Override
  protected void onNewIntent(Intent intent) {
    // This function is NOT called when this activity is opened for the first
    // time
    // This function will be called when its instance is already running
    // And the previous activity has brought it to front through
    // FLAG_ACTIVITY_REORDER_TO_FRONT
    super.onNewIntent(intent);
    // This is to kill this activity if backbutton is pressed from main activity
    // when this activity is not opened yet
    // The reason this is needed is to explicitly kill this activity to avoid
    // infinite loop when pressing back button
    Bundle extras = intent.getExtras();
    if (extras != null) {
      boolean killKey = extras.getBoolean("quit");
      if (killKey == true) {
        finish();
      }
    } else {
      // Animation on only when there is no bundle sent (which indicates this
      // time QueryActivity is brought to front normally instead of by
      // backbutton pressed at MainActivity)
      // If not included in if-statement, then animation will be played when app
      // is exiting -- not what we want
      // This animation is needed when opening Query page again from FAB in
      // Lookup page
      overridePendingTransition(R.anim.slide_in, R.anim.slide_stay);
    }
  }

  View.OnClickListener clearButtonListener = new OnClickListener() {

    @Override
    public void onClick(View v) {
      // clear criteria for Note search
      for (Iterator<Integer> iter = idsNote.iterator(); iter.hasNext();) {
        Integer id = iter.next();// resource ids
        TextView textBox = (TextView) findViewById(id);
        textBox.setText(null);
      }
      // clear criteria for Info search
      for (Iterator<Integer> iter = idsCardView.iterator(); iter.hasNext();) {
        Integer id = iter.next();// resource ids
        TextView textBox = (TextView) findViewById(id);
        textBox.setText(null);
      }
      for (Iterator<Integer> iter = idsCardEdit.iterator(); iter.hasNext();) {
        Integer id = iter.next();// resource ids
        EditText textBox = (EditText) findViewById(id);
        textBox.setText(null);
      }
      TextView textBox = (TextView) findViewById(R.id.query_when_met);
      textBox.setText(null);
    }

  };

  View.OnClickListener searchButtonListener = new View.OnClickListener() {

    @Override
    public void onClick(View v) {
      ecardIds.clear();
      returnedIds.clear();
      ParseQuery<ParseObject> query = ParseQuery.getQuery("ECardNote");
      query.fromLocalDatastore();
      query.whereEqualTo("userId", currentUser.getObjectId());
      int i = 0;
      // adding search criteria for Note search
      for (Iterator<Integer> iter = idsNote.iterator(); iter.hasNext();) {
        Integer id = iter.next();// resource ids
        String key = fieldsNote.get(i);
        TextView textBox = (TextView) findViewById(id);
        if (!textBox.getText().toString().isEmpty()) {
          // search based on lower case
          // requires when new EcardInfo/EcardNote created, make a duplicate in
          // lower cases
          query.whereContains(key,
            (textBox.getText().toString().toLowerCase(Locale.ENGLISH)));
        }
        i = i + 1;
      }
      // specifically for time search
      TextView textBox = (TextView) findViewById(R.id.query_when_met);
      if (!textBox.getText().toString().isEmpty()) {
        query.whereGreaterThanOrEqualTo("createdAt", date);
      }

      query.findInBackground(new FindCallback<ParseObject>() {

        @Override
        public void done(List<ParseObject> objects, ParseException e) {
          if (e == null) {
            if (objects.size() != 0) {
              System.out.println(objects.size());
              ecardIds.clear();
              for (Iterator<ParseObject> iter = objects.iterator(); iter
                .hasNext();) {
                ParseObject object = iter.next();
                // collect EcardInfo IDs satisfying Note searches
                // here object is Note object
                ecardIds.add((String) object.get("ecardId"));
              }
              // 2nd level search in EcardInfo for remaining
              // part of search criteria
              ParseQuery<ParseObject> query = ParseQuery.getQuery("ECardInfo");
              query.fromLocalDatastore();
              // restricting search to subset of EcardInfo
              // that satisfied top level search
              query.whereContainedIn("objectId", ecardIds);
              int i = 0;
              // adding search criteria for EcardInfo search, EditText
              for (Iterator<Integer> iter = idsCardEdit.iterator(); iter
                .hasNext();) {
                Integer id = iter.next();
                String key = fieldsCardEdit.get(i);
                EditText textBox = (EditText) findViewById(id);
                if (!textBox.getText().toString().isEmpty()) {
                  query.whereContains(key, textBox.getText().toString()
                    .toLowerCase(Locale.ENGLISH));
                }
                i = i + 1;
              }
              i = 0;
              // adding search criteria for EcardInfo search, TextView
              for (Iterator<Integer> iter = idsCardView.iterator(); iter
                .hasNext();) {
                Integer id = iter.next();
                String key = fieldsCardView.get(i);
                TextView textBox = (TextView) findViewById(id);
                if (!textBox.getText().toString().isEmpty()) {
                  query.whereContains(key, textBox.getText().toString()
                    .toLowerCase(Locale.ENGLISH));
                }
                i = i + 1;
              }
              query.findInBackground(new FindCallback<ParseObject>() {

                @Override
                public void done(List<ParseObject> objects, ParseException e) {
                  if (e == null) {
                    if (objects.size() != 0) {
                      // there are some EcardInfo records matching all search
                      // criteria
                      // find out the corresponding EcardNote by doing one last
                      // search
                      ecardIds.clear();
                      for (Iterator<ParseObject> iter = objects.iterator(); iter
                        .hasNext();) {
                        ParseObject object = iter.next();
                        // collect EcardInfo IDs satisfying Note searches
                        // here object is EcardInfo object
                        ecardIds.add((String) object.getObjectId());
                      }
                      ParseQuery<ParseObject> query = ParseQuery
                        .getQuery("ECardNote");
                      query.fromLocalDatastore();
                      query.whereEqualTo("userId", currentUser.getObjectId());
                      query.whereContainedIn("ecardId", ecardIds);
                      query.findInBackground(new FindCallback<ParseObject>() {

                        @Override
                        public void done(List<ParseObject> objects,
                          ParseException e) {
                          if (e == null) {
                            if (objects.size() != 0) {
                              returnedIds.clear();
                              for (Iterator<ParseObject> iter = objects
                                .iterator(); iter.hasNext();) {
                                ParseObject object = iter.next();
                                // collect EcardNote IDs satisfying all search
                                // criteria here object is Note Object
                                returnedIds.add((String) object.getObjectId());
                              }

                              Intent intent = new Intent(getBaseContext(),
                                ActivityMain.class);
                              Bundle b = new Bundle();
                              // pass the Note ids that satisfied all search
                              // criteria to Lookup_fragment for display
                              intent
                                .setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                              Log.d("Query", returnedIds.toString());
                              b.putStringArrayList("results", returnedIds);
                              intent.putExtras(b);
                              startActivity(intent);
                            } else {
                              Toast.makeText(getBaseContext(),
                                "General Parse Error", Toast.LENGTH_SHORT)
                                .show();
                            }
                          } else {
                            Toast.makeText(getBaseContext(),
                              "General Parse Error", Toast.LENGTH_SHORT).show();
                          }

                        }

                      });

                    } else {
                      Toast.makeText(getBaseContext(),
                        "Search returned nothing!", Toast.LENGTH_SHORT).show();
                    }
                  }

                }

              });

            } else {
              Toast.makeText(getBaseContext(), "Search returned nothing!",
                Toast.LENGTH_SHORT).show();
            }
          } else {
            Toast.makeText(getBaseContext(), "General parse error!",
              Toast.LENGTH_SHORT).show();
          }
        }

      });

    }

  };
}
