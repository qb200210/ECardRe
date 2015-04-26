package com.micklestudios.knowell;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import com.micklestudios.knowell.infrastructure.UserInfo;
import com.micklestudios.knowell.utils.CurvedAndTiled;
import com.micklestudios.knowell.utils.ECardUtils;
import com.micklestudios.knowell.utils.ExpandableHeightGridView;
import com.micklestudios.knowell.utils.MyGridViewAdapter;
import com.micklestudios.knowell.utils.MyScrollView;
import com.micklestudios.knowell.utils.MySimpleListViewAdapter;
import com.micklestudios.knowell.utils.MyTag;
import com.micklestudios.knowell.utils.SquareLayout;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.micklestudios.knowell.R;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ActivityDesign extends ActionBarActivity {

  ParseUser currentUser;
  private MyScrollView scrollView;
  private static final int CROP_FROM_CAMERA = 2;
  private static final int SELECT_PORTRAIT = 100;
  private static final int TAKE_IMAGE = 250;
  private static final int SELECT_LOGO = 200;
  private static int currentSource = 0;
  private Uri selectedImage, mCurrentPhotoPath;

  Bitmap photo = null;
  byte[] tmpImgData = null; // temporary storage of byte array for cropped img
  ParseFile file = null;
  boolean portraitChanged = false;

  // dummy array, will be replaced by extra info items
  private ArrayList<Integer> infoIcon = new ArrayList<Integer>();
  private ArrayList<String> infoLink = new ArrayList<String>();
  ArrayList<String> shownArrayList = new ArrayList<String>();
  String[] allowedArray = { "about", "linkedin", "phone", "message", "email",
    "facebook", "twitter", "googleplus", "web" };
  String[] allowedDisplayArray = { "About Me", "LinkedIn", "Phone", "Message",
    "Email", "Facebook", "Twitter", "Google +", "Web Link" };
  ArrayList<String> allowedArrayList = new ArrayList<String>(
    Arrays.asList(allowedArray));
  ArrayList<String> selectionArrayList = new ArrayList<String>(
    Arrays.asList(allowedArray));
  ArrayList<String> selectionDisplayArrayList = new ArrayList<String>(
    Arrays.asList(allowedDisplayArray));
  // The use of treeset is only to order selectionlist
  TreeSet<String> selectionTreeSet = new TreeSet<String>();
  TreeSet<String> selectionDisplayTreeSet = new TreeSet<String>();
  String[] selectionArray;
  String[] selectionDisplayArray;
  AlertDialog actions;
  ExpandableHeightGridView gridView;
  
  public static ArrayList<String> companyNames;
  ParseObject templateToBePinned = null;
  protected boolean flagLogoSet = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_design);
    scrollView = (MyScrollView) findViewById(R.id.design_scroll_view);
    scrollView.setmScrollable(true);

    currentUser = ParseUser.getCurrentUser();
    Bundle data = getIntent().getExtras();
    displayMyCard();

    // complete list of possible extrainfo items
    infoIcon = ActivityMain.myselfUserInfo.getInfoIcon();
    infoLink = ActivityMain.myselfUserInfo.getInfoLink();
    shownArrayList = ActivityMain.myselfUserInfo.getShownArrayList();

    for (int i = 0; i < allowedArray.length; i++) {
      // the extra info item
      String item = allowedArray[i];
      // the value of this extra info item
      if (shownArrayList.contains(item)) {
        // remove already added items from selection list
        int locToRm = selectionArrayList.indexOf(item);
        selectionArrayList.remove(locToRm);
        selectionDisplayArrayList.remove(locToRm);
      }
    }
    Log.i("DisplayArrayList", selectionDisplayArrayList.toString());
    // create ordered selection list using TreeSet
    selectionTreeSet.addAll(selectionArrayList);
    selectionDisplayTreeSet.addAll(selectionDisplayArrayList);

    // Add the last button as "add more" button
    infoIcon.add(R.drawable.addmore);
    infoLink.add("addmore");
    shownArrayList.add("addmore");

    // convert ordered TreeSet into array to be used by dialogbuilder
    selectionArray = (String[]) selectionTreeSet.toArray(new String[0]);
    selectionDisplayArray = (String[]) selectionDisplayTreeSet
      .toArray(new String[0]);

    Log.i("DisplayArray", Arrays.toString(selectionDisplayArray));
    // Upon initialization, build dialogAddMore for the first time
    buildAddMoreButtonDialog();

    // The gridView to display extra info items
    gridView = (ExpandableHeightGridView) findViewById(R.id.gridView1);
    gridView.setAdapter(new MyGridViewAdapter(getBaseContext(), shownArrayList,
      infoLink, infoIcon));
    // magically the onItemClickListener on gridView only needs to be specified
    // once
    // Even upon gridView items changes, this listener still works well
    gridView.setOnItemClickListener(gridViewItemClickListenerBuilder());

    // This is the life-saver! It fixes the bug that scrollView will go to the
    // bottom of GridView upon open
    // below is to re-scroll to the first view in the LinearLayout
    SquareLayout mainCardContainer = (SquareLayout) findViewById(R.id.main_card_container);
    scrollView.requestChildFocus(mainCardContainer, null);

    ImageView pbPortrait = (ImageView) findViewById(R.id.design_portrait);
    pbPortrait.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        displaySourceDialog();
      }
    });

  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    if (resultCode == RESULT_OK && requestCode == TAKE_IMAGE) {
      selectedImage = mCurrentPhotoPath;
      /*
       * String[] filePathColumn = { MediaStore.Images.Media.DATA };
       * 
       * Cursor cursor = getContentResolver().query(selectedImage,
       * filePathColumn, null, null, null); cursor.moveToFirst();
       * 
       * int columnIndex = cursor.getColumnIndex(filePathColumn[0]); String
       * picturePath = cursor.getString(columnIndex); cursor.close();
       * 
       * BitmapFactory.Options options = new BitmapFactory.Options();
       * options.inJustDecodeBounds = true;
       */
      doCrop();
    } else if (resultCode == RESULT_OK && null != data
      && requestCode == CROP_FROM_CAMERA) {
      Bundle extras = data.getExtras();
      if (extras != null) {
        photo = extras.getParcelable("data");
        ImageView ImageView1 = (ImageView) findViewById(R.id.design_portrait);
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

  private Integer iconSelector(String key) {
    // input key to select corresponding icon to display on button
    switch (key) {
    case "email":
      return R.drawable.mail;
    case "facebook":
      return R.drawable.facebook;
    case "linkedin":
      return R.drawable.linkedin;
    case "twitter":
      return R.drawable.twitter;
    case "phone":
      return R.drawable.phone;
    case "message":
      return R.drawable.message;
    case "about":
      return R.drawable.me;
    case "googleplus":
      return R.drawable.googleplus;
    case "web":
      return R.drawable.web;
    default:
      return R.drawable.ic_action_discard;
    }
  }

  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.design_actionbar, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // this function is called when either action bar icon is tapped
    switch (item.getItemId()) {
    case R.id.design_discard:
      Toast.makeText(this, "Discarded changes!", Toast.LENGTH_SHORT).show();
      setResult(RESULT_CANCELED);
      this.finish();
      return true;
    case R.id.design_save:
      // construct the updatedUserInfo to be sent back to ActivityMain
      saveChangesToUserInfo();

      ParseQuery<ParseObject> query = ParseQuery.getQuery("ECardInfo");
      query.fromLocalDatastore();
      query.getInBackground(currentUser.get("ecardId").toString(),
        new GetCallback<ParseObject>() {

          @Override
          public void done(final ParseObject object, ParseException e) {
            if (e == null) {
              if (object != null) {
                // the file should not be empty
                if (!portraitChanged) {
                  saveChangesToParse(object);
                } else {
                  // If no internet, save portrait to parse as byte array, then
                  // later convert to parse file
                  if (!ECardUtils.isNetworkAvailable(ActivityDesign.this)) {
                    Toast.makeText(ActivityDesign.this,
                      "No network, caching img", Toast.LENGTH_SHORT).show();
                    object.put("tmpImgByteArray", tmpImgData);
                    // flush sharedpreferences to 1969 so next time app opens
                    // with internet, convert the file
                    Date currentDate = new Date(0);
                    SharedPreferences prefs = getSharedPreferences(
                      ActivityBufferOpening.MY_PREFS_NAME, MODE_PRIVATE);
                    SharedPreferences.Editor prefEditor = prefs.edit();
                    prefEditor.putLong("DateSelfSynced", currentDate.getTime());
                    prefEditor.commit();
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

          private void saveChangesToParse(ParseObject object) {
            if (!ECardUtils.isNetworkAvailable(ActivityDesign.this)) {
              // if there is network, pin the ECardTemplate object to local
              if(templateToBePinned != null){
                templateToBePinned.pinInBackground();
              }
            }
            
            EditText name = (EditText) findViewById(R.id.design_name);
            String fullName = name.getText().toString();
            String[] splitName = fullName.split(" ");
            String firstName = "";
            String lastName = "";
            Toast.makeText(getApplicationContext(), splitName.length + " ll",
              Toast.LENGTH_SHORT).show();
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

            AutoCompleteTextView cmpName = (AutoCompleteTextView) findViewById(R.id.design_com);
            object.put("company", cmpName.getText().toString());
            name = (EditText) findViewById(R.id.design_job_title);
            object.put("title", name.getText().toString());
            name = (EditText) findViewById(R.id.design_address);
            object.put("city", name.getText().toString());

            ArrayList<String> remainedList = new ArrayList<String>();
            int numBtns = gridView.getChildCount() - 1;
            for (int i = 0; i < numBtns; i++) {
              View view = gridView.getChildAt(i);
              // Log.d("buttons:" , ((MyTag) view.getTag()).getKey() +
              // "   "+ ((MyTag) view.getTag()).getValue());
              object.put(((MyTag) view.getTag()).getKey(),
                ((MyTag) view.getTag()).getValue());
              remainedList.add(((MyTag) view.getTag()).getKey());
            }
            allowedArrayList.removeAll(remainedList);
            for (Iterator<String> iter = allowedArrayList.iterator(); iter
              .hasNext();) {
              String nullItem = iter.next();
              object.remove(nullItem);
            }
            object.saveEventually();
            Toast.makeText(getBaseContext(), "Save successful",
              Toast.LENGTH_SHORT).show();

          }

        });

      // need to pass this new UserInfo back to ActivityMain. Cannot wait for
      // object.saveinbackground.
      Intent intent = new Intent();
      // here myselfUserInfo has been updated
      setResult(RESULT_OK, intent);
      this.finish();
      return true;
    default:
      return super.onOptionsItemSelected(item);
    }
  }

  private void saveChangesToUserInfo() {
    if (portraitChanged) {
      ActivityMain.myselfUserInfo.setPortrait(photo);
    }
    EditText name = (EditText) findViewById(R.id.design_name);
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
    ActivityMain.myselfUserInfo.setFirstName(firstName);
    ActivityMain.myselfUserInfo.setLastName(lastName);

    AutoCompleteTextView cmpName = (AutoCompleteTextView) findViewById(R.id.design_com);
    ActivityMain.myselfUserInfo.setCompany(cmpName.getText().toString());
    name = (EditText) findViewById(R.id.design_job_title);
    ActivityMain.myselfUserInfo.setTitle(name.getText().toString());
    name = (EditText) findViewById(R.id.design_address);
    ActivityMain.myselfUserInfo.setCity(name.getText().toString());
    infoIcon.remove(infoIcon.size() - 1);
    infoLink.remove(infoLink.size() - 1);
    shownArrayList.remove(shownArrayList.size() - 1);
    ActivityMain.myselfUserInfo.setInfoIcon(infoIcon);
    ActivityMain.myselfUserInfo.setInfoLink(infoLink);
    ActivityMain.myselfUserInfo.setShownArrayList(shownArrayList);
  }

  OnItemClickListener gridViewItemClickListenerBuilder() {
    OnItemClickListener listener = new OnItemClickListener() {

      @SuppressLint("NewApi")
      @Override
      public void onItemClick(AdapterView<?> parent, final View view,
        final int position, long id) {
        if (position == infoLink.size() - 1) {
          // This is the last item in the gridView, representing "addmore"
          // button
          // Upon click, opens dialogAddMore
          // actions defined in buildAddMoreButtonDialog();
          actions.show();
        } else {
          String item = shownArrayList.get(position);
          int loc = allowedArrayList.indexOf(item);

          // setup dialog Views, which is the actual layout

          final View dialogPerItemView = setupDialogView(
            allowedDisplayArray[loc].toString(), ((MyTag) view.getTag())
              .getValue().toString());

          // display the actual dialog
          new AlertDialog.Builder(ActivityDesign.this)
            .setView(dialogPerItemView)
            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int whichButton) {
                EditText dialogText = (EditText) dialogPerItemView
                  .findViewById(R.id.dialog_text);
                Editable value = dialogText.getText();
                // update the tag of the view with updated values
                view.setTag(new MyTag(((MyTag) view.getTag()).getKey()
                  .toString(), value.toString()));
                // update the link contents
                infoLink.remove(position);
                infoLink.add(position, value.toString());
              }
            })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int whichButton) {
                // Do nothing.
              }
            })
            .setNeutralButton("Delete", new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int whichButton) {
                // remove the button from gridview
                infoLink.remove(position);
                infoIcon.remove(position);
                String item = shownArrayList.get(position);
                shownArrayList.remove(position);
                // add the removed option to selection candidate
                // list
                selectionTreeSet.add(item);
                int loc = allowedArrayList.indexOf(item);
                selectionDisplayTreeSet.add(allowedDisplayArray[loc]);
                // convert ordered TreeSet into array to be used by
                // dialogbuilder
                selectionArray = (String[]) selectionTreeSet
                  .toArray(new String[0]);
                selectionDisplayArray = (String[]) selectionDisplayTreeSet
                  .toArray(new String[0]);

                buildAddMoreButtonDialog();

                // make a new adapter with one less item
                MyGridViewAdapter updatedAdapter = new MyGridViewAdapter(
                  getBaseContext(), shownArrayList, infoLink, infoIcon);
                gridView.setAdapter(updatedAdapter);
                // Refresh the gridView to display the new item
                updatedAdapter.notifyDataSetChanged();
              }

            }).show();
        }
      }

    };
    return listener;

  }

  OnItemClickListener dialogAddMoreListItemClickListenerBuilder() {
    OnItemClickListener listener = new OnItemClickListener() {
      // This is the Listener that listens to the item selection in
      // dialogAddMore
      // Upon selection, pop up a new dialog to edit initial info of the to be
      // added item

      @SuppressLint("NewApi")
      @Override
      public void onItemClick(AdapterView<?> parent, View view,
        final int position, long id) {
        // Upon dialogAddMore item selection, it can be closed
        actions.dismiss();

        // setup dialog Views, which is the actual layout
        final View dialogPerItemView = setupDialogView(
          selectionDisplayArray[position].toString(), "");

        // Build the actual peritem dialog
        new AlertDialog.Builder(ActivityDesign.this).setView(dialogPerItemView)
          .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @SuppressLint("NewApi")
            public void onClick(DialogInterface dialog, int whichButton) {
              EditText dialogText = (EditText) dialogPerItemView
                .findViewById(R.id.dialog_text);
              Editable value = dialogText.getText();
              infoLink.add(infoLink.size() - 1, value.toString());
              // The reason array instead of TreeSet must be used: TreeSet has
              // no get()
              infoIcon.add(infoIcon.size() - 1,
                iconSelector(selectionArray[position]));
              // add to exiting list
              shownArrayList.add(shownArrayList.size() - 1,
                selectionArray[position]);

              List<String> list = new ArrayList<String>(Arrays
                .asList(selectionArray));
              selectionTreeSet.remove(list.get(position));
              list.remove(position);
              selectionArray = list.toArray(new String[0]);
              list = new ArrayList<String>(Arrays.asList(selectionDisplayArray));
              selectionDisplayTreeSet.remove(list.get(position));
              list.remove(position);
              selectionDisplayArray = list.toArray(new String[0]);

              // When this new item is added, the available items list is
              // changed,
              // this should be reflected in the change of dialogAddMore
              buildAddMoreButtonDialog();

              // make a new adapter with one more item included
              MyGridViewAdapter updatedAdapter = new MyGridViewAdapter(
                getBaseContext(), shownArrayList, infoLink, infoIcon);
              gridView.setAdapter(updatedAdapter);
              // Refresh the gridView to display the new item
              updatedAdapter.notifyDataSetChanged();
            }
          }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
              // Do nothing.
            }
          }).show();

      }

    };

    return listener;
  }

  @SuppressLint("NewApi")
  protected View setupDialogView(String title, String text) {
    // Get the layout inflater
    LayoutInflater inflater = getLayoutInflater();
    View dialogView = inflater.inflate(R.layout.layout_dialog_design_peritem,
      null);
    LinearLayout dialogHeader = (LinearLayout) dialogView
      .findViewById(R.id.dialog_header);
    final EditText dialogText = (EditText) dialogView
      .findViewById(R.id.dialog_text);
    TextView dialogTitle = (TextView) dialogView
      .findViewById(R.id.dialog_title);
    // Set dialog header background with rounded corner
    Bitmap bm = BitmapFactory
      .decodeResource(getResources(), R.drawable.striped);
    BitmapDrawable bmDrawable = new BitmapDrawable(getResources(), bm);
    dialogHeader.setBackground(new CurvedAndTiled(bmDrawable.getBitmap(), 5));
    // Set dialog title and main EditText
    dialogTitle.setText("Edit " + title);
    dialogText.setText(text);
    return dialogView;
  }

  public void displayMyCard() {
    TextView name = (TextView) findViewById(R.id.design_name);
    String tmpString = ActivityMain.myselfUserInfo.getFirstName();
    String nameString = null;
    if (tmpString != null)
      nameString = tmpString;
    tmpString = ActivityMain.myselfUserInfo.getLastName();
    if (tmpString != null)
      nameString = nameString + " " + tmpString;
    if (nameString != null)
      name.setText(nameString);
    
    
    name = (TextView) findViewById(R.id.design_job_title);
    name.setText(ActivityMain.myselfUserInfo.getTitle());
    name = (TextView) findViewById(R.id.design_address);
    name.setText(ActivityMain.myselfUserInfo.getCity());
    ImageView portraitImg = (ImageView) findViewById(R.id.design_portrait);
    portraitImg.setImageBitmap(ActivityMain.myselfUserInfo.getPortrait());
    
    final ImageView logoImg = (ImageView) findViewById(R.id.design_logo);
    final AutoCompleteTextView cmpName = (AutoCompleteTextView) findViewById(R.id.design_com);
    cmpName.setText(ActivityMain.myselfUserInfo.getCompany());
    ECardUtils.findAndSetLogo(ActivityDesign.this,logoImg, cmpName.getText().toString(), true);
    
    Log.i("autoc", companyNames.toString());
    ArrayAdapter<String> adapterCompanyNames = new ArrayAdapter<String>(this,
      android.R.layout.select_dialog_item, companyNames);
    cmpName.setAdapter(adapterCompanyNames);
    cmpName.setOnItemClickListener(new OnItemClickListener(){

      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position,
        long id) {
        ECardUtils.findAndSetLogo(ActivityDesign.this, logoImg, parent.getItemAtPosition(position).toString(), true);
      }
      
    });
    cmpName.setOnFocusChangeListener(new OnFocusChangeListener() {          

      public void onFocusChange(View v, boolean hasFocus) {
          if (!hasFocus) {
            // code to execute when EditText loses focus
            ECardUtils.findAndSetLogo(ActivityDesign.this, logoImg, cmpName.getText().toString(), true);
          }
      }
    });
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
      .setSingleChoiceItems(source, currentSource,
        new DialogInterface.OnClickListener() {

          @Override
          public void onClick(DialogInterface dialog, int which) {
            currentSource = which;

            switch (currentSource) {
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
    final ArrayList<CropOption> cropOptions = new ArrayList<CropOption>();

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

  @SuppressLint("NewApi")
  private void buildAddMoreButtonDialog() {
    // Get the layout inflater
    LayoutInflater inflater = getLayoutInflater();
    View dialogAddMoreView = inflater.inflate(R.layout.layout_dialog_addmore,
      null);
    LinearLayout dialogHeader = (LinearLayout) dialogAddMoreView
      .findViewById(R.id.dialog_header);
    TextView dialogTitle = (TextView) dialogAddMoreView
      .findViewById(R.id.dialog_title);
    // Set dialog header background with rounded corner
    Bitmap bm = BitmapFactory
      .decodeResource(getResources(), R.drawable.striped);
    BitmapDrawable bmDrawable = new BitmapDrawable(getResources(), bm);
    dialogHeader.setBackgroundDrawable(new CurvedAndTiled(bmDrawable
      .getBitmap(), 5));
    // Set dialog title and main EditText
    dialogTitle.setText("Add Info");

    AlertDialog.Builder builder = new AlertDialog.Builder(ActivityDesign.this);
    builder.setView(dialogAddMoreView);
    builder.setNegativeButton("Cancel", null);
    // actions now links to the dialog
    actions = builder.create();
    Log.i("dialog", Arrays.toString(selectionDisplayArray));
    Log.i("infoIcon", infoIcon.toString());
    ArrayList<Integer> iconListForAddMoreDialog = new ArrayList<Integer>();
    for (int i = 0; i < selectionDisplayArray.length; i++) {
      // the extra info item
      String item = selectionDisplayArray[i];
      // the value of this extra info item
      List<String> allowedDisplayArrayList = Arrays.asList(allowedDisplayArray);

      if (allowedDisplayArrayList.contains(selectionDisplayArray[i])) {
        // remove already added items from selection list
        int locToRm = allowedDisplayArrayList.indexOf(item);
        iconListForAddMoreDialog.add(iconSelector(allowedArray[locToRm]));
      }
    }
    Log.i("arraylist", selectionDisplayArrayList.toString());

    // Below is to build the listener for items listed inside the poped up
    // "addmorebutton dialog"
    ListView listViewInDialog = (ListView) dialogAddMoreView
      .findViewById(R.id.dialog_listview);
    listViewInDialog.setAdapter(new MySimpleListViewAdapter(
      ActivityDesign.this, selectionDisplayArray, iconListForAddMoreDialog));
    listViewInDialog
      .setOnItemClickListener(dialogAddMoreListItemClickListenerBuilder());
  }
}
