package com.warpspace.ecardv4;

import java.util.ArrayList;

import com.warpspace.ecardv4.R;
import com.warpspace.ecardv4.utils.MyScrollView;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

public class WelcomeMainFragment extends Fragment {

  private static final String ARG_SECTION_NUMBER = "section_number";
  private static final int INITIAL_DELAY_MILLIS = 300;
  private ViewGroup mContainerView;
  // use a boolean to keep track of whether QR code drawer is open
  private boolean qrOnDisplay = false;
  private MyScrollView scrollView;
  private boolean flagReady = true;

  // dummy array, will be replaced by extra info items
  static final ArrayList<String> extraInfoList = new ArrayList<String>();
  static final ArrayList<String> extraInfoLinkList = new ArrayList<String>();

  public static WelcomeMainFragment newInstance(int sectionNumber) {
    WelcomeMainFragment fragment = new WelcomeMainFragment();
    Bundle args = new Bundle();
    args.putInt(ARG_SECTION_NUMBER, sectionNumber);
    fragment.setArguments(args);
    return fragment;
  }

  public WelcomeMainFragment() {

  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    // by default, when app opens, display actionbar with
    // welcome-page-association buttons
    inflater.inflate(R.menu.main_actionbar, menu);
    super.onCreateOptionsMenu(menu, inflater);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // this function is called when either action bar icon is tapped
    switch (item.getItemId()) {
    case R.id.edit_item:
      // Should be replaced by pop up activity of editable welcome page
      Toast.makeText(getActivity(), "Edit item!", Toast.LENGTH_SHORT).show();
      return true;
    case R.id.log_out:
      ParseUser.logOut();
      Intent intent = new Intent(getActivity(), ActivityPreLogin.class);
      startActivity(intent);
      getActivity().finish();
      return true;

    default:
      return super.onOptionsItemSelected(item);
    }
  }

  private void hideQR() {
    // Since QR code will always be the first item in the ViewGroup, its index
    // is 0
    mContainerView.removeViewAt(0);
    qrOnDisplay = false;
  }

  private void displayQR() {
    // Instantiate a new "row" view.
    final ViewGroup newView = (ViewGroup) LayoutInflater.from(getActivity())
      .inflate(R.layout.qr_container, mContainerView, false);

    // Because mContainerView has android:animateLayoutChanges set to true,
    // adding this view is automatically animated. Adding at index 0

    mContainerView.addView(newView, 0);
    qrOnDisplay = true;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
    Bundle savedInstanceState) {
    View rootView = inflater.inflate(R.layout.fragment_welcome_main, container,
      false);
    mContainerView = (ViewGroup) rootView.findViewById(R.id.welcome_container);
    scrollView = (MyScrollView) rootView.findViewById(R.id.scroll_view);
    scrollView.setmScrollable(true);

    ParseUser currentUser = ParseUser.getCurrentUser();
    displayMyCard(currentUser, rootView);

    // dummy array, will be replaced by extra info items
    extraInfoList.clear();
    extraInfoList.add("Faacebook");
    extraInfoList.add("LinkedIn");
    extraInfoList.add("Homepage");
    extraInfoList.add("Company Website");
    extraInfoList.add("Instagram");

    extraInfoLinkList.clear();
    extraInfoLinkList.add("http://www.facebook.com");
    extraInfoLinkList.add("http://www.LinkedIn.com");
    extraInfoLinkList.add("http://www.Homepage.com");
    extraInfoLinkList.add("http://www.Company.com");
    extraInfoLinkList.add("http://www.Instagram.com");

    // LinearLayout mExtraInfoContainerView = (LinearLayout)
    // rootView.findViewById(R.id.extra_info_container);
    // for (int i = 0; i < extraInfoList.size(); i++){
    // View mListView = inflater.inflate(R.layout.list_row_extra_info_design,
    // null);
    // TextView text = (TextView)
    // mListView.findViewById(R.id.list_row_draganddrop_textview);
    // text.setText(extraInfoList.get(i));
    // mListView.setTag(extraInfoLinkList.get(i));
    // mListView.setOnClickListener(new View.OnClickListener() {
    // @Override
    // public void onClick(View v) {
    // String tag = (String) v.getTag();
    // if(tag != null){
    // Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(tag));
    // startActivity(browserIntent);
    // }
    // }
    // });
    // mExtraInfoContainerView.addView(mListView);
    // }

    // detect the swipe gesture so that
    // when QR is hidden, can swipe scrollview; when it's up, scrollView is
    // locked and swipe calls hideQR()
    final GestureDetector gesture = new GestureDetector(getActivity(),
      new GestureDetector.SimpleOnGestureListener() {

        @Override
        public boolean onDown(MotionEvent e) {
          return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
          float velocityY) {
          if (velocityY < 0) {
            // if swipe up, always not ready to display QR
            flagReady = false;
          }
          if (qrOnDisplay) {
            if (velocityY < 0) {
              // if QR is up, swipe up will hide QR and unlock scrollView
              hideQR();
              scrollView.setmScrollable(true);
            }
          } else {
            if (velocityY > 0) {
              if (scrollView.ismTopReached()) {
                // default ismTopReached = true; flagReady = true
                // If swipe hits top, then ready to display QR in next down
                // swipe
                flagReady = true;
              }
              if (flagReady) {
                // If ready, display QR and lock scrollView
                displayQR();
                scrollView.setmScrollable(false);
              }
            }
          }

          return super.onFling(e1, e2, velocityX, velocityY);
        }
      });
    rootView.setOnTouchListener(new OnTouchListener() {

      @Override
      public boolean onTouch(View v, MotionEvent event) {
        return gesture.onTouchEvent(event);
      }

    });

    // This option needs to be set for action bar item to function
    setHasOptionsMenu(true);

    return rootView;

  }

  public void displayMyCard(ParseUser currentUser, final View rootView) {
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

              TextView name = (TextView) rootView
                .findViewById(R.id.design_first_name);
              String tmpString = object.getString("firstName");
              if (tmpString != null)
                name.setText(tmpString);
              name = (TextView) rootView.findViewById(R.id.design_last_name);
              tmpString = object.getString("lastName");
              if (tmpString != null)
                name.setText(tmpString);
              name = (TextView) rootView.findViewById(R.id.design_company);
              tmpString = object.getString("company");
              if (tmpString != null)
                name.setText(tmpString);
              name = (TextView) rootView.findViewById(R.id.design_job_title);
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
              Toast.makeText(getActivity(),
                "Self Ecardinfo not found locally!", Toast.LENGTH_SHORT).show();
            }
          } else {
            Toast.makeText(getActivity(), "Error getting data to display card",
              Toast.LENGTH_SHORT).show();
          }
        }

      });
  }

}
