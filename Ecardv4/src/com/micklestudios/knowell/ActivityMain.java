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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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
import com.micklestudios.knowell.utils.RobotoEditText;
import com.parse.ParseInstallation;
import com.parse.ParsePush;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.micklestudios.knowell.R;

public class ActivityMain extends ActionBarActivity {

  private static final int EDIT_CARD = 1000;
  protected static final int UPLOAD_DOC = 1001;
  private static final String KNOWELL_ROOT = "KnoWell";
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

  public static Context applicationContext;

  @SuppressLint("NewApi")
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    showActionBar();
    setContentView(R.layout.activity_main);

    applicationContext = getApplicationContext();

    Bundle b = getIntent().getExtras();
    if (b != null) {
      if (b.get("imgFromTmpData") != null) {
        imgFromTmpData = (boolean) b.get("imgFromTmpData");
      }
    }

    Display display = getWindowManager().getDefaultDisplay();
    DisplayMetrics outMetrics = new DisplayMetrics();
    display.getMetrics(outMetrics);

    float density = getResources().getDisplayMetrics().density;
    float dpHeight = outMetrics.heightPixels / density;
    float dpWidth = outMetrics.widthPixels / density;
    Log.i("res", "height: " + dpHeight + "  , width: " + dpWidth);

    currentUser = ParseUser.getCurrentUser();
    // pull myself info from localdatastore
    Log.i("imgtmp", " " + imgFromTmpData);
    myselfUserInfo = new UserInfo(currentUser.get("ecardId").toString(), "",
      "", true, false, imgFromTmpData);

    mAdapter = new MyPagerAdapter(getSupportFragmentManager());

    mPager = (MyViewPager) findViewById(R.id.pager);
    mPager.setAdapter(mAdapter);
    mPager.setCurrentItem(0x40000000);
    mPager.setPageTransformer(true, new FlipHorizontalTransformer());

    InitializeListeners();

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
//    // Set dialog header background with rounded corner
//    Bitmap bm = BitmapFactory
//      .decodeResource(getResources(), R.drawable.striped);
//    BitmapDrawable bmDrawable = new BitmapDrawable(getResources(), bm);
//    dialogHeader.setBackground(new CurvedAndTiled(bmDrawable.getBitmap(), 5)); \n vvvvvvvv
    dialogHeader.setBackgroundColor(getResources().getColor(R.color.blue_extra));
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
//    // Set dialog header background with rounded corner
//    Bitmap bm = BitmapFactory
//      .decodeResource(getResources(), R.drawable.striped);
//    BitmapDrawable bmDrawable = new BitmapDrawable(getResources(), bm);
//    dialogHeader.setBackground(new CurvedAndTiled(bmDrawable.getBitmap(), 5)); \n vvvvvvvv
    dialogHeader.setBackgroundColor(getResources().getColor(R.color.blue_extra));
    // Set dialog title and main EditText
    dialogTitle.setText("Share " + docName + " to ...");

    new AlertDialog.Builder(this).setView(dialogView)
      .setPositiveButton("Send", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int whichButton) {
          String link = getLink();
          RobotoEditText targetEmailView = (RobotoEditText) dialogView
            .findViewById(R.id.target_email);
          RobotoEditText targetNameView = (RobotoEditText) dialogView
            .findViewById(R.id.target_name);
          String targetEmail = targetEmailView.getText().toString();
          String targetName = targetNameView.getText().toString();
          if (targetEmail == null || targetEmail.isEmpty()) {
          } else {

            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.setType("message/rfc822");
            sendIntent.putExtra(Intent.EXTRA_EMAIL,
              new String[] { targetEmail });

            String msgSubject = "Greetings from "
              + ActivityMain.myselfUserInfo.getFirstName() + " "
              + ActivityMain.myselfUserInfo.getLastName();
            if (ActivityMain.currentUser.get("docMsgSubject") != null
              && !ActivityMain.currentUser.get("docMsgSubject").toString()
                .isEmpty()) {
              msgSubject = ActivityMain.currentUser.get("docMsgSubject")
                .toString();
            }

            sendIntent.putExtra(Intent.EXTRA_SUBJECT, msgSubject);

            String msgBody = "Hi "
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

            if (ActivityMain.currentUser.get("docMsgBody") != null
              && !ActivityMain.currentUser.get("docMsgBody").toString()
                .isEmpty()) {
              msgBody = ActivityMain.currentUser.get("docMsgBody").toString();
            }

            sendIntent.putExtra(Intent.EXTRA_TEXT, msgBody);

            sendIntent.putExtra(Intent.EXTRA_STREAM, uri);
            startActivity(sendIntent);

          }
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
//    // Set dialog header background with rounded corner
//    Bitmap bm = BitmapFactory
//      .decodeResource(getResources(), R.drawable.striped);
//    BitmapDrawable bmDrawable = new BitmapDrawable(getResources(), bm);
//    dialogHeader.setBackground(new CurvedAndTiled(bmDrawable.getBitmap(), 5)); \n vvvvvvvv
    dialogHeader.setBackgroundColor(getResources().getColor(R.color.blue_extra));
    // Set dialog title and main EditText
    dialogTitle.setText("Share QR code");
    
    flagShareEmail = true;
    ImageView switch2message = (ImageView) dialogView.findViewById(R.id.share_switch2message);
    ImageView switch2email = (ImageView) dialogView.findViewById(R.id.share_switch2email);
    final RelativeLayout messagePanel = (RelativeLayout) dialogView.findViewById(R.id.share_message_panel);
    final RelativeLayout emailPanel = (RelativeLayout) dialogView.findViewById(R.id.share_email_panel);
    switch2message.setOnClickListener(new View.OnClickListener(){

      @Override
      public void onClick(View v) {
        flagShareEmail = false;
        messagePanel.setVisibility(View.VISIBLE);
        emailPanel.setVisibility(View.GONE);
      }
      
    });
    switch2email.setOnClickListener(new View.OnClickListener(){

      @Override
      public void onClick(View v) {
        flagShareEmail = true;
        emailPanel.setVisibility(View.VISIBLE);
        messagePanel.setVisibility(View.GONE);
      }
      
    });

    new AlertDialog.Builder(this).setView(dialogView)
      .setPositiveButton("Send", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int whichButton) {
          String link = getLink();
          RobotoEditText targetEmailView = (RobotoEditText) dialogView
            .findViewById(R.id.target_email);
          RobotoEditText targetSMSView = (RobotoEditText) dialogView
            .findViewById(R.id.target_sms);
          RobotoEditText targetNameView = (RobotoEditText) dialogView
            .findViewById(R.id.target_name);
          
          String targetEmail = targetEmailView.getText().toString();
          String targetSMS = targetSMSView.getText().toString();
          String targetName = targetNameView.getText().toString();

          if (!flagShareEmail && !targetSMS.isEmpty()) {
            // send to message
            String msgBody;
            if (ActivityMain.currentUser.get("defaultMsgBody") != null
              && !ActivityMain.currentUser.get("defaultMsgBody").toString()
                .isEmpty()) {
              msgBody = ActivityMain.currentUser.get("defaultMsgBody")
                .toString();
            } else {
              msgBody = "Hi "
                + targetName
                + ", this is "
                + ActivityMain.myselfUserInfo.getFirstName()
                + " "
                + ActivityMain.myselfUserInfo.getLastName()
                + " from "
                + ActivityMain.myselfUserInfo.getCompany()
                + ". It was great to meet you! Please accept my business card here: "
                + link;
            }
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("smsto:"
              + targetSMS));
            intent.putExtra("sms_body", msgBody);
            startActivity(intent);
          }
          if (flagShareEmail && !targetEmail.isEmpty()) {
            String msgSubject = "Greetings from "
              + ActivityMain.myselfUserInfo.getFirstName() + " "
              + ActivityMain.myselfUserInfo.getLastName();
            if (ActivityMain.currentUser.get("defaultMsgSubject") != null
              && !ActivityMain.currentUser.get("defaultMsgSubject").toString()
                .isEmpty()) {
              msgSubject = ActivityMain.currentUser.get("defaultMsgSubject")
                .toString();
            }
            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.setType("message/rfc822");
            sendIntent.putExtra(Intent.EXTRA_EMAIL,
              new String[] { targetEmail });
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, msgSubject);
            String msgBody;
            if (ActivityMain.currentUser.get("defaultMsgBody") != null
              && !ActivityMain.currentUser.get("defaultMsgBody").toString()
                .isEmpty()) {
              msgBody = ActivityMain.currentUser.get("defaultMsgBody")
                .toString();
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
            startActivity(sendIntent);

          }
        }
      }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int whichButton) {

        }
      }).setCancelable(false).show();

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
//    // Set dialog header background with rounded corner
//    Bitmap bm = BitmapFactory
//      .decodeResource(getResources(), R.drawable.striped);
//    BitmapDrawable bmDrawable = new BitmapDrawable(getResources(), bm);
//    dialogHeader.setBackground(new CurvedAndTiled(bmDrawable.getBitmap(), 5)); \n vvvvvvvv
    dialogHeader.setBackgroundColor(getResources().getColor(R.color.blue_extra));
    // Set dialog title and main EditText
    dialogTitle.setText("Upload successful!");
    RobotoEditText docFilenameView = (RobotoEditText) dialogView
      .findViewById(R.id.doc_filename);
    docFilenameView.setText(filename);

    new AlertDialog.Builder(this).setView(dialogView)
      .setPositiveButton("Done", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int whichButton) {

          RobotoEditText docFilenameView = (RobotoEditText) dialogView
            .findViewById(R.id.doc_filename);
          RobotoEditText docMessageView = (RobotoEditText) dialogView
            .findViewById(R.id.doc_message);
          String docFilename = docFilenameView.getText().toString();
          String docMessage = docMessageView.getText().toString();
          if (docFilename == null || docFilename.isEmpty()) {
            ActivityMain.currentUser.put("docName", filename);
          } else {
            // filename not null, save it to sharedpreference
            ActivityMain.currentUser.put("docName", docFilename);
          }
          if (docMessage == null || docMessage.isEmpty()) {
            ActivityMain.currentUser.remove("docMsgBody");
          } else {
            // description not null, save it
            ActivityMain.currentUser.put("docMsgBody", docMessage);
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
    case R.id.log_out:
      ParseUser.logOut();
      intent = new Intent(this, ActivityPreLogin.class);
      startActivity(intent);
      this.finish();
      return true;
    default:
      return super.onOptionsItemSelected(item);
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (resultCode == Activity.RESULT_OK) {

      switch (requestCode) {
      case EDIT_CARD:
        // Refreshing fragments
        mAdapter.notifyDataSetChanged();        
        break;
      case UPLOAD_DOC:
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

        break;
      }
    }
    super.onActivityResult(requestCode, resultCode, data);
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
