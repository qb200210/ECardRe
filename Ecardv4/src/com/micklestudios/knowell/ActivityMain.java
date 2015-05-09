package com.micklestudios.knowell;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
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
import android.widget.Toast;

import com.google.zxing.client.android.Intents;
import com.micklestudios.knowell.infrastructure.UserInfo;
import com.micklestudios.knowell.utils.CurvedAndTiled;
import com.micklestudios.knowell.utils.CustomQRScanner;
import com.micklestudios.knowell.utils.MyPagerAdapter;
import com.micklestudios.knowell.utils.MyViewPager;
import com.parse.ParseInstallation;
import com.parse.ParsePush;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.viewpagerindicator.CirclePageIndicator;
import com.viewpagerindicator.PageIndicator;
import com.micklestudios.knowell.R;

public class ActivityMain extends ActionBarActivity {

  private static final int EDIT_CARD = 0;
  /**
   * The {@link ViewPager} that will host the section contents.
   */
  MyPagerAdapter mAdapter;
  MyViewPager mPager;
  PageIndicator mIndicator;
  ActionBar mActionBar;
  Menu mMenu;
  int currentPosition = 0;
  public static ParseUser currentUser;
  // set myselfUserInfo to be global for each access across the entire app
  public static UserInfo myselfUserInfo = null;
  boolean imgFromTmpData = false;

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
    DisplayMetrics outMetrics = new DisplayMetrics ();
    display.getMetrics(outMetrics);

    float density  = getResources().getDisplayMetrics().density;
    float dpHeight = outMetrics.heightPixels / density;
    float dpWidth  = outMetrics.widthPixels / density;
    Log.i("res", "height: "+ dpHeight +"  , width: "+ dpWidth);
    
    

    currentUser = ParseUser.getCurrentUser();
    // pull myself info from localdatastore
    Log.i("imgtmp", " " + imgFromTmpData);
    myselfUserInfo = new UserInfo(currentUser.get("ecardId").toString(), "",
      "", true, false, imgFromTmpData);

    mAdapter = new MyPagerAdapter(getSupportFragmentManager());

    mPager = (MyViewPager) findViewById(R.id.pager);
    mPager.setAdapter(mAdapter);

    mIndicator = (CirclePageIndicator) findViewById(R.id.indicator);
    mIndicator.setViewPager(mPager);

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
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == EDIT_CARD && resultCode == RESULT_OK) {
      // Refreshing fragments
      mPager.getAdapter().notifyDataSetChanged();
    }

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
