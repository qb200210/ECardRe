package com.warpspace.ecardv4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.warpspace.ecardv4.R;
import com.warpspace.ecardv4.utils.ExpandableHeightGridView;
import com.warpspace.ecardv4.utils.MyGridViewAdapter;
import com.warpspace.ecardv4.utils.MyTag;
import com.warpspace.ecardv4.utils.MyScrollView;
import com.warpspace.ecardv4.utils.SquareLayout;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class ActivityDesign extends ActionBarActivity {

  ParseUser currentUser;
  private MyScrollView scrollView;
  // dummy array, will be replaced by extra info items
  private ArrayList<Integer> infoIcon = new ArrayList<Integer>();
  private ArrayList<String> infoLink = new ArrayList<String>();
  ArrayList<String> shownArrayList = new ArrayList<String>();
  String[] allowedArray = { "facebook", "linkedin", "twitter", "googleplus",
    "phone", "about", "email" };
  String[] allowedDisplayArray = { "Facebook", "LinkedIn", "Twitter",
    "Google +", "Phone", "About", "Email" };
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

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_design);

    scrollView = (MyScrollView) findViewById(R.id.scroll_view2);
    scrollView.setmScrollable(true);

    currentUser = ParseUser.getCurrentUser();
    displayMyCard();

    // complete list of possible extrainfo items
    infoIcon.clear();
    infoLink.clear();

    ParseQuery<ParseObject> query = ParseQuery.getQuery("ECardInfo");
    query.fromLocalDatastore();
    query.getInBackground(currentUser.get("EcardID").toString(),
      new GetCallback<ParseObject>() {

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
              selectionArray = (String[]) selectionTreeSet
                .toArray(new String[0]);
              selectionDisplayArray = (String[]) selectionDisplayTreeSet
                .toArray(new String[0]);

              // Build the dialog for selection of available info items
              AlertDialog.Builder builder = new AlertDialog.Builder(
                ActivityDesign.this);
              builder.setTitle("Add Info");
              // Set list for selection, and set up the listener -- the listener
              // is to
              // monitor choice inside the dialog
              builder.setItems(selectionDisplayArray, listenerBuilder());
              builder.setNegativeButton("Cancel", null);
              // actions now links to the dialog
              actions = builder.create();

              // The reason to use expandableHeightGridView is to remove the
              // scroll bar
              // that comes with GridView
              // This is because this gridView is part of the welcome page that
              // is already
              // scrollable
              gridView = (ExpandableHeightGridView) findViewById(R.id.gridView1);
              gridView.setAdapter(new MyGridViewAdapter(getBaseContext(),
                shownArrayList, infoLink, infoIcon));
              gridView.setOnItemClickListener(new OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, final View view,
                  final int position, long id) {
                  if (position == infoLink.size() - 1) {
                    // This is the link to open selection candidate dialog.
                    // actions is defined above, linking to the just-built
                    // dialog
                    // When the last item in the gridView is clicked, show the
                    // dialog
                    actions.show();
                  } else {
                    final EditText input = new EditText(ActivityDesign.this);
                    // display existing values for this info item by getting it
                    // from the tag of the view
                    input.setText(((MyTag) view.getTag()).getValue().toString());
                    String item = shownArrayList.get(position);
                    int loc = allowedArrayList.indexOf(item);
                    // Below is the dialog for changing button content or delete
                    // button
                    new AlertDialog.Builder(ActivityDesign.this)
                      .setTitle("Update Status")
                      .setMessage(allowedDisplayArray[loc].toString())
                      .setView(input)
                      .setPositiveButton("Ok",
                        new DialogInterface.OnClickListener() {
                          public void onClick(DialogInterface dialog,
                            int whichButton) {

                            Editable value = input.getText();
                            // update the tag of the view with updated values
                            view.setTag(new MyTag(((MyTag) view.getTag())
                              .getKey().toString(), value.toString()));
                            // update the link contents
                            infoLink.remove(position);
                            infoLink.add(position, value.toString());
                          }
                        })
                      .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                          public void onClick(DialogInterface dialog,
                            int whichButton) {
                            // Do nothing.
                          }
                        })
                      .setNeutralButton("Delete",
                        new DialogInterface.OnClickListener() {
                          public void onClick(DialogInterface dialog,
                            int whichButton) {
                            // remove the button from gridview
                            infoLink.remove(position);
                            infoIcon.remove(position);
                            String item = shownArrayList.get(position);
                            shownArrayList.remove(position);
                            // add the removed option to selection candidate
                            // list
                            selectionTreeSet.add(item);
                            int loc = allowedArrayList.indexOf(item);
                            selectionDisplayTreeSet
                              .add(allowedDisplayArray[loc]);
                            // convert ordered TreeSet into array to be used by
                            // dialogbuilder
                            selectionArray = (String[]) selectionTreeSet
                              .toArray(new String[0]);
                            selectionDisplayArray = (String[]) selectionDisplayTreeSet
                              .toArray(new String[0]);

                            // Re-Build the dialog for selection of available
                            // info items
                            AlertDialog.Builder builder = new AlertDialog.Builder(
                              ActivityDesign.this);
                            builder.setTitle("Add Info");
                            builder.setItems(selectionDisplayArray,
                              listenerBuilder());
                            builder.setNegativeButton("Cancel", null);
                            actions = builder.create();

                            // make a new adapter with one less item
                            MyGridViewAdapter updatedAdapter = new MyGridViewAdapter(
                              getBaseContext(), shownArrayList, infoLink,
                              infoIcon);
                            gridView.setAdapter(updatedAdapter);
                            // Refresh the gridView to display the new item
                            updatedAdapter.notifyDataSetChanged();
                          }
                        }).show();
                  }
                }

              });
            }
          }
        }
      });

    // This is the life-saver! It fixes the bug that scrollView will go to the
    // bottom of GridView upon open
    // below is to re-scroll to the first view in the LinearLayout
    SquareLayout mainCardContainer = (SquareLayout) findViewById(R.id.main_card_container);
    scrollView.requestChildFocus(mainCardContainer, null);
  }

  private Integer iconSelector(String key) {
    // input key to select corresponding icon to display on button
    switch (key) {
    case "email":
      return R.drawable.email;
    case "facebook":
      return R.drawable.facebook;
    case "linkedin":
      return R.drawable.linkedin;
    case "twitter":
      return R.drawable.twitter;
    case "phone":
      return R.drawable.ic_action_discard;
    case "about":
      return R.drawable.ic_action_discard;
    case "googleplus":
      return R.drawable.googleplus;
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
      query.getInBackground(currentUser.get("EcardID").toString(),
        new GetCallback<ParseObject>() {

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
                object.pinInBackground();
                Toast.makeText(getBaseContext(), "Save successful",
                  Toast.LENGTH_SHORT).show();
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

  DialogInterface.OnClickListener listenerBuilder() {
    // This is the listener wrapper

    DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
      // This is the listener for dialog
      // Basically, it listens for selection event from the poped up dialog

      @Override
      public void onClick(DialogInterface dialog, int which) {
        infoLink.add(infoLink.size() - 1, "http://www." + "xxxxxx" + ".com");
        // The reason array instead of TreeSet must be used: TreeSet has no
        // get()
        infoIcon.add(infoIcon.size() - 1, iconSelector(selectionArray[which]));
        // add to exiting list
        shownArrayList.add(shownArrayList.size() - 1, selectionArray[which]);

        List<String> list = new ArrayList<String>(Arrays.asList(selectionArray));
        selectionTreeSet.remove(list.get(which));
        list.remove(which);
        selectionArray = list.toArray(new String[0]);
        list = new ArrayList<String>(Arrays.asList(selectionDisplayArray));
        selectionDisplayTreeSet.remove(list.get(which));
        list.remove(which);
        selectionDisplayArray = list.toArray(new String[0]);

        for (int i = 0; i < selectionArray.length; i++) {
          System.out.println("selectionArray + " + selectionArray.length
            + selectionArray[i]);
        }

        for (int i = 0; i < selectionDisplayArray.length; i++) {
          System.out.println("selectionDisplayArray + "
            + selectionDisplayArray.length + selectionDisplayArray[i]);
        }

        // Re-Build the dialog for selection of available info items
        AlertDialog.Builder builder = new AlertDialog.Builder(
          ActivityDesign.this);
        builder.setTitle("Add Info");
        builder.setItems(selectionDisplayArray, listenerBuilder());
        builder.setNegativeButton("Cancel", null);
        actions = builder.create();

        // make a new adapter with one more item included
        MyGridViewAdapter updatedAdapter = new MyGridViewAdapter(
          getBaseContext(), shownArrayList, infoLink, infoIcon);
        gridView.setAdapter(updatedAdapter);
        // Refresh the gridView to display the new item
        updatedAdapter.notifyDataSetChanged();
      }
    };
    return listener;
  }

  public void displayMyCard() {
    ParseQuery<ParseObject> query = ParseQuery.getQuery("ECardInfo");
    query.fromLocalDatastore();
    query.getInBackground(currentUser.get("EcardID").toString(),
      new GetCallback<ParseObject>() {

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
              Toast.makeText(getBaseContext(),
                "Self Ecardinfo not found locally!", Toast.LENGTH_SHORT).show();
            }
          } else {
            Toast.makeText(getBaseContext(),
              "Error getting data to display card", Toast.LENGTH_SHORT).show();
          }
        }

      });
  }
}
