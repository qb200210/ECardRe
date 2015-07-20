package com.micklestudios.knowell;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;

import com.micklestudios.knowell.utils.AppGlobals;
import com.micklestudios.knowell.utils.RobotoEditText;
import com.micklestudios.knowell.utils.RobotoTextView;
import com.parse.ParseException;
import com.parse.ParsePush;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ActivityUserSetting extends ActionBarActivity {
  private ParseUser currentUser;
  private AlertDialog uploadDialog;
  private AlertDialog feedbackDialog;
  private PrefsFragment newPrefFrag;
  protected static final int UPLOAD_DOC = 1001;
  protected static final int SEND_FEEDBACK = 1002;
  private static final String KNOWELL_ROOT = "KnoWell";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    // TODO Auto-generated method stub
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_usersetting);

    showActionBar();

    currentUser = ParseUser.getCurrentUser();
    newPrefFrag = new PrefsFragment();
    getFragmentManager().beginTransaction()
      .replace(R.id.preference_container, newPrefFrag).commit();

  }

  public boolean onCreateOptionsMenu(Menu menu) {
    return true;
  }

  @SuppressLint("InflateParams")
  private void showActionBar() {
    LayoutInflater inflator = (LayoutInflater) this
      .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    View v = inflator.inflate(R.layout.layout_actionbar_search, null);
    LinearLayout btnBack = (LinearLayout) v.findViewById(R.id.btn_back);
    TextView title = (TextView) v.findViewById(R.id.search_actionbar_title);
    title.setText("Settings");
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

  public class PrefsFragment extends PreferenceFragment {

    public void setprefDocGreeting(boolean flag) {
      Preference prefDocGreeting = (Preference) findPreference(getString(R.string.prefDocGreeting));
      prefDocGreeting.setEnabled(flag);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
      // TODO Auto-generated method stub
      super.onCreate(savedInstanceState);

      // Load the preferences from an XML resource
      addPreferencesFromResource(R.xml.user_settings);
      Preference button = (Preference) findPreference(getString(R.string.prefLogoutButton));
      button
        .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
          @Override
          public boolean onPreferenceClick(Preference preference) {
            ParseUser.logOut();

            Intent intent = new Intent(ActivityUserSetting.this,
              ActivityMain.class);
            intent.putExtra("finish", true);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP
              | Intent.FLAG_ACTIVITY_CLEAR_TOP); // To clean up all activities
            startActivity(intent);
            ActivityUserSetting.this.finish();
            return true;
          }
        });

      // Set default email greetings
      Preference prefEmailGreeting = (Preference) findPreference(getString(R.string.prefEmailGreeting));
      prefEmailGreeting
        .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
          @Override
          public boolean onPreferenceClick(Preference preference) {

            LayoutInflater inflater = getLayoutInflater();
            final View dialogView = inflater.inflate(
              R.layout.layout_default_msg, null);
            LinearLayout dialogHeader = (LinearLayout) dialogView
              .findViewById(R.id.dialog_header);
            final TextView dialogText = (TextView) dialogView
              .findViewById(R.id.dialog_text);
            TextView dialogTitle = (TextView) dialogView
              .findViewById(R.id.dialog_title);
            dialogHeader.setBackgroundColor(getResources().getColor(
              R.color.blue_extra));
            dialogTitle.setText("Set Default Email");
            final RobotoEditText subjectView = (RobotoEditText) dialogView
              .findViewById(R.id.message_subject);
            final RobotoEditText messageView = (RobotoEditText) dialogView
              .findViewById(R.id.message_body);

            LinearLayout helpBtn = (LinearLayout) dialogView
              .findViewById(R.id.help);
            LinearLayout previewBtn = (LinearLayout) dialogView
              .findViewById(R.id.preview);

            helpBtn.setOnClickListener(new OnClickListener() {

              @Override
              public void onClick(View v) {
                LayoutInflater inflaterTmp = getLayoutInflater();
                View helpView = inflaterTmp.inflate(R.layout.layout_help, null);
                RobotoTextView helpBody = (RobotoTextView) helpView
                  .findViewById(R.id.help_body);

                String rawString = "#myname# or #m#:\n"
                  + ActivityMain.myselfUserInfo.getFirstName()
                  + " "
                  + ActivityMain.myselfUserInfo.getLastName()
                  + "\n#company# or #c#:\n"
                  + ActivityMain.myselfUserInfo.getCompany()
                  + "\n#recipient# or #r#:\nThe name of recipient\n#knowell# or #k#:\nLink to your card.";
                helpBody.setText(rawString, TextView.BufferType.SPANNABLE);
                Spannable str = (Spannable) helpBody.getText();
                setColor(helpBody, rawString, "#myname# or #m#:",
                  Color.parseColor(getString(R.color.indigo_extra)), str);
                setColor(helpBody, rawString, "#company# or #c#:",
                  Color.parseColor(getString(R.color.indigo_extra)), str);
                setColor(helpBody, rawString, "#recipient# or #r#:",
                  Color.parseColor(getString(R.color.indigo_extra)), str);
                setColor(helpBody, rawString, "#knowell# or #k#:",
                  Color.parseColor(getString(R.color.indigo_extra)), str);
                new AlertDialog.Builder(ActivityUserSetting.this)
                  .setView(helpView)
                  .setNeutralButton("OK",
                    new DialogInterface.OnClickListener() {
                      public void onClick(DialogInterface dialog,
                        int whichButton) {

                      }
                    }).setCancelable(true).show();

              }

            });

            previewBtn.setOnClickListener(new OnClickListener() {

              @Override
              public void onClick(View v) {
                LayoutInflater inflaterTmp = getLayoutInflater();
                View helpView = inflaterTmp.inflate(R.layout.layout_preview,
                  null);
                RobotoTextView helpSubject = (RobotoTextView) helpView
                  .findViewById(R.id.preview_subject);
                RobotoTextView helpBody = (RobotoTextView) helpView
                  .findViewById(R.id.preview_body);
                String rawSubject = subjectView.getText().toString();
                String rawBody = messageView.getText().toString();
                String processedSubject = rawSubject.replaceAll(
                  "#r[a-zA-Z0-9]*#", "Recipient");
                processedSubject = processedSubject.replaceAll(
                  "#m[a-zA-Z0-9]*#", ActivityMain.myselfUserInfo.getFirstName()
                    + " " + ActivityMain.myselfUserInfo.getLastName());
                processedSubject = processedSubject.replaceAll(
                  "#c[a-zA-Z0-9]*#", ActivityMain.myselfUserInfo.getCompany());
                processedSubject = processedSubject.replaceAll(
                  "#k[a-zA-Z0-9]*#", getLink());
                String processedBody = rawBody.replaceAll("#r[a-zA-Z0-9]*#",
                  "Recipient");
                processedBody = processedBody.replaceAll("#m[a-zA-Z0-9]*#",
                  ActivityMain.myselfUserInfo.getFirstName() + " "
                    + ActivityMain.myselfUserInfo.getLastName());
                processedBody = processedBody.replaceAll("#c[a-zA-Z0-9]*#",
                  ActivityMain.myselfUserInfo.getCompany());
                processedBody = processedBody.replaceAll("#k[a-zA-Z0-9]*#",
                  getLink());
                helpSubject.setText(processedSubject);
                helpBody.setText(processedBody);
                new AlertDialog.Builder(ActivityUserSetting.this)
                  .setView(helpView)
                  .setNeutralButton("OK",
                    new DialogInterface.OnClickListener() {
                      public void onClick(DialogInterface dialog,
                        int whichButton) {

                      }
                    }).setCancelable(true).show();

              }

            });

            String body = currentUser.getString("emailBody");
            String subject = currentUser.getString("emailSubject");

            if (subject == null || subject.isEmpty()) {
              subjectView.setText("Greetings from #myname#");
            } else {
              subjectView.setText(subject);
            }
            if (body == null || body.isEmpty()) {
              messageView
                .setText("Hi #recipient#,\n\nThis is #myname# from #company#.\n\nIt was great to meet you! Keep in touch! \n\nBest,\n#myname#\n\nPlease accept my business card here: #knowell#");
            } else {
              messageView.setText(body);
            }

            new AlertDialog.Builder(ActivityUserSetting.this)
              .setView(dialogView)
              .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                  String subject = subjectView.getText().toString();
                  String body = messageView.getText().toString();

                  currentUser.put("emailBody", body);
                  currentUser.put("emailSubject", subject);
                  currentUser.saveEventually();
                }
              })
              .setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                  public void onClick(DialogInterface dialog, int whichButton) {

                  }
                }).setCancelable(false).show();
            return true;
          }
        });

      // set default SMS greeting
      Preference prefSmsGreeting = (Preference) findPreference(getString(R.string.prefSMSGreeting));
      prefSmsGreeting
        .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
          @Override
          public boolean onPreferenceClick(Preference preference) {

            LayoutInflater inflater = getLayoutInflater();
            final View dialogView = inflater.inflate(
              R.layout.layout_default_msg, null);
            LinearLayout dialogHeader = (LinearLayout) dialogView
              .findViewById(R.id.dialog_header);
            final TextView dialogText = (TextView) dialogView
              .findViewById(R.id.dialog_text);
            TextView dialogTitle = (TextView) dialogView
              .findViewById(R.id.dialog_title);
            dialogHeader.setBackgroundColor(getResources().getColor(
              R.color.blue_extra));
            dialogTitle.setText("Set Default SMS message");
            final RobotoEditText subjectView = (RobotoEditText) dialogView
              .findViewById(R.id.message_subject);
            final RobotoEditText messageView = (RobotoEditText) dialogView
              .findViewById(R.id.message_body);

            LinearLayout panelSubject = (LinearLayout) dialogView
              .findViewById(R.id.panel_subject);
            panelSubject.setVisibility(View.GONE);
            LinearLayout helpBtn = (LinearLayout) dialogView
              .findViewById(R.id.help);
            LinearLayout previewBtn = (LinearLayout) dialogView
              .findViewById(R.id.preview);

            helpBtn.setOnClickListener(new OnClickListener() {

              @Override
              public void onClick(View v) {
                LayoutInflater inflaterTmp = getLayoutInflater();
                View helpView = inflaterTmp.inflate(R.layout.layout_help, null);
                RobotoTextView helpBody = (RobotoTextView) helpView
                  .findViewById(R.id.help_body);

                String rawString = "#myname# or #m#:\n"
                  + ActivityMain.myselfUserInfo.getFirstName()
                  + " "
                  + ActivityMain.myselfUserInfo.getLastName()
                  + "\n#company# or #c#:\n"
                  + ActivityMain.myselfUserInfo.getCompany()
                  + "\n#recipient# or #r#:\nThe name of recipient\n#knowell# or #k#:\nLink to your card.";
                helpBody.setText(rawString, TextView.BufferType.SPANNABLE);
                Spannable str = (Spannable) helpBody.getText();
                setColor(helpBody, rawString, "#myname# or #m#:",
                  Color.parseColor(getString(R.color.indigo_extra)), str);
                setColor(helpBody, rawString, "#company# or #c#:",
                  Color.parseColor(getString(R.color.indigo_extra)), str);
                setColor(helpBody, rawString, "#recipient# or #r#:",
                  Color.parseColor(getString(R.color.indigo_extra)), str);
                setColor(helpBody, rawString, "#knowell# or #k#:",
                  Color.parseColor(getString(R.color.indigo_extra)), str);
                new AlertDialog.Builder(ActivityUserSetting.this)
                  .setView(helpView)
                  .setNeutralButton("OK",
                    new DialogInterface.OnClickListener() {
                      public void onClick(DialogInterface dialog,
                        int whichButton) {

                      }
                    }).setCancelable(true).show();

              }

            });

            previewBtn.setOnClickListener(new OnClickListener() {

              @Override
              public void onClick(View v) {
                LayoutInflater inflaterTmp = getLayoutInflater();
                View helpView = inflaterTmp.inflate(R.layout.layout_preview,
                  null);
                LinearLayout panelSubject = (LinearLayout) helpView
                  .findViewById(R.id.panel_subject);
                panelSubject.setVisibility(View.GONE);
                RobotoTextView helpBody = (RobotoTextView) helpView
                  .findViewById(R.id.preview_body);
                String rawSubject = subjectView.getText().toString();
                String rawBody = messageView.getText().toString();
                String processedSubject = rawSubject.replaceAll(
                  "#r[a-zA-Z0-9]*#", "Recipient");
                processedSubject = processedSubject.replaceAll(
                  "#m[a-zA-Z0-9]*#", ActivityMain.myselfUserInfo.getFirstName()
                    + " " + ActivityMain.myselfUserInfo.getLastName());
                processedSubject = processedSubject.replaceAll(
                  "#c[a-zA-Z0-9]*#", ActivityMain.myselfUserInfo.getCompany());
                processedSubject = processedSubject.replaceAll(
                  "#k[a-zA-Z0-9]*#", getShortLink());
                String processedBody = rawBody.replaceAll("#r[a-zA-Z0-9]*#",
                  "Recipient");
                processedBody = processedBody.replaceAll("#m[a-zA-Z0-9]*#",
                  ActivityMain.myselfUserInfo.getFirstName() + " "
                    + ActivityMain.myselfUserInfo.getLastName());
                processedBody = processedBody.replaceAll("#c[a-zA-Z0-9]*#",
                  ActivityMain.myselfUserInfo.getCompany());
                processedBody = processedBody.replaceAll("#k[a-zA-Z0-9]*#",
                  getShortLink());
                helpBody.setText(processedBody);
                new AlertDialog.Builder(ActivityUserSetting.this)
                  .setView(helpView)
                  .setNeutralButton("OK",
                    new DialogInterface.OnClickListener() {
                      public void onClick(DialogInterface dialog,
                        int whichButton) {

                      }
                    }).setCancelable(true).show();

              }

            });

            String body = currentUser.getString("smsBody");

            if (body == null || body.isEmpty()) {
              messageView
                .setText("Hi #recipient#, this is #myname# from #company#. Keep in touch! My business card: #knowell#");
            } else {
              messageView.setText(body);
            }

            new AlertDialog.Builder(ActivityUserSetting.this)
              .setView(dialogView)
              .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                  String body = messageView.getText().toString();
                  currentUser.put("smsBody", body);
                  currentUser.saveEventually();
                }
              })
              .setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                  public void onClick(DialogInterface dialog, int whichButton) {

                  }
                }).setCancelable(false).show();
            return true;
          }
        });

      // set default doc greeting
      Preference prefDocGreeting = (Preference) findPreference(getString(R.string.prefDocGreeting));
      if (currentUser.getString("docPath") == null
        || currentUser.getString("docPath").isEmpty()) {
        prefDocGreeting.setEnabled(false);
      } else {
        File file = new File(currentUser.getString("docPath"));
        if (!file.exists()) {
          prefDocGreeting.setEnabled(false);
        } else {
          // If the docPath points to a valid file on device
          prefDocGreeting.setEnabled(true);
        }
      }
      prefDocGreeting
        .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
          @Override
          public boolean onPreferenceClick(Preference preference) {

            LayoutInflater inflater = getLayoutInflater();
            final View dialogView = inflater.inflate(
              R.layout.layout_default_msg, null);
            LinearLayout dialogHeader = (LinearLayout) dialogView
              .findViewById(R.id.dialog_header);
            final TextView dialogText = (TextView) dialogView
              .findViewById(R.id.dialog_text);
            TextView dialogTitle = (TextView) dialogView
              .findViewById(R.id.dialog_title);
            dialogHeader.setBackgroundColor(getResources().getColor(
              R.color.blue_extra));
            dialogTitle.setText("Set Default Document Message");
            final RobotoEditText subjectView = (RobotoEditText) dialogView
              .findViewById(R.id.message_subject);
            final RobotoEditText messageView = (RobotoEditText) dialogView
              .findViewById(R.id.message_body);

            LinearLayout helpBtn = (LinearLayout) dialogView
              .findViewById(R.id.help);
            LinearLayout previewBtn = (LinearLayout) dialogView
              .findViewById(R.id.preview);

            final String docName;
            if (currentUser.getString("docName") != null
              && !currentUser.getString("docName").isEmpty()) {
              docName = currentUser.getString("docName");
            } else {
              docName = "Document Name";
            }

            helpBtn.setOnClickListener(new OnClickListener() {

              @Override
              public void onClick(View v) {
                LayoutInflater inflaterTmp = getLayoutInflater();
                View helpView = inflaterTmp.inflate(R.layout.layout_help, null);
                RobotoTextView helpBody = (RobotoTextView) helpView
                  .findViewById(R.id.help_body);

                String rawString = "#myname# or #m#:\n"
                  + ActivityMain.myselfUserInfo.getFirstName()
                  + " "
                  + ActivityMain.myselfUserInfo.getLastName()
                  + "\n#document# or #d#:\n"
                  + docName
                  + "\n#company# or #c#:\n"
                  + ActivityMain.myselfUserInfo.getCompany()
                  + "\n#recipient# or #r#:\nThe name of recipient\n#knowell# or #k#:\nLink to your card.";
                helpBody.setText(rawString, TextView.BufferType.SPANNABLE);
                Spannable str = (Spannable) helpBody.getText();
                setColor(helpBody, rawString, "#myname# or #m#:",
                  Color.parseColor(getString(R.color.indigo_extra)), str);
                setColor(helpBody, rawString, "#document# or #d#:",
                  Color.parseColor(getString(R.color.indigo_extra)), str);
                setColor(helpBody, rawString, "#company# or #c#:",
                  Color.parseColor(getString(R.color.indigo_extra)), str);
                setColor(helpBody, rawString, "#recipient# or #r#:",
                  Color.parseColor(getString(R.color.indigo_extra)), str);
                setColor(helpBody, rawString, "#knowell# or #k#:",
                  Color.parseColor(getString(R.color.indigo_extra)), str);
                new AlertDialog.Builder(ActivityUserSetting.this)
                  .setView(helpView)
                  .setNeutralButton("OK",
                    new DialogInterface.OnClickListener() {
                      public void onClick(DialogInterface dialog,
                        int whichButton) {

                      }
                    }).setCancelable(true).show();

              }

            });

            previewBtn.setOnClickListener(new OnClickListener() {

              @Override
              public void onClick(View v) {
                LayoutInflater inflaterTmp = getLayoutInflater();
                View helpView = inflaterTmp.inflate(R.layout.layout_preview,
                  null);
                RobotoTextView helpSubject = (RobotoTextView) helpView
                  .findViewById(R.id.preview_subject);
                RobotoTextView helpBody = (RobotoTextView) helpView
                  .findViewById(R.id.preview_body);
                String rawSubject = subjectView.getText().toString();
                String rawBody = messageView.getText().toString();
                String processedSubject = rawSubject.replaceAll(
                  "#r[a-zA-Z0-9]*#", "Recipient");
                processedSubject = processedSubject.replaceAll(
                  "#m[a-zA-Z0-9]*#", ActivityMain.myselfUserInfo.getFirstName()
                    + " " + ActivityMain.myselfUserInfo.getLastName());
                processedSubject = processedSubject.replaceAll(
                  "#d[a-zA-Z0-9]*#", docName);
                processedSubject = processedSubject.replaceAll(
                  "#c[a-zA-Z0-9]*#", ActivityMain.myselfUserInfo.getCompany());
                processedSubject = processedSubject.replaceAll(
                  "#k[a-zA-Z0-9]*#", getLink());
                String processedBody = rawBody.replaceAll("#r[a-zA-Z0-9]*#",
                  "Recipient");
                processedBody = processedBody.replaceAll("#m[a-zA-Z0-9]*#",
                  ActivityMain.myselfUserInfo.getFirstName() + " "
                    + ActivityMain.myselfUserInfo.getLastName());
                processedBody = processedBody.replaceAll("#d[a-zA-Z0-9]*#",
                  docName);
                processedBody = processedBody.replaceAll("#c[a-zA-Z0-9]*#",
                  ActivityMain.myselfUserInfo.getCompany());
                processedBody = processedBody.replaceAll("#k[a-zA-Z0-9]*#",
                  getLink());
                helpSubject.setText(processedSubject);
                helpBody.setText(processedBody);
                new AlertDialog.Builder(ActivityUserSetting.this)
                  .setView(helpView)
                  .setNeutralButton("OK",
                    new DialogInterface.OnClickListener() {
                      public void onClick(DialogInterface dialog,
                        int whichButton) {

                      }
                    }).setCancelable(true).show();

              }

            });

            String body = currentUser.getString("docMsgBody");
            String subject = currentUser.getString("docMsgSubject");

            if (subject == null || subject.isEmpty()) {
              subjectView.setText("Greetings from #myname#");
            } else {
              subjectView.setText(subject);
            }
            if (body == null || body.isEmpty()) {
              messageView
                .setText("Hi #recipient#,\n\nThis is #myname# from #company#.\n\nIt was great to meet you! Please find my #document# in the attachment.\n\nKeep in touch! \n\nBest,\n#myname#\n\nPlease accept my business card here: #knowell#");
            } else {
              messageView.setText(body);
            }

            new AlertDialog.Builder(ActivityUserSetting.this)
              .setView(dialogView)
              .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                  String subject = subjectView.getText().toString();
                  String body = messageView.getText().toString();

                  currentUser.put("docMsgBody", body);
                  currentUser.put("docMsgSubject", subject);
                  currentUser.saveEventually();
                }
              })
              .setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                  public void onClick(DialogInterface dialog, int whichButton) {

                  }
                }).setCancelable(false).show();
            return true;
          }
        });

      // set default doc name
      Preference prefDocName = (Preference) findPreference(getString(R.string.prefDocName));
      prefDocName
        .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
          @Override
          public boolean onPreferenceClick(Preference preference) {

            if (currentUser.getString("docPath") == null
              || currentUser.getString("docPath").isEmpty()) {
              uploadDoc();
            } else {
              File file = new File(currentUser.getString("docPath"));
              if (!file.exists()) {
                uploadDoc();
              } else {
                // when the docPath points to a valid file on device
                LayoutInflater inflater = getLayoutInflater();
                final View dialogView = inflater.inflate(
                  R.layout.layout_change_docname, null);
                LinearLayout dialogHeader = (LinearLayout) dialogView
                  .findViewById(R.id.dialog_header);
                final TextView dialogText = (TextView) dialogView
                  .findViewById(R.id.dialog_text);
                TextView dialogTitle = (TextView) dialogView
                  .findViewById(R.id.dialog_title);
                dialogHeader.setBackgroundColor(getResources().getColor(
                  R.color.blue_extra));
                dialogTitle.setText("Change Uploaded Document Name");
                final RobotoEditText docNameView = (RobotoEditText) dialogView
                  .findViewById(R.id.doc_name);

                final String docName;
                if (currentUser.getString("docName") != null
                  && !currentUser.getString("docName").isEmpty()) {
                  docName = currentUser.getString("docName");
                  docNameView.setText(docName);
                } else {
                  docName = "Document Name";
                }

                new AlertDialog.Builder(ActivityUserSetting.this)
                  .setView(dialogView)
                  .setPositiveButton("Save",
                    new DialogInterface.OnClickListener() {
                      public void onClick(DialogInterface dialog,
                        int whichButton) {

                        currentUser.put("docName", docNameView.getText()
                          .toString());
                        currentUser.saveEventually();
                      }
                    })
                  .setNeutralButton("Replace",
                    new DialogInterface.OnClickListener() {
                      public void onClick(DialogInterface dialog,
                        int whichButton) {
                        uploadDoc();
                      }
                    })
                  .setNegativeButton("Cancel",
                    new DialogInterface.OnClickListener() {
                      public void onClick(DialogInterface dialog,
                        int whichButton) {

                      }
                    }).setCancelable(false).show();
              }
            }
            return true;
          }
        });

      // Set Push toggle
      final CheckBoxPreference pushToggle = (CheckBoxPreference) findPreference(getString(R.string.prefPushNotifications));
      // check sharedpreferences
      SharedPreferences sharedPref = getSharedPreferences(
        AppGlobals.MY_PREFS_NAME, MODE_PRIVATE);
      final SharedPreferences.Editor prefEditor = sharedPref.edit();
      boolean notificationsEnabled = sharedPref.getBoolean("KnoWellPushToggle",
        true);
      if (notificationsEnabled) {
        pushToggle.setChecked(true);
      } else {
        pushToggle.setChecked(false);
      }
      pushToggle
        .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

          @Override
          public boolean onPreferenceChange(Preference preference,
            Object newValue) {
            if ((boolean) newValue) {
              ParsePush.subscribeInBackground(AppGlobals.PUSH_CHANNEL_NAME,
                new SaveCallback() {
                  @Override
                  public void done(ParseException e) {
                    if (e == null) {
                      Log.d("com.parse.push",
                        "successfully subscribed to the broadcast channel.");
                      prefEditor.putBoolean("KnoWellPushToggle", true);
                      prefEditor.commit();
                      pushToggle.setChecked(true);
                    } else {
                      Log
                        .e("com.parse.push", "failed to subscribe for push", e);
                    }
                  }
                });
            } else {
              ParsePush.unsubscribeInBackground(AppGlobals.PUSH_CHANNEL_NAME,
                new SaveCallback() {
                  @Override
                  public void done(ParseException e) {
                    if (e == null) {
                      Log
                        .d("com.parse.push",
                          "successfully unsubscribed from the broadcast channel.");
                      prefEditor.putBoolean("KnoWellPushToggle", false);
                      prefEditor.commit();
                      pushToggle.setChecked(false);
                    } else {
                      Log.e("com.parse.push", "failed to unsubscribe for push",
                        e);
                    }
                  }
                });
            }
            return false;
          }
        });

      // Placeholder for reminder

      // Rating

      // Feedback
      Preference feedback = (Preference) findPreference(getString(R.string.prefFeedBack));
      feedback
        .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

          @Override
          public boolean onPreferenceClick(Preference preference) {
            LayoutInflater inflaterTmp = getLayoutInflater();
            View helpView = inflaterTmp.inflate(R.layout.layout_help, null);
            RobotoTextView helpHeader = (RobotoTextView) helpView
              .findViewById(R.id.help_header);
            RobotoTextView helpBody = (RobotoTextView) helpView
              .findViewById(R.id.help_body);
            helpHeader.setText("Team KnoWell");
            helpBody.setText("We appreciate your feedback!");
            feedbackDialog = new AlertDialog.Builder(ActivityUserSetting.this)
              .setView(helpView)
              .setPositiveButton("Sure", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                  sendFeedback();
                }
              })
              .setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                  public void onClick(DialogInterface dialog, int whichButton) {
                  }
                }).setCancelable(true).show();
            return true;
          }

        });

      // Acknowledgement
      Preference acknowledgement = (Preference) findPreference(getString(R.string.prefAcknowledgement));
      acknowledgement
        .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

          @Override
          public boolean onPreferenceClick(Preference preference) {
            LayoutInflater inflaterTmp = getLayoutInflater();
            View helpView = inflaterTmp.inflate(R.layout.layout_help, null);
            RobotoTextView helpHeader = (RobotoTextView) helpView
              .findViewById(R.id.help_header);
            RobotoTextView helpBody = (RobotoTextView) helpView
              .findViewById(R.id.help_body);
            helpHeader.setText("Acknowledgement");
            helpBody.setText(getString(R.string.prefAckList));
            feedbackDialog = new AlertDialog.Builder(ActivityUserSetting.this)
              .setView(helpView)
              .setNeutralButton("Got it",
                new DialogInterface.OnClickListener() {
                  public void onClick(DialogInterface dialog, int whichButton) {
                  }
                }).setCancelable(true).show();
            return false;
          }

        });

    }

  }

  @SuppressLint("NewApi")
  private void sendFeedback() {

    Intent sendIntent = new Intent(Intent.ACTION_SEND);
    sendIntent.setType("message/rfc822");
    sendIntent.putExtra(Intent.EXTRA_EMAIL,
      new String[] { getString(R.string.prefFeedbackEmail) });
    String msgSubject = "[Feedback] From "
      + ActivityMain.myselfUserInfo.getFirstName() + " "
      + ActivityMain.myselfUserInfo.getLastName();
    sendIntent.putExtra(Intent.EXTRA_SUBJECT, msgSubject);
    startActivityForResult(sendIntent, SEND_FEEDBACK);

  }

  @SuppressLint("NewApi")
  private void uploadDoc() {
    // Get the layout inflater
    LayoutInflater inflater = getLayoutInflater();
    final View dialogView = inflater.inflate(R.layout.layout_upload_doc, null);
    LinearLayout dialogHeader = (LinearLayout) dialogView
      .findViewById(R.id.dialog_header);
    final TextView dialogText = (TextView) dialogView
      .findViewById(R.id.dialog_text);
    TextView dialogTitle = (TextView) dialogView
      .findViewById(R.id.dialog_title);
    dialogHeader
      .setBackgroundColor(getResources().getColor(R.color.blue_extra));
    // Set dialog title and main EditText
    dialogTitle.setText("Upload Document");

    LinearLayout uploadButton = (LinearLayout) dialogView
      .findViewById(R.id.upload_button);
    uploadButton.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {
        showFileChooser();
      }

      private void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
          startActivityForResult(
            Intent.createChooser(intent, "Select a File to Upload"), UPLOAD_DOC);
        } catch (android.content.ActivityNotFoundException ex) {
          // Potentially direct the user to the Market with a Dialog
        }
      }

    });

    uploadDialog = new AlertDialog.Builder(this).setView(dialogView)
      .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int whichButton) {

        }
      }).setCancelable(true).show();

  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    Log.e("res", "" + resultCode);
    switch (requestCode) {
    case UPLOAD_DOC:
      if (resultCode == Activity.RESULT_OK) {
        uploadDialog.dismiss();
        // Get the Uri of the selected file
        Uri uri = data.getData();
        Log.d("uri", "File Uri: " + uri.toString());
        // Get the path
        String path;
        try {
          String srcPath = getPath(ActivityUserSetting.this, uri);
          File srcFile = new File(srcPath);
          String dstPath = Environment.getExternalStorageDirectory().getPath()
            + "/" + KNOWELL_ROOT + "/" + srcFile.getName();
          Log.i("asdf", dstPath);
          File dstFile = new File(dstPath);
          try {
            copyFile(srcFile, dstFile);
            ActivityMain.currentUser.put("docPath", dstPath);
          } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
          // Get the file instance
          //
          // Initiate the upload
          addDocDescription(srcFile.getName());
        } catch (URISyntaxException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
      break;
    case SEND_FEEDBACK:
      feedbackDialog.dismiss();
      LayoutInflater inflaterTmp = getLayoutInflater();
      View helpView = inflaterTmp.inflate(R.layout.layout_help, null);
      RobotoTextView helpHeader = (RobotoTextView) helpView
        .findViewById(R.id.help_header);
      RobotoTextView helpBody = (RobotoTextView) helpView
        .findViewById(R.id.help_body);
      helpHeader.setText("Team KnoWell");
      helpBody
        .setText("Dear "
          + ActivityMain.myselfUserInfo.getFirstName()
          + ",\n\nWe deeply appreciate you providing us your valuable feedback. We will be in touch with you in the next 72 hours.\n\nBest,\nTeam KnoWell");
      new AlertDialog.Builder(ActivityUserSetting.this).setView(helpView)
        .setNeutralButton("OK", new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int whichButton) {

          }
        }).setCancelable(true).show();
      break;
    }

    super.onActivityResult(requestCode, resultCode, data);
  }

  @SuppressLint("NewApi")
  private void addDocDescription(final String filename) {
    // Get the layout inflater
    LayoutInflater inflater = getLayoutInflater();
    final View dialogView = inflater.inflate(R.layout.layout_upload_doc_done,
      null);
    LinearLayout dialogHeader = (LinearLayout) dialogView
      .findViewById(R.id.dialog_header);
    final TextView dialogText = (TextView) dialogView
      .findViewById(R.id.dialog_text);
    TextView dialogTitle = (TextView) dialogView
      .findViewById(R.id.dialog_title);
    // // Set dialog header background with rounded corner
    // Bitmap bm = BitmapFactory
    // .decodeResource(getResources(), R.drawable.striped);
    // BitmapDrawable bmDrawable = new BitmapDrawable(getResources(), bm);
    // dialogHeader.setBackground(new CurvedAndTiled(bmDrawable.getBitmap(),
    // 5)); \n vvvvvvvv
    dialogHeader
      .setBackgroundColor(getResources().getColor(R.color.blue_extra));
    // Set dialog title and main EditText
    dialogTitle.setText("Rename Uploaded File");
    RobotoEditText docFilenameView = (RobotoEditText) dialogView
      .findViewById(R.id.doc_filename);
    docFilenameView.setText(filename);

    new AlertDialog.Builder(this).setView(dialogView)
      .setPositiveButton("Done", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int whichButton) {

          RobotoEditText docFilenameView = (RobotoEditText) dialogView
            .findViewById(R.id.doc_filename);
          String docFilename = docFilenameView.getText().toString();
          if (docFilename == null || docFilename.isEmpty()) {
            ActivityMain.currentUser.put("docName", filename);
          } else {
            // filename not null, save it to sharedpreference
            ActivityMain.currentUser.put("docName", docFilename);
          }
          ActivityMain.currentUser.saveEventually(null);
          newPrefFrag.setprefDocGreeting(true);
        }
      }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int whichButton) {

        }
      }).setCancelable(false).show();

  }

  public void copyFile(File src, File dst) throws IOException {
    InputStream in = new FileInputStream(src);
    OutputStream out = new FileOutputStream(dst);

    // Transfer bytes from in to out
    byte[] buf = new byte[1024];
    int len;
    while ((len = in.read(buf)) > 0) {
      out.write(buf, 0, len);
    }
    in.close();
    out.close();
  }

  public static String getPath(Context context, Uri uri)
    throws URISyntaxException {
    if ("content".equalsIgnoreCase(uri.getScheme())) {
      String[] projection = { "_data" };
      Cursor cursor = null;

      try {
        cursor = context.getContentResolver().query(uri, projection, null,
          null, null);
        int column_index = cursor.getColumnIndexOrThrow("_data");
        if (cursor.moveToFirst()) {
          return cursor.getString(column_index);
        }
      } catch (Exception e) {
        // Eat it
      }
    } else if ("file".equalsIgnoreCase(uri.getScheme())) {
      return uri.getPath();
    }

    return null;
  }

  private void setColor(TextView view, String fulltext, String subtext,
    int color, Spannable str) {

    int i = fulltext.indexOf(subtext);
    str.setSpan(new ForegroundColorSpan(color), i, i + subtext.length(),
      Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

  }

  private String getLink() {
    String website = ActivityMain.applicationContext
      .getString(R.string.base_website_user);
    StringBuffer qrString = new StringBuffer(website);
    qrString.append("id=");
    qrString.append(ActivityMain.myselfUserInfo.getObjId());
    qrString.append("&fn=");
    qrString.append(ActivityMain.myselfUserInfo.getFirstName());
    qrString.append("&ln=");
    qrString.append(ActivityMain.myselfUserInfo.getLastName());
    return qrString.toString();
  }

  private String getShortLink() {
    String website = ActivityMain.applicationContext
      .getString(R.string.base_website_user);
    StringBuffer qrString = new StringBuffer(website);
    qrString.append("id=");
    qrString.append(ActivityMain.myselfUserInfo.getObjId());
    return qrString.toString();
  }

}
