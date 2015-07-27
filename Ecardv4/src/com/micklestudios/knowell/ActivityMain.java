package com.micklestudios.knowell;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ToxicBakery.viewpager.transforms.FlipHorizontalTransformer;
import com.ToxicBakery.viewpager.transforms.RotateUpTransformer;
import com.google.zxing.client.android.Intents;
import com.micklestudios.knowell.infrastructure.UserInfo;
import com.micklestudios.knowell.utils.CurvedAndTiled;
import com.micklestudios.knowell.utils.CustomQRScanner;
import com.micklestudios.knowell.utils.MyPagerAdapter;
import com.micklestudios.knowell.utils.MyViewPager;
import com.parse.ParseACL;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParsePush;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.micklestudios.knowell.R;

public class ActivityMain extends ActionBarActivity {

  private static final int EDIT_CARD = 1000;
  protected static final int UPLOAD_DOC = 1001;
  private static final String KNOWELL_ROOT = "KnoWell";
  protected static final int SHARE_QR_MSG = 1002;
  protected static final int SHARE_QR_EMAIL = 1003;
  protected static final int SHARE_DOC = 1004;
  protected static final int SHARE_GENERIC = 2001;
  /**
   * The {@link ViewPager} that will host the section contents.
   */
  MyPagerAdapter mAdapter;
  MyViewPager mPager;
  ActionBar mActionBar;
  Menu mMenu;
  int currentPosition = 0;
  public static ParseUser currentUser;
  // set myselfUserInfo to be global for each access across the entire app
  public static UserInfo myselfUserInfo = null;
  boolean imgFromTmpData = false;
  private AlertDialog uploadDialog;
  boolean flagShareEmail = true;
  private String targetEmail = null;
  private String targetName = null;
  protected String targetSMS = null;
  private BroadcastReceiver logoutNotifier;

  public static Context applicationContext;

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent);
    Bundle b = getIntent().getExtras();
    if (b != null) {
      boolean finish = b.getBoolean("finish", false);
      if (finish) {
        startActivity(new Intent(ActivityMain.this, ActivityPreLogin.class));
        finish();
        return;
      }
    }
  }

  @SuppressLint("NewApi")
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    applicationContext = getApplicationContext();
    Bundle b = getIntent().getExtras();
    if (b != null) {
      if (b.get("imgFromTmpData") != null) {
        // this value is set when returning from ActivityDesign
        imgFromTmpData = (boolean) b.get("imgFromTmpData");
      }
    }
    
    // This fixes the lost data/ crash issues upon restoring from resume
    if(savedInstanceState != null) {
      currentUser = ParseUser.getCurrentUser();
      if(myselfUserInfo == null){
        myselfUserInfo = savedInstanceState.getParcelable("myself");
        imgFromTmpData = savedInstanceState.getBoolean("imgFromTmpData");
      }
      Log.e("main", "getting savedisntance");
    } else {
      currentUser = ParseUser.getCurrentUser();
      if(myselfUserInfo == null){
        myselfUserInfo = new UserInfo(currentUser.get("ecardId").toString(), "",
          "", true, false, imgFromTmpData);
      }
    }

    showActionBar();
    setContentView(R.layout.activity_main);
    TextView motto = (TextView) findViewById(R.id.my_motto);
    String tmpString = myselfUserInfo.getMotto();
    if (tmpString != null)
      motto.setText(tmpString);

    Display display = getWindowManager().getDefaultDisplay();
    DisplayMetrics outMetrics = new DisplayMetrics();
    display.getMetrics(outMetrics);

    float density = getResources().getDisplayMetrics().density;
    float dpHeight = outMetrics.heightPixels / density;
    float dpWidth = outMetrics.widthPixels / density;
    Log.i("res", "height: " + dpHeight + "  , width: " + dpWidth);

    mAdapter = new MyPagerAdapter(getSupportFragmentManager());

    mPager = (MyViewPager) findViewById(R.id.pager);
    mPager.setAdapter(mAdapter);
    mPager.setCurrentItem(0x40000000);
    mPager.setPageTransformer(true, new FlipHorizontalTransformer());
    
    InitializeListeners();

  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    outState.putParcelable("myself", ActivityMain.myselfUserInfo);
    outState.putBoolean("imgFromTmpData", imgFromTmpData);
    super.onSaveInstanceState(outState);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    LocalBroadcastManager.getInstance(this).unregisterReceiver(logoutNotifier);
  }

  private void InitializeListeners() {
    LinearLayout ll_add = (LinearLayout) findViewById(R.id.ll_add);
    LinearLayout ll_search = (LinearLayout) findViewById(R.id.ll_search);
    LinearLayout ll_share = (LinearLayout) findViewById(R.id.ll_share);
    LinearLayout ll_doc = (LinearLayout) findViewById(R.id.ll_doc);

    ll_add.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {
        Intent intent = new Intent(v.getContext(), CustomQRScanner.class);
        intent.setAction(Intents.Scan.ACTION);
        intent.putExtra(Intents.Scan.FORMATS, "QR_CODE");
        startActivity(intent);
      }

    });

    ll_search.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {
        Intent intent = new Intent(v.getContext(), ActivitySearch.class);
        startActivity(intent);
      }

    });

    ll_share.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {
        // pop up dialog for sharing
        shareQRCode();
      }

    });

    ll_doc.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {
        // check if doc exists
        String docName = "";
        String docPath = "";
        if (ActivityMain.currentUser.get("docName") != null
          && !ActivityMain.currentUser.get("docName").toString().isEmpty()) {
          docName = ActivityMain.currentUser.get("docName").toString();
        }
        if (ActivityMain.currentUser.get("docPath") != null
          && !ActivityMain.currentUser.get("docPath").toString().isEmpty()) {
          docPath = ActivityMain.currentUser.get("docPath").toString();
        }

        File file = new File(docPath);

        if (file.exists()) {
          // if yes, pop up dialog for sharing
          Uri uri = Uri.fromFile(file);
          shareDoc(docName, uri);
        } else {
          // if no, pop up dialog for selecting file
          uploadDoc();

        }
      }

    });
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
    // // Set dialog header background with rounded corner
    // Bitmap bm = BitmapFactory
    // .decodeResource(getResources(), R.drawable.striped);
    // BitmapDrawable bmDrawable = new BitmapDrawable(getResources(), bm);
    // dialogHeader.setBackground(new CurvedAndTiled(bmDrawable.getBitmap(),
    // 5)); \n vvvvvvvv
    dialogHeader
      .setBackgroundColor(getResources().getColor(R.color.blue_extra));
    // Set dialog title and main EditText
    dialogTitle.setText("You have no document yet ...");

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

  @SuppressLint("NewApi")
  private void shareDoc(final String docName, final Uri uri) {
    // Get the layout inflater
    LayoutInflater inflater = getLayoutInflater();
    final View dialogView = inflater.inflate(R.layout.layout_share_doc, null);
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
    dialogTitle.setText("Share " + docName + " to ...");

    new AlertDialog.Builder(this).setView(dialogView)
      .setPositiveButton("Send", new DialogInterface.OnClickListener() {

        public void onClick(DialogInterface dialog, int whichButton) {
          String link = getLink();
          EditText targetEmailView = (EditText) dialogView
            .findViewById(R.id.target_email);
          EditText targetNameView = (EditText) dialogView
            .findViewById(R.id.target_name);
          targetEmail = targetEmailView.getText().toString();
          targetName = targetNameView.getText().toString();

          Intent sendIntent = new Intent(Intent.ACTION_SEND);
          sendIntent.setType("message/rfc822");
          sendIntent.putExtra(Intent.EXTRA_EMAIL, new String[] { targetEmail });

          String msgSubject;
          if (ActivityMain.currentUser.get("docMsgSubject") != null
            && !ActivityMain.currentUser.get("docMsgSubject").toString()
              .isEmpty()) {
            msgSubject = ActivityMain.currentUser.get("docMsgSubject")
              .toString();
            String processedSubject = msgSubject.replaceAll("#r[a-zA-Z0-9]*#",
              targetName);
            processedSubject = processedSubject.replaceAll("#m[a-zA-Z0-9]*#",
              ActivityMain.myselfUserInfo.getFirstName() + " "
                + ActivityMain.myselfUserInfo.getLastName());
            processedSubject = processedSubject.replaceAll("#d[a-zA-Z0-9]*#",
              docName);
            processedSubject = processedSubject.replaceAll("#c[a-zA-Z0-9]*#",
              ActivityMain.myselfUserInfo.getCompany());
            msgSubject = processedSubject.replaceAll("#k[a-zA-Z0-9]*#",
              getLink());

          } else {
            msgSubject = "Greetings from "
              + ActivityMain.myselfUserInfo.getFirstName() + " "
              + ActivityMain.myselfUserInfo.getLastName();
          }

          sendIntent.putExtra(Intent.EXTRA_SUBJECT, msgSubject);

          String msgBody;

          if (ActivityMain.currentUser.get("docMsgBody") != null
            && !ActivityMain.currentUser.get("docMsgBody").toString().isEmpty()) {
            msgBody = ActivityMain.currentUser.get("docMsgBody").toString();
            String processedBody = msgBody.replaceAll("#r[a-zA-Z0-9]*#",
              targetName);
            processedBody = processedBody.replaceAll("#m[a-zA-Z0-9]*#",
              ActivityMain.myselfUserInfo.getFirstName() + " "
                + ActivityMain.myselfUserInfo.getLastName());
            processedBody = processedBody
              .replaceAll("#d[a-zA-Z0-9]*#", docName);
            processedBody = processedBody.replaceAll("#c[a-zA-Z0-9]*#",
              ActivityMain.myselfUserInfo.getCompany());
            msgBody = processedBody.replaceAll("#k[a-zA-Z0-9]*#", getLink());
          } else {
            msgBody = "Hi "
              + targetName
              + ",\n\nThis is "
              + ActivityMain.myselfUserInfo.getFirstName()
              + " "
              + ActivityMain.myselfUserInfo.getLastName()
              + " from "
              + ActivityMain.myselfUserInfo.getCompany()
              + ". Please find my "
              + docName
              + " in attachment. \n\nIt was great to meet you! Keep in touch! \n\nBest,\n"
              + ActivityMain.myselfUserInfo.getFirstName()
              + "\n\nPlease accept my business card here: " + link;
          }

          sendIntent.putExtra(Intent.EXTRA_TEXT, msgBody);

          sendIntent.putExtra(Intent.EXTRA_STREAM, uri);
          startActivityForResult(sendIntent, SHARE_DOC);

        }
      }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int whichButton) {

        }
      }).setCancelable(false).show();

  }

  @SuppressLint("NewApi")
  private void shareQRCode() {
    // Get the layout inflater
    LayoutInflater inflater = getLayoutInflater();
    final View dialogView = inflater.inflate(R.layout.layout_share_qr, null);
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
    dialogTitle.setText("Share QR code");

    flagShareEmail = true;
    ImageView switch2message = (ImageView) dialogView
      .findViewById(R.id.share_switch2message);
    ImageView switch2email = (ImageView) dialogView
      .findViewById(R.id.share_switch2email);
    ImageView directLink1 = (ImageView) dialogView
      .findViewById(R.id.share_direct_link1);
    ImageView directLink2 = (ImageView) dialogView
      .findViewById(R.id.share_direct_link2);
    final RelativeLayout messagePanel = (RelativeLayout) dialogView
      .findViewById(R.id.share_message_panel);
    final RelativeLayout emailPanel = (RelativeLayout) dialogView
      .findViewById(R.id.share_email_panel);
    switch2message.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        flagShareEmail = false;
        messagePanel.setVisibility(View.VISIBLE);
        emailPanel.setVisibility(View.GONE);
      }

    });
    switch2email.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        flagShareEmail = true;
        emailPanel.setVisibility(View.VISIBLE);
        messagePanel.setVisibility(View.GONE);
      }

    });

    final AlertDialog dialog = new AlertDialog.Builder(this)
      .setView(dialogView)
      .setPositiveButton("Send", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int whichButton) {
          String link = getLink();
          EditText targetEmailView = (EditText) dialogView
            .findViewById(R.id.target_email);
          EditText targetSMSView = (EditText) dialogView
            .findViewById(R.id.target_sms);
          EditText targetNameView = (EditText) dialogView
            .findViewById(R.id.target_name);

          targetEmail = targetEmailView.getText().toString();
          targetSMS = targetSMSView.getText().toString();
          targetName = targetNameView.getText().toString();

          if (!flagShareEmail) {
            // send to message
            String msgBody;
            if (ActivityMain.currentUser.get("smsBody") != null
              && !ActivityMain.currentUser.get("smsBody").toString().isEmpty()) {
              msgBody = ActivityMain.currentUser.get("smsBody").toString();

              String processedBody = msgBody.replaceAll("#r[a-zA-Z0-9]*#",
                targetName);
              processedBody = processedBody.replaceAll("#m[a-zA-Z0-9]*#",
                ActivityMain.myselfUserInfo.getFirstName() + " "
                  + ActivityMain.myselfUserInfo.getLastName());
              processedBody = processedBody.replaceAll("#c[a-zA-Z0-9]*#",
                ActivityMain.myselfUserInfo.getCompany());
              msgBody = processedBody.replaceAll("#k[a-zA-Z0-9]*#",
                getShortLink());
            } else {
              msgBody = "Hi " + targetName + ", this is "
                + ActivityMain.myselfUserInfo.getFirstName() + " "
                + ActivityMain.myselfUserInfo.getLastName() + " from "
                + ActivityMain.myselfUserInfo.getCompany()
                + ". Keep in touch! My business card: " + getShortLink();
            }
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("smsto:"
              + targetSMS));
            intent.putExtra("sms_body", msgBody);
            startActivityForResult(intent, SHARE_QR_MSG);
          }
          if (flagShareEmail) {
            String msgSubject;
            if (ActivityMain.currentUser.get("emailSubject") != null
              && !ActivityMain.currentUser.get("emailSubject").toString()
                .isEmpty()) {
              msgSubject = ActivityMain.currentUser.get("emailSubject")
                .toString();
              String processedSubject = msgSubject.replaceAll(
                "#r[a-zA-Z0-9]*#", targetName);
              processedSubject = processedSubject.replaceAll("#m[a-zA-Z0-9]*#",
                ActivityMain.myselfUserInfo.getFirstName() + " "
                  + ActivityMain.myselfUserInfo.getLastName());
              processedSubject = processedSubject.replaceAll("#c[a-zA-Z0-9]*#",
                ActivityMain.myselfUserInfo.getCompany());
              msgSubject = processedSubject.replaceAll("#k[a-zA-Z0-9]*#",
                getLink());

            } else {
              msgSubject = "Greetings from "
                + ActivityMain.myselfUserInfo.getFirstName() + " "
                + ActivityMain.myselfUserInfo.getLastName();
            }
            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.setType("message/rfc822");
            sendIntent.putExtra(Intent.EXTRA_EMAIL,
              new String[] { targetEmail });
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, msgSubject);
            String msgBody;
            if (ActivityMain.currentUser.get("emailBody") != null
              && !ActivityMain.currentUser.get("emailBody").toString()
                .isEmpty()) {
              msgBody = ActivityMain.currentUser.get("emailBody").toString();
              String processedBody = msgBody.replaceAll("#r[a-zA-Z0-9]*#",
                targetName);
              processedBody = processedBody.replaceAll("#m[a-zA-Z0-9]*#",
                ActivityMain.myselfUserInfo.getFirstName() + " "
                  + ActivityMain.myselfUserInfo.getLastName());
              processedBody = processedBody.replaceAll("#c[a-zA-Z0-9]*#",
                ActivityMain.myselfUserInfo.getCompany());
              msgBody = processedBody.replaceAll("#k[a-zA-Z0-9]*#", getLink());
            } else {
              msgBody = "Hi " + targetName + ",\n\nThis is "
                + ActivityMain.myselfUserInfo.getFirstName() + " "
                + ActivityMain.myselfUserInfo.getLastName() + " from "
                + ActivityMain.myselfUserInfo.getCompany()
                + ". It was great to meet you! Keep in touch! \n\nBest,\n"
                + ActivityMain.myselfUserInfo.getFirstName()
                + "\n\nPlease accept my business card here: " + link;
            }
            sendIntent.putExtra(Intent.EXTRA_TEXT, msgBody);
            startActivityForResult(sendIntent, SHARE_QR_EMAIL);

          }
        }
      }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int whichButton) {

        }
      }).setCancelable(false).show();

    directLink1.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        String link = getLink();
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, link);
        intent.setType("text/plain");
        startActivityForResult(Intent.createChooser(intent, "Share link to:"),
          SHARE_GENERIC);
        dialog.dismiss();
      }
    });
    directLink2.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        String link = getLink();
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, link);
        sendIntent.setType("text/plain");
        startActivityForResult(sendIntent, SHARE_GENERIC);
        dialog.dismiss();
      }
    });
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
    EditText docFilenameView = (EditText) dialogView
      .findViewById(R.id.doc_filename);
    docFilenameView.setText(filename);

    new AlertDialog.Builder(this).setView(dialogView)
      .setPositiveButton("Done", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int whichButton) {

          EditText docFilenameView = (EditText) dialogView
            .findViewById(R.id.doc_filename);
          String docFilename = docFilenameView.getText().toString();
          if (docFilename == null || docFilename.isEmpty()) {
            ActivityMain.currentUser.put("docName", filename);
          } else {
            // filename not null, save it to sharedpreference
            ActivityMain.currentUser.put("docName", docFilename);
          }
          ActivityMain.currentUser.saveEventually(null);
        }
      }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int whichButton) {

        }
      }).setCancelable(false).show();

  }

  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.main_actionbar, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // this function is called when either action bar icon is tapped
    switch (item.getItemId()) {
    case R.id.edit_item:
      // Should be replaced by pop up activity of editable welcome page
      Intent intent = new Intent(this, ActivityDesign.class);
      startActivityForResult(intent, EDIT_CARD);
      return true;
    case R.id.settings:
      intent = new Intent(ActivityMain.this, ActivityUserSetting.class);
      startActivity(intent);
      return true;
    default:
      return super.onOptionsItemSelected(item);
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {

    switch (requestCode) {
    case EDIT_CARD:
      // Refreshing fragments
      if (resultCode == Activity.RESULT_OK) {
        mAdapter.notifyDataSetChanged();
        // Motto is not part of madapter
        TextView motto = (TextView) findViewById(R.id.my_motto);
        String tmpString = myselfUserInfo.getMotto();
        if (tmpString != null)
          motto.setText(tmpString);
      }
      break;
    case UPLOAD_DOC:
      if (resultCode == Activity.RESULT_OK) {
        uploadDialog.dismiss();
        // Get the Uri of the selected file
        Uri uri = data.getData();
        Log.d("uri", "File Uri: " + uri.toString());
        // Get the path
        String path;
        try {
          String srcPath = getPath(this, uri);
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
    case SHARE_QR_EMAIL:
      addToHistory(SHARE_QR_EMAIL);
      break;
    case SHARE_QR_MSG:
      addToHistory(SHARE_QR_MSG);
      break;
    case SHARE_DOC:
      addToHistory(SHARE_DOC);
      break;
    case SHARE_GENERIC:
      addToHistory(SHARE_GENERIC);
      break;
    }

    super.onActivityResult(requestCode, resultCode, data);
  }

  private void addToHistory(final int code) {

    LayoutInflater inflater = getLayoutInflater();
    final View dialogView = inflater.inflate(R.layout.layout_add_history, null);
    LinearLayout dialogHeader = (LinearLayout) dialogView
      .findViewById(R.id.dialog_header);
    final TextView dialogText = (TextView) dialogView
      .findViewById(R.id.dialog_text);
    TextView dialogTitle = (TextView) dialogView
      .findViewById(R.id.dialog_title);
    dialogHeader
      .setBackgroundColor(getResources().getColor(R.color.blue_extra));
    // Set dialog title and main EditText
    dialogTitle.setText("Keep record?");

    final EditText addHistoryNameView = (EditText) dialogView
      .findViewById(R.id.add_history_name);
    final EditText addHistoryEmailView = (EditText) dialogView
      .findViewById(R.id.add_history_email);
    final EditText addHistorySmsView = (EditText) dialogView
      .findViewById(R.id.add_history_sms);
    final EditText addHistoryNotesView = (EditText) dialogView
      .findViewById(R.id.add_history_note);

    if (targetName != null && !targetName.isEmpty()) {
      addHistoryNameView.setText(targetName);
    }
    if (targetEmail != null && !targetEmail.isEmpty()) {
      addHistoryEmailView.setText(targetEmail);
    }
    if (targetSMS != null && !targetSMS.isEmpty()) {
      addHistorySmsView.setText(targetSMS);
    }
    // clear these global variables so they do not pass on to next share
    targetName = "";
    targetEmail = "";
    targetSMS = "";

    new AlertDialog.Builder(this).setView(dialogView)
      .setPositiveButton("Save", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int whichButton) {
          ParseObject historyObj = new ParseObject("History");
          historyObj.setACL(new ParseACL(currentUser));
          historyObj.put("userId", currentUser.getObjectId());

          String nameString = addHistoryNameView.getText().toString();
          String emailString = addHistoryEmailView.getText().toString();
          String messageString = addHistorySmsView.getText().toString();
          String notesString = addHistoryNotesView.getText().toString();

          if (nameString != null && !nameString.isEmpty()) {
            historyObj.put("fullName", nameString);
          }
          if (emailString != null && !emailString.isEmpty()) {
            historyObj.put("email", emailString);
          }
          if (messageString != null && !messageString.isEmpty()) {
            historyObj.put("message", messageString);
          }
          if (notesString != null && !notesString.isEmpty()) {
            historyObj.put("notes", notesString);
          }
          historyObj.put("type", code);
          historyObj.saveEventually();
          historyObj.pinInBackground();

        }
      }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int whichButton) {

        }
      }).setCancelable(false).show();
  }

  private void showActionBar() {
    LayoutInflater inflator = (LayoutInflater) this
      .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    View v = inflator.inflate(R.layout.layout_actionbar_main, null);
    ImageView btnNotif = (ImageView) v.findViewById(R.id.btn_notifications);
    btnNotif.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent intent = new Intent(getApplicationContext(),
          ActivityConversations.class);
        startActivity(intent);
      }
    });
    ImageView btnHistory = (ImageView) v.findViewById(R.id.btn_history);
    btnHistory.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent intent = new Intent(getApplicationContext(),
          ActivityHistory.class);
        startActivity(intent);
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

}
