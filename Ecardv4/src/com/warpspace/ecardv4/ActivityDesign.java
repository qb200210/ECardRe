package com.warpspace.ecardv4;

import java.util.ArrayList;

import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.warpspace.ecardv4.R;
import com.warpspace.ecardv4.utils.ExpandableHeightGridView;
import com.warpspace.ecardv4.utils.MyGridViewAdapter;
import com.warpspace.ecardv4.utils.MyScrollView;
import com.warpspace.ecardv4.utils.SquareLayout;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;
import android.widget.Toast;

public class ActivityDesign extends ActionBarActivity {

  private MyScrollView scrollView;
  // dummy array, will be replaced by extra info items
  static final ArrayList<Integer> infoIcon = new ArrayList<Integer>();
  static final ArrayList<String> infoLink = new ArrayList<String>();
  static final ArrayList<String> infoItemsList = new ArrayList<String>();

  ExpandableHeightGridView gridView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_design);

    scrollView = (MyScrollView) findViewById(R.id.scroll_view2);
    scrollView.setmScrollable(true);

    ParseUser currentUser = ParseUser.getCurrentUser();
    displayMyCard(currentUser);

    // dummy array, will be replaced by extrainfo items
    infoIcon.clear();
    infoIcon.add(R.drawable.facebook);
    infoIcon.add(R.drawable.linkedin);
    infoIcon.add(R.drawable.twitter);
    infoIcon.add(R.drawable.googleplus);
    infoIcon.add(R.drawable.addmore);
    infoLink.clear();
    infoLink.add("http://www.facebook.com");
    infoLink.add("http://www.LinkedIn.com");
    infoLink.add("http://www.twitter.com");
    infoLink.add("http://www.google.com");
    infoLink.add("addmore");
    infoItemsList.clear();
    infoItemsList.add("Facebook");
    infoItemsList.add("LinkedIn");
    infoItemsList.add("Twitter");
    infoItemsList.add("Google+");
    String[] infoItemsArray = (String[]) infoItemsList.toArray(new String[0]);

    // Build the dialog for selection of available info items
    AlertDialog.Builder builder = new AlertDialog.Builder(ActivityDesign.this);
    builder.setTitle("Add Info");
    // Set list for selection, and set up the listener -- the listener is to
    // monitor choice inside the dialog
    builder.setItems(infoItemsArray, listenerBuilder(infoItemsArray));
    builder.setNegativeButton("Cancel", null);
    // actions now links to the dialog
    final AlertDialog actions = builder.create();

    // The reason to use expandableHeightGridView is to remove the scroll bar
    // that comes with GridView
    // This is because this gridView is part of the welcome page that is already
    // scrollable
    gridView = (ExpandableHeightGridView) findViewById(R.id.gridView1);
    gridView.setAdapter(new MyGridViewAdapter(this, infoLink, infoIcon));
    gridView.setOnItemClickListener(new OnItemClickListener() {

      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position,
        long id) {
        if (position == infoLink.size() - 1) {
          // This is the link to open dialog. actions is defined above, linking
          // to the just-built dialog
          // When the last item in the gridView is clicked, show the dialog
          actions.show();
        } else {
        	String tag = (String) view.getTag();
            if(tag != null){
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(tag));
                startActivity(browserIntent);
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
			Toast.makeText(this, "Save changes!", Toast.LENGTH_SHORT).show();
			this.finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

  DialogInterface.OnClickListener listenerBuilder(final String[] records) {
    // This is the listener wrapper

    DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
      // This is the listener for dialog
      // Basically, it listens for selection event from the poped up dialog

      @Override
      public void onClick(DialogInterface dialog, int which) {
        infoLink.add(infoLink.size() - 1, "http://www." + "xxxxxx" + ".com");
        infoIcon.add(infoIcon.size() - 1, infoIcon.get(which));
        // make a new adapter with one more item included
        MyGridViewAdapter updatedAdapter = new MyGridViewAdapter(
          getBaseContext(), infoLink, infoIcon);
        gridView.setAdapter(updatedAdapter);
        // Refresh the gridView to display the new item
        updatedAdapter.notifyDataSetChanged();
      }
    };
    return listener;
  }

  public void displayMyCard(ParseUser currentUser) {
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

              TextView name = (TextView) findViewById(R.id.my_first_name);
              String tmpString = object.getString("firstName");
              if (tmpString != null)
                name.setText(tmpString);
              name = (TextView) findViewById(R.id.my_last_name);
              tmpString = object.getString("lastName");
              if (tmpString != null)
                name.setText(tmpString);
              name = (TextView) findViewById(R.id.my_company);
              tmpString = object.getString("company");
              if (tmpString != null)
                name.setText(tmpString);
              name = (TextView) findViewById(R.id.my_job_title);
              tmpString = object.getString("title");
              if (tmpString != null)
                name.setText(tmpString);
              // name = (TextView) findViewById(R.id.tel);
              // name.setText(object.getString("tel"));
              // name = (TextView) findViewById(R.id.email);
              // name.setText(object.getString("email"));
              // name = (TextView) findViewById(R.id.link);
              // name.setText(object.getString("link"));
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
