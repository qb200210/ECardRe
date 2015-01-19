package com.warpspace.ecardv4;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.warpspace.ecardv4.R;
import com.warpspace.ecardv4.utils.CurvedAndTiled;
import com.warpspace.ecardv4.utils.ExpandableHeightGridView;
import com.warpspace.ecardv4.utils.MyGridViewAdapter;
import com.warpspace.ecardv4.utils.MySimpleListViewAdapter;
import com.warpspace.ecardv4.utils.MyTag;
import com.warpspace.ecardv4.utils.MyScrollView;
import com.warpspace.ecardv4.utils.SquareLayout;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ActivityDesign extends ActionBarActivity {

	ParseUser currentUser;
	private MyScrollView scrollView;
	private static final int CROP_FROM_CAMERA = 2;
	private static final int SELECT_PORTRAIT = 100;
	private static final int TAKE_IMAGE=250;
	private static final int SELECT_LOGO = 200;
	private static int currentSource=0;
	private Uri selectedImage;
	
	// dummy array, will be replaced by extra info items
	private ArrayList<Integer> infoIcon = new ArrayList<Integer>();
	private ArrayList<String> infoLink = new ArrayList<String>();
	ArrayList<String> shownArrayList = new ArrayList<String>();
	String[] allowedArray = { "about", "linkedin", "phone", "message", "email", "facebook", "twitter", "googleplus", "web" };
	String[] allowedDisplayArray = { "About Me", "LinkedIn", "Phone", "Message", "Email", "Facebook", "Twitter", "Google +", "Web Link" };
	ArrayList<String> allowedArrayList = new ArrayList<String>(Arrays.asList(allowedArray));
	ArrayList<String> selectionArrayList = new ArrayList<String>(Arrays.asList(allowedArray));
	ArrayList<String> selectionDisplayArrayList = new ArrayList<String>(Arrays.asList(allowedDisplayArray));
	// The use of treeset is only to order selectionlist
	TreeSet<String> selectionTreeSet = new TreeSet<String>();
	TreeSet<String> selectionDisplayTreeSet = new TreeSet<String>();
	String[] selectionArray;
	String[] selectionDisplayArray;
	AlertDialog actions;
	ExpandableHeightGridView gridView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_design);
		scrollView = (MyScrollView) findViewById(R.id.design_scroll_view);
		scrollView.setmScrollable(true);

		currentUser = ParseUser.getCurrentUser();
		displayMyCard();

		// complete list of possible extrainfo items
		infoIcon.clear();
		infoLink.clear();

		ParseQuery<ParseObject> query = ParseQuery.getQuery("ECardInfo");
		query.fromLocalDatastore();
		query.getInBackground(currentUser.get("ecardId").toString(), new GetCallback<ParseObject>() {

			@SuppressLint("NewApi")
			@Override
			public void done(ParseObject object, ParseException e) {
				if (e == null) {
					if (object != null) {
						for (int i = 0; i < allowedArray.length; i++) {
							// the extra info item
							String item = allowedArray[i];
							// the value of this extra info item
							Object value = object.get(item);
							if (value != null && value.toString() != "") {
								infoIcon.add(iconSelector(item));
								infoLink.add(value.toString());
								// note down the existing extra info items
								shownArrayList.add(item);
								// remove already added items from selection list
								int locToRm = selectionArrayList.indexOf(item);
								selectionArrayList.remove(locToRm);
								selectionDisplayArrayList.remove(locToRm);
							}
						}

						// create ordered selection list using TreeSet
						selectionTreeSet.addAll(selectionArrayList);
						selectionDisplayTreeSet.addAll(selectionDisplayArrayList);

						// Add the last button as "add more" button
						infoIcon.add(R.drawable.addmore);
						infoLink.add("addmore");
						shownArrayList.add("addmore");

						// convert ordered TreeSet into array to be used by dialogbuilder
						selectionArray = (String[]) selectionTreeSet.toArray(new String[0]);
						selectionDisplayArray = (String[]) selectionDisplayTreeSet.toArray(new String[0]);
						
						// Upon initialization, build dialogAddMore for the first time
						buildAddMoreButtonDialog();

						// The gridView to display extra info items
						gridView = (ExpandableHeightGridView) findViewById(R.id.gridView1);
						gridView.setAdapter(new MyGridViewAdapter(getBaseContext(), shownArrayList, infoLink, infoIcon));
						// magically the onItemClickListener on gridView only needs to be specified once
						// Even upon gridView items changes, this listener still works well
						gridView.setOnItemClickListener(gridViewItemClickListenerBuilder());
					}
				}
			}
		});

		// This is the life-saver! It fixes the bug that scrollView will go to the
		// bottom of GridView upon open
		// below is to re-scroll to the first view in the LinearLayout
		SquareLayout mainCardContainer = (SquareLayout) findViewById(R.id.main_card_container);
		scrollView.requestChildFocus(mainCardContainer, null);

		ImageButton pbPortrait = (ImageButton) findViewById(R.id.design_PortraitButton);
		pbPortrait.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				displaySourceDialog();
			}
		});
		ImageButton pbLogo = (ImageButton) findViewById(R.id.design_CompanyLogo);
		pbLogo.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Toast.makeText(ActivityDesign.this, "Select Image", Toast.LENGTH_LONG).show();
				Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
				photoPickerIntent.setType("image/*");
				startActivityForResult(photoPickerIntent, SELECT_LOGO);
			}
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if(requestCode == TAKE_IMAGE){
			Bundle extras = data.getExtras();
			Bitmap imageBitmap =(Bitmap) extras.get("data");
			ImageButton imageButton = (ImageButton) findViewById(R.id.design_PortraitButton);
			imageButton.setImageBitmap(imageBitmap);
			//selectedImage = (Uri) extras.get("Uri");
			
	        //doCrop();
		}
		else if(requestCode == CROP_FROM_CAMERA){
			Bundle extras = data.getExtras();
			if (extras != null) {
				Bitmap photo = extras.getParcelable("data");
				ImageButton imageButton1 = (ImageButton) findViewById(R.id.design_PortraitButton);
				imageButton1.setImageBitmap(photo);
			}
			File f = new File(selectedImage.getPath());
			if (f.exists()) f.delete();
		}
		else {
		if (resultCode == RESULT_OK && null != data) {
			selectedImage = data.getData();
			String[] filePathColumn = { MediaStore.Images.Media.DATA };

			Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
			cursor.moveToFirst();

			int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
			String picturePath = cursor.getString(columnIndex);
			cursor.close();

			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			
			switch(requestCode){
			case SELECT_PORTRAIT:
				doCrop();
				break;
			
			case SELECT_LOGO:
				ImageButton imageButton2 = (ImageButton) findViewById(R.id.design_CompanyLogo);
				imageButton2.setImageBitmap(decodeSampledBitmapFromFile(picturePath, 96, 96));
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
			this.finish();
			return true;
		case R.id.design_save:
			ParseQuery<ParseObject> query = ParseQuery.getQuery("ECardInfo");
			query.fromLocalDatastore();
			query.getInBackground(currentUser.get("ecardId").toString(), new GetCallback<ParseObject>() {

				@Override
				public void done(ParseObject object, ParseException e) {
					if (e == null) {
						if (object != null) {
							ArrayList<String> remainedList = new ArrayList<String>();
							int numBtns = gridView.getChildCount() - 1;
							for (int i = 0; i < numBtns; i++) {
								View view = gridView.getChildAt(i);
								// Log.d("buttons:" , ((MyTag) view.getTag()).getKey() +
								// "   "+ ((MyTag) view.getTag()).getValue());
								object.put(((MyTag) view.getTag()).getKey(), ((MyTag) view.getTag()).getValue());
								remainedList.add(((MyTag) view.getTag()).getKey());
							}
							allowedArrayList.removeAll(remainedList);
							for (Iterator<String> iter = allowedArrayList.iterator(); iter.hasNext();) {
								String nullItem = iter.next();
								object.remove(nullItem);
							}
							object.saveEventually();
							object.pinInBackground();
							Toast.makeText(getBaseContext(), "Save successful", Toast.LENGTH_SHORT).show();
						}
					}
				}

			});

			this.finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	OnItemClickListener gridViewItemClickListenerBuilder(){
		OnItemClickListener listener = new OnItemClickListener() {

			@SuppressLint("NewApi")
			@Override
			public void onItemClick(AdapterView<?> parent, final View view, final int position, long id) {
				if (position == infoLink.size() - 1) {
					// This is the last item in the gridView, representing "addmore" button 
					// Upon click, opens dialogAddMore
					// actions defined in buildAddMoreButtonDialog();
					actions.show();
				} else {
					String item = shownArrayList.get(position);
					int loc = allowedArrayList.indexOf(item);
					
					// setup dialog Views, which is the actual layout
					
					final View dialogPerItemView = setupDialogView(allowedDisplayArray[loc].toString(), ((MyTag) view.getTag()).getValue().toString());
					
		    	    // display the actual dialog
					new AlertDialog.Builder(ActivityDesign.this)
							.setView(dialogPerItemView)
							.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {
									EditText dialogText = (EditText) dialogPerItemView.findViewById(R.id.dialog_text);
									Editable value = dialogText.getText();
									// update the tag of the view with updated values
									view.setTag(new MyTag(((MyTag) view.getTag()).getKey().toString(), value.toString()));
									// update the link contents
									infoLink.remove(position);
									infoLink.add(position, value.toString());
								}
							}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {
									// Do nothing.
								}
							}).setNeutralButton("Delete", new DialogInterface.OnClickListener() {
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
									selectionArray = (String[]) selectionTreeSet.toArray(new String[0]);
									selectionDisplayArray = (String[]) selectionDisplayTreeSet.toArray(new String[0]);

									buildAddMoreButtonDialog();													

									// make a new adapter with one less item
									MyGridViewAdapter updatedAdapter = new MyGridViewAdapter(getBaseContext(), shownArrayList, infoLink,
											infoIcon);
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

	OnItemClickListener dialogAddMoreListItemClickListenerBuilder(){
		OnItemClickListener listener = new OnItemClickListener(){
			// This is the Listener that listens to the item selection in dialogAddMore
			// Upon selection, pop up a new dialog to edit initial info of the to be added item
			
			@SuppressLint("NewApi")
			@Override
			public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
				// Upon dialogAddMore item selection, it can be closed
				actions.dismiss();
				
				// setup dialog Views, which is the actual layout				
				final View dialogPerItemView = setupDialogView(selectionDisplayArray[position].toString(), "");
	    	    
	    	    // Build the actual peritem dialog
				new AlertDialog.Builder(ActivityDesign.this)
					.setView(dialogPerItemView)
					.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
						@SuppressLint("NewApi")
						public void onClick(DialogInterface dialog, int whichButton) {
							EditText dialogText = (EditText) dialogPerItemView.findViewById(R.id.dialog_text);
							Editable value = dialogText.getText();
							infoLink.add(infoLink.size() - 1, value.toString());
							// The reason array instead of TreeSet must be used: TreeSet has no get()
							infoIcon.add(infoIcon.size() - 1, iconSelector(selectionArray[position]));
							// add to exiting list
							shownArrayList.add(shownArrayList.size() - 1, selectionArray[position]);

							List<String> list = new ArrayList<String>(Arrays.asList(selectionArray));
							selectionTreeSet.remove(list.get(position));
							list.remove(position);
							selectionArray = list.toArray(new String[0]);
							list = new ArrayList<String>(Arrays.asList(selectionDisplayArray));
							selectionDisplayTreeSet.remove(list.get(position));
							list.remove(position);
							selectionDisplayArray = list.toArray(new String[0]);
							
							// When this new item is added, the available items list is changed, 
							// this should be reflected in the change of dialogAddMore
							buildAddMoreButtonDialog();

							// make a new adapter with one more item included
							MyGridViewAdapter updatedAdapter = new MyGridViewAdapter(getBaseContext(), shownArrayList, infoLink, infoIcon);
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
	    View dialogView = inflater.inflate(R.layout.layout_dialog_design_peritem, null);
	    LinearLayout dialogHeader = (LinearLayout) dialogView.findViewById(R.id.dialog_header);
	    final EditText dialogText = (EditText) dialogView.findViewById(R.id.dialog_text);
	    TextView dialogTitle = (TextView) dialogView.findViewById(R.id.dialog_title);
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
		ParseQuery<ParseObject> query = ParseQuery.getQuery("ECardInfo");
		query.fromLocalDatastore();
		query.getInBackground(currentUser.get("ecardId").toString(), new GetCallback<ParseObject>() {

			@Override
			public void done(ParseObject object, ParseException e) {
				if (e == null) {
					if (object != null) {
						// ParseFile portraitFile = (ParseFile) object.get("portrait");
						// if (portraitFile != null) {
						// portraitFile.getDataInBackground(new GetDataCallback() {
						//
						// @Override
						// public void done(byte[] data, ParseException e) {
						// if (e == null) {
						// Bitmap bmp = BitmapFactory.decodeByteArray(data, 0,
						// data.length);
						//
						// ImageView pic = (ImageView) findViewById(R.id.Portrait);
						// pic.setImageBitmap(bmp);
						//
						// } else {
						// Toast.makeText(getActivity(), "Error displaying portrait!",
						// Toast.LENGTH_SHORT).show();
						// }
						// }
						// });
						// } else {
						// Toast.makeText(getActivity(), "Portrait empty!",
						// Toast.LENGTH_SHORT).show();
						// }
						//
						// ParseFile qrCodeFile = (ParseFile) object.get("qrCode");
						// if (qrCodeFile != null) {
						// qrCodeFile.getDataInBackground(new GetDataCallback() {
						//
						// @Override
						// public void done(byte[] data, ParseException e) {
						// if (e == null) {
						// Bitmap bmp = BitmapFactory.decodeByteArray(data, 0,
						// data.length);
						//
						// QRImg = new ImageView(getActivity());
						// QRImg.setId(QR_IMG_ID);
						// QRImg.setImageBitmap(bmp);
						// } else {
						// Toast.makeText(getActivity(), "Error displaying QR Code!",
						// Toast.LENGTH_SHORT).show();
						// }
						// }
						// });
						// } else {
						// Toast.makeText(getActivity(), "QR Code empty!",
						// Toast.LENGTH_SHORT).show();
						// }

						TextView name = (TextView) findViewById(R.id.design_first_name);
						String tmpString = object.getString("firstName");
						if (tmpString != null)
							name.setText(tmpString);
						name = (TextView) findViewById(R.id.design_last_name);
						tmpString = object.getString("lastName");
						if (tmpString != null)
							name.setText(tmpString);
						name = (TextView) findViewById(R.id.design_company);
						tmpString = object.getString("company");
						if (tmpString != null)
							name.setText(tmpString);
						name = (TextView) findViewById(R.id.design_job_title);
						tmpString = object.getString("title");
						if (tmpString != null)
							name.setText(tmpString);
					} else {
						Toast.makeText(getBaseContext(), "Self Ecardinfo not found locally!", Toast.LENGTH_SHORT).show();
					}
				} else {
					Toast.makeText(getBaseContext(), "Error getting data to display card", Toast.LENGTH_SHORT).show();
				}
			}

		});
	}

	@SuppressWarnings("deprecation")
	public static Bitmap decodeSampledBitmapFromFile(String picturePath, int reqWidth, int reqHeight) {

		// First decode with inJustDecodeBounds=true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(picturePath, options);

		// Calculate inSampleSize
	    int photoW = options.outWidth;
	    int photoH = options.outHeight;
	    
	    int scaleFactor = Math.min(photoW/reqWidth, photoH/reqHeight);

	    options.inJustDecodeBounds = false;
	    options.inSampleSize = scaleFactor;
	    options.inPurgeable = true;
		options.inJustDecodeBounds = false;
		return BitmapFactory.decodeFile(picturePath, options);
	}

private void displaySourceDialog() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    String source[] = { "Gallary", "Camera" };

    builder.setTitle("Select Image From:")
            .setSingleChoiceItems(source, currentSource,
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog,
                                int which) {
                            currentSource = which;
                            
                            switch (currentSource){
                            case 0:
                				Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                				photoPickerIntent.setType("image/*");
                				startActivityForResult(photoPickerIntent, SELECT_PORTRAIT);
                				
                				break;
                				
                            case 1:
                            	Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                            	//selectedImage = Uri.fromFile(new File(Environment.getExternalStorageDirectory(),"tmp_avatar_" + String.valueOf(System.currentTimeMillis()) + ".jpg"));
                            	//takePicture.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, selectedImage);
                            	//takePicture.putExtra("Uri", selectedImage);
                            	//takePicture.putExtra("return-data", true);
                            	//startActivityForResult(takePicture, TAKE_IMAGE);
                            	// if (takePicture.resolveActivity(getPackageManager()) != null) {
                            	//        startActivityForResult(takePicture, TAKE_IMAGE);
                            	//    }
                            	 
                            	 if (takePicture.resolveActivity(getPackageManager()) != null) {
                            	        // Create the File where the photo should go
                            	        File photoFile = null;
                            	        try {
                            	            photoFile = createImageFile();
                            	        } catch (IOException ex) {
                            	            // Error occurred while creating the File
                            	            //...
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
String mCurrentPhotoPath;
private File createImageFile() throws IOException {
    // Create an image file name
    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
    String imageFileName = "JPEG_" + timeStamp + "_";
    File storageDir = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES);
    File image = File.createTempFile(
        imageFileName,  /* prefix */
        ".jpg",         /* suffix */
        storageDir      /* directory */
    );

    // Save a file: path for use with ACTION_VIEW intents
    mCurrentPhotoPath = "file:" + image.getAbsolutePath();
    return image;
}

private void doCrop() {
	final ArrayList<CropOption> cropOptions = new ArrayList<CropOption>();
	
	Intent intent = new Intent("com.android.camera.action.CROP");
    intent.setType("image/*");
    
    List<ResolveInfo> list = getPackageManager().queryIntentActivities( intent, 0 );
    
    int size = list.size();
    
    if (size == 0) {	        
    	Toast.makeText(this, "Can not find image crop app", Toast.LENGTH_SHORT).show();
    	
        return;
    } else {
    	intent.setData(selectedImage);
        
        intent.putExtra("outputX", 200);
        intent.putExtra("outputY", 200);
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("scale", true);
        intent.putExtra("return-data", true);
        
    	if (size == 1) {
    		Intent i 		= new Intent(intent);
        	ResolveInfo res	= list.get(0);
        	
        	i.setComponent( new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
        	
        	startActivityForResult(i, CROP_FROM_CAMERA);
    	} else {
	        for (ResolveInfo res : list) {
	        	final CropOption co = new CropOption();
	        	
	        	co.title 	= getPackageManager().getApplicationLabel(res.activityInfo.applicationInfo);
	        	co.icon		= getPackageManager().getApplicationIcon(res.activityInfo.applicationInfo);
	        	co.appIntent= new Intent(intent);
	        	
	        	co.appIntent.setComponent( new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
	        	
	            cropOptions.add(co);
	        }
        
	        CropOptionAdapter adapter = new CropOptionAdapter(getApplicationContext(), cropOptions);
	        
	        AlertDialog.Builder builder = new AlertDialog.Builder(this);
	        builder.setTitle("Choose Crop App");
	        builder.setAdapter( adapter, new DialogInterface.OnClickListener() {
	            public void onClick( DialogInterface dialog, int item ) {
	                startActivityForResult( cropOptions.get(item).appIntent, CROP_FROM_CAMERA);
	            }
	        });
        
	        builder.setOnCancelListener( new DialogInterface.OnCancelListener() {
	            @Override
	            public void onCancel( DialogInterface dialog ) {
	               
	                if (selectedImage != null ) {
	                    getContentResolver().delete(selectedImage, null, null );
	                    selectedImage = null;
	                }
	            }
	        } );
	        
	        AlertDialog alert = builder.create();
	        
	        alert.show();
    	}
    }
}
	
	@SuppressLint("NewApi")
	private void buildAddMoreButtonDialog() {
		// Get the layout inflater
	    LayoutInflater inflater = getLayoutInflater();
	    View dialogAddMoreView = inflater.inflate(R.layout.layout_dialog_addmore, null);
	    LinearLayout dialogHeader = (LinearLayout) dialogAddMoreView.findViewById(R.id.dialog_header);
	    TextView dialogTitle = (TextView) dialogAddMoreView.findViewById(R.id.dialog_title);
	    // Set dialog header background with rounded corner
	    Bitmap bm = BitmapFactory
	    	      .decodeResource(getResources(), R.drawable.striped);
	    BitmapDrawable bmDrawable = new BitmapDrawable(getResources(), bm);
	    dialogHeader.setBackground(new CurvedAndTiled(bmDrawable.getBitmap(), 5));
	    // Set dialog title and main EditText
	    dialogTitle.setText("Add Info");
	    	    
	    AlertDialog.Builder builder = new AlertDialog.Builder(ActivityDesign.this);
		builder.setView(dialogAddMoreView);
		builder.setNegativeButton("Cancel", null);
		// actions now links to the dialog
		actions = builder.create();
		
		// Below is to build the listener for items listed inside the poped up "addmorebutton dialog"
		ListView listViewInDialog = (ListView)dialogAddMoreView.findViewById(R.id.dialog_listview);
	    listViewInDialog.setAdapter(new MySimpleListViewAdapter(ActivityDesign.this, selectionDisplayArray));
		listViewInDialog.setOnItemClickListener(dialogAddMoreListItemClickListenerBuilder());
	}
}
