package com.micklestudios.knowell;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;

import com.google.zxing.client.android.Intents;
import com.micklestudios.knowell.infrastructure.UserInfo;
import com.micklestudios.knowell.utils.CurvedAndTiled;
import com.micklestudios.knowell.utils.CustomQRScanner;
import com.micklestudios.knowell.utils.ECardUtils;
import com.micklestudios.knowell.utils.MyTag;
import com.micklestudios.knowell.utils.RobotoEditText;
import com.micklestudios.knowell.utils.UpdateableFragment;
import com.parse.ParseUser;
import com.micklestudios.knowell.R;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class FragmentMaincard extends Fragment implements UpdateableFragment {

  private static final String ARG_SECTION_NUMBER = "section_number";
  protected static final int UPLOAD_DOC = 1001;
  private static final String KNOWELL_ROOT = "KnoWell";

  public static FragmentMaincard newInstance(int sectionNumber) {
    Log.i("maincard", "newinstance");
    FragmentMaincard fragment = new FragmentMaincard();
    Bundle args = new Bundle();
    args.putInt(ARG_SECTION_NUMBER, sectionNumber);
    fragment.setArguments(args);
    return fragment;
  }

  private View rootView;
  private AlertDialog uploadDialog;

  public FragmentMaincard() {

  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    Log.v("onsave", "In frag's on save instance state ");
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
    Bundle savedInstanceState) {
    Bundle bundle = this.getArguments();

    Log.i("maincard", "oncreateview");
    if (getArguments().getInt(ARG_SECTION_NUMBER, 1) == 1) {
      // if this is to create maincard fragment
      rootView = inflater.inflate(R.layout.fragment_maincard, container, false);

      LinearLayout ll_add = (LinearLayout) rootView.findViewById(R.id.ll_add);
      LinearLayout ll_search = (LinearLayout) rootView
        .findViewById(R.id.ll_search);

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

      // display the main card
      if (ActivityMain.myselfUserInfo != null) {
        displayCard(rootView, ActivityMain.myselfUserInfo);
      }
      setHasOptionsMenu(true);
      return rootView;
    }
    if (getArguments().getInt(ARG_SECTION_NUMBER, 1) == 2) {
      View rootView = inflater.inflate(R.layout.fragment_qr, container, false);

      LinearLayout ll_share = (LinearLayout) rootView
        .findViewById(R.id.ll_share);
      LinearLayout ll_doc = (LinearLayout) rootView.findViewById(R.id.ll_doc);

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

      if (ActivityMain.myselfUserInfo != null) {
        ImageView qrCode = (ImageView) rootView.findViewById(R.id.qr_container);
        qrCode.setImageBitmap(ActivityMain.myselfUserInfo.getQRCode());
      }
      setHasOptionsMenu(true);
      return rootView;
    }
    return null;

  }

  @SuppressLint("NewApi")
  private void uploadDoc() {
    // Get the layout inflater
    LayoutInflater inflater = getActivity().getLayoutInflater();
    final View dialogView = inflater.inflate(R.layout.layout_upload_doc, null);
    LinearLayout dialogHeader = (LinearLayout) dialogView
      .findViewById(R.id.dialog_header);
    final TextView dialogText = (TextView) dialogView
      .findViewById(R.id.dialog_text);
    TextView dialogTitle = (TextView) dialogView
      .findViewById(R.id.dialog_title);
    // Set dialog header background with rounded corner
    Bitmap bm = BitmapFactory
      .decodeResource(getResources(), R.drawable.striped);
    BitmapDrawable bmDrawable = new BitmapDrawable(getResources(), bm);
    dialogHeader.setBackground(new CurvedAndTiled(bmDrawable.getBitmap(), 5));
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

    uploadDialog = new AlertDialog.Builder(getActivity()).setView(dialogView)
      .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int whichButton) {

        }
      }).setCancelable(true).show();

  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (resultCode == Activity.RESULT_OK) {

      switch (requestCode) {
      case UPLOAD_DOC:
        uploadDialog.dismiss();
        // Get the Uri of the selected file
        Uri uri = data.getData();
        Log.d("uri", "File Uri: " + uri.toString());
        // Get the path
        String path;
        try {
          String srcPath = getPath(getActivity(), uri);
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
    LayoutInflater inflater = getActivity().getLayoutInflater();
    final View dialogView = inflater.inflate(R.layout.layout_upload_doc_done,
      null);
    LinearLayout dialogHeader = (LinearLayout) dialogView
      .findViewById(R.id.dialog_header);
    final TextView dialogText = (TextView) dialogView
      .findViewById(R.id.dialog_text);
    TextView dialogTitle = (TextView) dialogView
      .findViewById(R.id.dialog_title);
    // Set dialog header background with rounded corner
    Bitmap bm = BitmapFactory
      .decodeResource(getResources(), R.drawable.striped);
    BitmapDrawable bmDrawable = new BitmapDrawable(getResources(), bm);
    dialogHeader.setBackground(new CurvedAndTiled(bmDrawable.getBitmap(), 5));
    // Set dialog title and main EditText
    dialogTitle.setText("Upload successful!");
    RobotoEditText docFilenameView = (RobotoEditText) dialogView
      .findViewById(R.id.doc_filename);
    docFilenameView.setText(filename);

    new AlertDialog.Builder(getActivity()).setView(dialogView)
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

  @SuppressLint("NewApi")
  private void shareDoc(final String docName, final Uri uri) {
    // Get the layout inflater
    LayoutInflater inflater = getActivity().getLayoutInflater();
    final View dialogView = inflater.inflate(R.layout.layout_share_doc, null);
    LinearLayout dialogHeader = (LinearLayout) dialogView
      .findViewById(R.id.dialog_header);
    final TextView dialogText = (TextView) dialogView
      .findViewById(R.id.dialog_text);
    TextView dialogTitle = (TextView) dialogView
      .findViewById(R.id.dialog_title);
    // Set dialog header background with rounded corner
    Bitmap bm = BitmapFactory
      .decodeResource(getResources(), R.drawable.striped);
    BitmapDrawable bmDrawable = new BitmapDrawable(getResources(), bm);
    dialogHeader.setBackground(new CurvedAndTiled(bmDrawable.getBitmap(), 5));
    // Set dialog title and main EditText
    dialogTitle.setText("Share " + docName + " to ...");

    new AlertDialog.Builder(getActivity()).setView(dialogView)
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
    LayoutInflater inflater = getActivity().getLayoutInflater();
    final View dialogView = inflater.inflate(R.layout.layout_share_qr, null);
    LinearLayout dialogHeader = (LinearLayout) dialogView
      .findViewById(R.id.dialog_header);
    final TextView dialogText = (TextView) dialogView
      .findViewById(R.id.dialog_text);
    TextView dialogTitle = (TextView) dialogView
      .findViewById(R.id.dialog_title);
    // Set dialog header background with rounded corner
    Bitmap bm = BitmapFactory
      .decodeResource(getResources(), R.drawable.striped);
    BitmapDrawable bmDrawable = new BitmapDrawable(getResources(), bm);
    dialogHeader.setBackground(new CurvedAndTiled(bmDrawable.getBitmap(), 5));
    // Set dialog title and main EditText
    dialogTitle.setText("Share QR code to ...");

    new AlertDialog.Builder(getActivity()).setView(dialogView)
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

          if (targetEmail == null || targetEmail.isEmpty()) {
            if (targetSMS == null || targetSMS.isEmpty()) {
              // Pop alert both email and sms empty
            } else {
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
          } else {
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

  public void displayCard(View rootView, UserInfo newUser) {

    TextView name = (TextView) rootView.findViewById(R.id.my_name);
    String tmpString = newUser.getFirstName();
    String nameString = null;
    if (tmpString != null)
      nameString = tmpString;
    tmpString = newUser.getLastName();
    if (tmpString != null)
      nameString = nameString + " " + tmpString;
    if (nameString != null)
      name.setText(nameString);
    name = (TextView) rootView.findViewById(R.id.my_com);
    tmpString = newUser.getCompany();
    if (tmpString != null) {
      name.setText(tmpString);
      ImageView logoImg = (ImageView) rootView.findViewById(R.id.my_logo);
      // display logo
      ECardUtils.findAndSetLogo(getActivity(), logoImg, tmpString, true);
    }
    name = (TextView) rootView.findViewById(R.id.my_job_title);
    tmpString = newUser.getTitle();
    if (tmpString != null)
      name.setText(tmpString);
    name = (TextView) rootView.findViewById(R.id.my_add);
    tmpString = newUser.getCity();
    if (tmpString != null)
      name.setText(tmpString);
    ImageView portraitImg = (ImageView) rootView.findViewById(R.id.my_portrait);
    if (newUser.getPortrait() != null) {
      portraitImg.setImageBitmap(newUser.getPortrait());
    }

  }

  @Override
  public void update(UserInfo userInfo) {
    if (getArguments().getInt(ARG_SECTION_NUMBER, 1) == 1) {
      // update the main card
      displayCard(rootView, userInfo);
    }
    if (getArguments().getInt(ARG_SECTION_NUMBER, 1) == 2) {
      // For unknown reason, trying to rootView.findViewById(anything) will
      // crash
      // Luckily QR code doesn't need to change noticeably. Next time app
      // restarts
      // Changes in QR code can be reflected
    }

  }
}
