package com.micklestudios.knowell;

import com.micklestudios.knowell.utils.RobotoEditText;
import com.parse.ParseUser;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ActivityUserSetting extends ActionBarActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    // TODO Auto-generated method stub
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_usersetting);

    showActionBar();

    getFragmentManager().beginTransaction()
      .replace(R.id.preference_container, new PrefsFragment()).commit();

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
              ActivityPreLogin.class);
            startActivity(intent);
            ActivityUserSetting.this.finish();
            return true;
          }
        });
      Preference prefEmailGreeting = (Preference) findPreference(getString(R.string.prefEmailGreeting));
      prefEmailGreeting
        .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
          @Override
          public boolean onPreferenceClick(Preference preference) {
            
            
            LayoutInflater inflater = getLayoutInflater();
            final View dialogView = inflater.inflate(R.layout.layout_default_msg, null);
            LinearLayout dialogHeader = (LinearLayout) dialogView
              .findViewById(R.id.dialog_header);
            final TextView dialogText = (TextView) dialogView
              .findViewById(R.id.dialog_text);
            TextView dialogTitle = (TextView) dialogView
              .findViewById(R.id.dialog_title);
            dialogHeader
              .setBackgroundColor(getResources().getColor(R.color.blue_extra));
            dialogTitle.setText("Set Default Email");
            final RobotoEditText subjectView = (RobotoEditText) dialogView
              .findViewById(R.id.subject);
            final RobotoEditText messageView = (RobotoEditText) dialogView
                .findViewById(R.id.message_body);

            subjectView.setText("Greetings from "
            + ActivityMain.myselfUserInfo.getFirstName() + " "
            + ActivityMain.myselfUserInfo.getLastName());
            messageView.setText("Hi "
            
            + ",\n\nThis is "
            + ActivityMain.myselfUserInfo.getFirstName()
            + " "
            + ActivityMain.myselfUserInfo.getLastName()
            + " from "
            + ActivityMain.myselfUserInfo.getCompany()
            + ".\n\nIt was great to meet you! Keep in touch! \n\nBest,\n"
            + ActivityMain.myselfUserInfo.getFirstName()
            + "\n\nPlease accept my business card here: " + getLink());
            
            new AlertDialog.Builder(ActivityUserSetting.this).setView(dialogView)
            .setPositiveButton("Done", new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int whichButton) {
                
                String docFilename = subjectView.getText().toString();
                String docMessage = messageView.getText().toString();
                
              }
            }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int whichButton) {

              }
            }).setCancelable(false).show();
            return true;
          }
        });
      
      Preference prefSmsGreeting = (Preference) findPreference(getString(R.string.prefSMSGreeting));
      prefSmsGreeting
        .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
          @Override
          public boolean onPreferenceClick(Preference preference) {
            
            
            LayoutInflater inflater = getLayoutInflater();
            final View dialogView = inflater.inflate(R.layout.layout_default_msg, null);
            LinearLayout dialogHeader = (LinearLayout) dialogView
              .findViewById(R.id.dialog_header);
            final TextView dialogText = (TextView) dialogView
              .findViewById(R.id.dialog_text);
            TextView dialogTitle = (TextView) dialogView
              .findViewById(R.id.dialog_title);
            dialogHeader
              .setBackgroundColor(getResources().getColor(R.color.blue_extra));
            dialogTitle.setText("Set Default SMS");
            final RobotoEditText subjectView = (RobotoEditText) dialogView
              .findViewById(R.id.subject);
            final RobotoEditText messageView = (RobotoEditText) dialogView
                .findViewById(R.id.message_body);

            subjectView.setVisibility(View.GONE);
            messageView.setText("Hi "
            
            + ",\n\nThis is "
            + ActivityMain.myselfUserInfo.getFirstName()
            + " "
            + ActivityMain.myselfUserInfo.getLastName()
            + " from "
            + ActivityMain.myselfUserInfo.getCompany()
            + ".\n\nIt was great to meet you! Keep in touch! \n\nBest,\n"
            + ActivityMain.myselfUserInfo.getFirstName()
            + "\n\nPlease accept my business card here: " + getLink());
            
            new AlertDialog.Builder(ActivityUserSetting.this).setView(dialogView)
            .setPositiveButton("Done", new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int whichButton) {
                
                String docFilename = subjectView.getText().toString();
                String docMessage = messageView.getText().toString();
                
              }
            }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int whichButton) {

              }
            }).setCancelable(false).show();
            return true;
          }
        });

      Preference prefDocGreeting = (Preference) findPreference(getString(R.string.prefDocGreeting));
      prefDocGreeting
        .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
          @Override
          public boolean onPreferenceClick(Preference preference) {
            
            
            LayoutInflater inflater = getLayoutInflater();
            final View dialogView = inflater.inflate(R.layout.layout_default_msg, null);
            LinearLayout dialogHeader = (LinearLayout) dialogView
              .findViewById(R.id.dialog_header);
            final TextView dialogText = (TextView) dialogView
              .findViewById(R.id.dialog_text);
            TextView dialogTitle = (TextView) dialogView
              .findViewById(R.id.dialog_title);
            dialogHeader
              .setBackgroundColor(getResources().getColor(R.color.blue_extra));
            dialogTitle.setText("Set Default Document Message");
            final RobotoEditText subjectView = (RobotoEditText) dialogView
              .findViewById(R.id.subject);
            final RobotoEditText messageView = (RobotoEditText) dialogView
                .findViewById(R.id.message_body);

            subjectView.setText("Greetings from "
                + ActivityMain.myselfUserInfo.getFirstName() + " "
                + ActivityMain.myselfUserInfo.getLastName());
            messageView.setText("Hi "
            
            + ",\n\nThis is "
            + ActivityMain.myselfUserInfo.getFirstName()
            + " "
            + ActivityMain.myselfUserInfo.getLastName()
            + " from "
            + ActivityMain.myselfUserInfo.getCompany()
            + ".\n\nIt was great to meet you! Keep in touch! \n\nBest,\n"
            + ActivityMain.myselfUserInfo.getFirstName()
            + "\n\nPlease accept my business card here: " + getLink());
            
            new AlertDialog.Builder(ActivityUserSetting.this).setView(dialogView)
            .setPositiveButton("Done", new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int whichButton) {
                
                String docFilename = subjectView.getText().toString();
                String docMessage = messageView.getText().toString();
                
              }
            }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int whichButton) {

              }
            }).setCancelable(false).show();
            return true;
          }
        });

    }

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

}
