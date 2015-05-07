package com.micklestudios.knowell;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.PopupMenu;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
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
import com.micklestudios.knowell.utils.MyTag;
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
  ParseUser currentUser;
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
    
    LinearLayout ll_add = (LinearLayout) findViewById(R.id.ll_add);
    LinearLayout ll_search = (LinearLayout) findViewById(R.id.ll_search);
    Bitmap bm = BitmapFactory
      .decodeResource(getResources(), R.drawable.striped);
    BitmapDrawable bmDrawable = new BitmapDrawable(getResources(), bm);
    Resources r = getResources();
    float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, r.getDisplayMetrics());
    ll_add.setBackgroundDrawable(new CurvedAndTiled(bmDrawable.getBitmap(), px));
    ll_search.setBackgroundDrawable(new CurvedAndTiled(bmDrawable.getBitmap(), px));

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
    case R.id.share_item:
    	showPopup();
    	
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
  
  public void showPopup(){
	  	
	    View menuItemView = findViewById(R.id.share_item);
	    PopupMenu popup = new PopupMenu(ActivityMain.this, menuItemView);
	    MenuInflater inflate = popup.getMenuInflater();
	    inflate.inflate(R.menu.main_popup_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
        	@Override
            public boolean onMenuItemClick(MenuItem item) {
        		Intent intent;
        		switch (item.getItemId()) {
        		
        		case R.id.share_qr:
        			break;
        		case R.id.share_email:
        			intent = new Intent(Intent.ACTION_SEND);
        			intent.setType("text/html");
        			intent.putExtra(Intent.EXTRA_SUBJECT, "Please connect with me through KnoWell");
        			intent.putExtra(Intent.EXTRA_TEXT, "Hi, I'd like to connect with you through KnoWell.");

        			startActivity(Intent.createChooser(intent, "Send Email"));
					break;
        		case R.id.share_message:
                    intent = new Intent( Intent.ACTION_VIEW, Uri.parse( "sms:" + "" ) );
                    intent.putExtra( "Please connect with me through KnoWell", "" );
					startActivity(intent);
					break;
        		case R.id.search_other_users:
        			break;
        		}
        		Toast.makeText(ActivityMain.this,
                        "Clicked popup menu item " + item.getTitle(),
                        Toast.LENGTH_SHORT).show();
                return true;
            }
        });
	    popup.show();

	}

}
