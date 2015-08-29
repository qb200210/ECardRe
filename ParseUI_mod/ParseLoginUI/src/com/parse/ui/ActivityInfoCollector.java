package com.parse.ui;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import com.parse.GetCallback;
import com.parse.ParseACL;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class ActivityInfoCollector extends ActionBarActivity {
  

  private static final int CROP_FROM_CAMERA = 2;
  private static final int SELECT_PORTRAIT = 100;
  private static final int TAKE_IMAGE = 250;
  private static final int SELECT_LOGO = 200;
  private static int CURRENT_SOURCE = 0;
  
  Bitmap photo = null;
  byte[] tmpImgData = null; // temporary storage of byte array for cropped img
  ParseFile file = null;
  boolean portraitChanged = false;

  private Uri selectedImage, mCurrentPhotoPath;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.info_collector);
    
    ImageView pbPortrait = (ImageView) findViewById(R.id.collect_portrait);
    pbPortrait.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        displaySourceDialog();
      }
    });
        
    Button nextButton = (Button) findViewById(R.id.collect_done);
    nextButton.setOnClickListener(new OnClickListener(){

      @Override
      public void onClick(View v) {
        ParseUser currentUser = ParseUser.getCurrentUser();
        ParseQuery<ParseObject> query = ParseQuery.getQuery("ECardInfo");
        query.fromLocalDatastore();
        query.getInBackground(currentUser.get("ecardId").toString(),
          new GetCallback<ParseObject>() {

            @Override
            public void done(final ParseObject object, ParseException e) {
              if( e == null) {
                if ( object != null){
                  if (!portraitChanged) {
                    saveChangesToParse(object);
                  } else {
                    // If no internet, save portrait to parse as byte array, then
                    // later convert to parse file
                    if (!isNetworkAvailable((Activity)ParseLoginBuilder.context)) {
                      object.put("tmpImgByteArray", tmpImgData);
                      // flush sharedpreferences to 1969 so next time app opens
                      // with internet, convert the file
                      
                      saveChangesToParse(object);
                    } else {
                      // with network, can save parsefile like normal
                      // have to do saveInBackground, otherwise hang and crash in
                      // poor networks
                      file = new ParseFile("portrait.jpg", tmpImgData);
                      file.saveInBackground(new SaveCallback() {

                        @Override
                        public void done(ParseException arg0) {
                          object.put("portrait", file);
                          saveChangesToParse(object);
                        }

                      });
                    }
                  }
                }
              }
            }
          
        });
        setResult(RESULT_OK);
        finish();
      }
      
    });
    
    showActionBar();
  }
  
  private void showActionBar() {
    LayoutInflater inflator = (LayoutInflater) this
      .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    View v = inflator.inflate(R.layout.layout_actionbar_infocollect, null);
    TextView title = (TextView) v.findViewById(R.id.collect_actionbar_title);
    title.setText("Complete your profile");
    Button skipButton = (Button) v.findViewById(R.id.collect_skip_button);
    skipButton.setOnClickListener(new OnClickListener(){

      @Override
      public void onClick(View v) {
        setResult(RESULT_OK);
        finish();
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
  
  private void saveChangesToParse(ParseObject object) {

    EditText name = (EditText) findViewById(R.id.collect_name);
    String fullName = name.getText().toString();
    String[] splitName = fullName.split(" ");
    String firstName = "";
    String lastName = "";
    if (splitName.length > 1) {
      for (int i = 0; i < splitName.length - 2; i++) {
        firstName = firstName + splitName[i] + " ";
      }
      for (int i = splitName.length - 2; i < splitName.length - 1; i++) {
        firstName = firstName + splitName[i];
      }
      lastName = splitName[splitName.length - 1];
    } else {
      firstName = splitName[0];
    }
    object.put("firstName", firstName);
    object.put("lastName", lastName);

    EditText cmpName = (EditText) findViewById(R.id.collect_company);
 
    
    
    if(cmpName!=null){
      object.put("company", cmpName.getText().toString());
      
      // Testing beforeSave for ECardTemplate Objects
      ParseObject newTemplateObj = new ParseObject("ECardTemplate");
      newTemplateObj.put("companyName", cmpName.getText().toString().trim());
      newTemplateObj.put("companyNameLC", cmpName.getText().toString().toLowerCase(Locale.ENGLISH).trim());
      ParseACL defaultACL = new ParseACL();
      defaultACL.setPublicReadAccess(true);
      defaultACL.setPublicWriteAccess(false);
      newTemplateObj.setACL(defaultACL);
      newTemplateObj.saveEventually();
    }
    name = (EditText) findViewById(R.id.collect_position);
    if(cmpName!=null){
      object.put("title", name.getText().toString());
    }
    name = (EditText) findViewById(R.id.collect_city);
    if(cmpName!=null){
      object.put("city", name.getText().toString());
    }

    object.saveEventually();
    Toast.makeText(getBaseContext(), "Save successful",
      Toast.LENGTH_SHORT).show();

  }
  
  @SuppressWarnings("deprecation")
  public static Bitmap decodeSampledBitmapFromFile(String picturePath,
    int reqWidth, int reqHeight) {

    // First decode with inJustDecodeBounds=true to check dimensions
    final BitmapFactory.Options options = new BitmapFactory.Options();
    options.inJustDecodeBounds = true;
    BitmapFactory.decodeFile(picturePath, options);

    // Calculate inSampleSize
    int photoW = options.outWidth;
    int photoH = options.outHeight;

    int scaleFactor = Math.min(photoW / reqWidth, photoH / reqHeight);

    options.inJustDecodeBounds = false;
    options.inSampleSize = scaleFactor;
    options.inPurgeable = true;
    options.inJustDecodeBounds = false;
    return BitmapFactory.decodeFile(picturePath, options);
  }

  private void displaySourceDialog() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    String source[] = { "Gallary", "Camera" };

    builder
      .setTitle("Select Image From:")
      .setSingleChoiceItems(source, CURRENT_SOURCE,
        new DialogInterface.OnClickListener() {

          @Override
          public void onClick(DialogInterface dialog, int which) {
            CURRENT_SOURCE = which;

            switch (CURRENT_SOURCE) {
            case 0:
              Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
              photoPickerIntent.setType("image/*");
              startActivityForResult(photoPickerIntent, SELECT_PORTRAIT);

              break;

            case 1:
              Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
              // Peng: By default, the taken image will be stored in Gallery.
              // Can we directly use it
              // instead of creating photoFile?
              // startActivityForResult(takePicture, TAKE_IMAGE);
              // selectedImage = Uri.fromFile(new
              // File(Environment.getExternalStorageDirectory(),"tmp_avatar_" +
              // String.valueOf(System.currentTimeMillis()) + ".jpg"));
              // takePicture.putExtra(android.provider.MediaStore.EXTRA_OUTPUT,
              // selectedImage);
              // takePicture.putExtra("Uri", selectedImage);
              // takePicture.putExtra("return-data", true);
              // startActivityForResult(takePicture, TAKE_IMAGE);
              // if (takePicture.resolveActivity(getPackageManager()) != null) {
              // startActivityForResult(takePicture, TAKE_IMAGE);
              // }

              if (takePicture.resolveActivity(getPackageManager()) != null) {
                // Create the File where the photo should go
                File photoFile = null;
                try {
                  photoFile = createImageFile();
                } catch (IOException ex) {
                  // Error occurred while creating the File
                  // ...
                }
                // Continue only if the File was successfully created
                if (photoFile != null) {
                  takePicture.putExtra(MediaStore.EXTRA_OUTPUT,
                    Uri.fromFile(photoFile));
                  startActivityForResult(takePicture, TAKE_IMAGE);
                }
              }
              break;

            }

            dialog.dismiss();
          }
        }).show();
  }

  private File createImageFile() throws IOException {
    // Create an image file name
    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
      .format(new Date());
    String imageFileName = "JPEG_" + timeStamp + "_";
    File storageDir = Environment
      .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
    File image = File.createTempFile(imageFileName, /* prefix */
      ".jpg", /* suffix */
      storageDir /* directory */
    );

    // Save a file: path for use with ACTION_VIEW intents
    mCurrentPhotoPath = Uri.fromFile(image);
    return image;
  }

  private void doCrop() {

    Intent intent = new Intent("com.android.camera.action.CROP");
    intent.setType("image/*");

    List<ResolveInfo> list = getPackageManager().queryIntentActivities(intent,
      0);

    int size = list.size();

    if (size == 0) {
      Toast.makeText(this, "Can not find image crop app", Toast.LENGTH_SHORT)
        .show();

      return;
    } else {
      intent.setData(selectedImage);

      intent.putExtra("outputX", 200);
      intent.putExtra("outputY", 200);
      intent.putExtra("aspectX", 1);
      intent.putExtra("aspectY", 1);
      intent.putExtra("scale", true);
      intent.putExtra("return-data", true);

      if (size >= 1) {
        Intent i = new Intent(intent);
        ResolveInfo res = list.get(0);

        i.setComponent(new ComponentName(res.activityInfo.packageName,
          res.activityInfo.name));

        startActivityForResult(i, CROP_FROM_CAMERA);
      }
    }
  }
  
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    if (resultCode == RESULT_OK && requestCode == TAKE_IMAGE) {
      selectedImage = mCurrentPhotoPath;
      doCrop();
    } else if (resultCode == RESULT_OK && null != data
      && requestCode == CROP_FROM_CAMERA) {
      Bundle extras = data.getExtras();
      if (extras != null) {
        photo = extras.getParcelable("data");
        ImageView ImageView1 = (ImageView) findViewById(R.id.collect_portrait);
        ImageView1.setImageBitmap(photo);
        // converting Bitmap to byte array
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        photo.compress(Bitmap.CompressFormat.PNG, 100, stream);
        tmpImgData = stream.toByteArray();
        portraitChanged = true;
      }
      File f = new File(selectedImage.getPath());
      if (f.exists())
        f.delete();
    } else {
      if (resultCode == RESULT_OK && null != data) {
        selectedImage = data.getData();
        String[] filePathColumn = { MediaStore.Images.Media.DATA };

        Cursor cursor = getContentResolver().query(selectedImage,
          filePathColumn, null, null, null);
        cursor.moveToFirst();

        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        String picturePath = cursor.getString(columnIndex);
        cursor.close();

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        switch (requestCode) {
        case SELECT_PORTRAIT:
          doCrop();
          break;

        case SELECT_LOGO:
          break;

        }
      }
    }

  }
  
  public static boolean isNetworkAvailable(Activity activity) {
    ConnectivityManager connectivityManager = (ConnectivityManager) activity
      .getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
    return activeNetworkInfo != null && activeNetworkInfo.isConnected();
  }
}
