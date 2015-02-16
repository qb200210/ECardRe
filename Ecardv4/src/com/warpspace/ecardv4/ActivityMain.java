package com.warpspace.ecardv4;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.zxing.client.android.Intents;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParsePush;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.viewpagerindicator.CirclePageIndicator;
import com.viewpagerindicator.PageIndicator;
import com.warpspace.ecardv4.R;
import com.warpspace.ecardv4.infrastructure.UserInfo;
import com.warpspace.ecardv4.utils.CurvedAndTiled;
import com.warpspace.ecardv4.utils.CustomQRScanner;
import com.warpspace.ecardv4.utils.ECardUtils;
import com.warpspace.ecardv4.utils.MyPagerAdapter;
import com.warpspace.ecardv4.utils.MyViewPager;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

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
  UserInfo myselfUserInfo= null;
  boolean imgFromTmpData = false;

  @SuppressLint("NewApi")
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    showActionBar();
    setContentView(R.layout.activity_main);
    
    Bundle b = getIntent().getExtras();
    if(b!=null){
    	if(b.get("imgFromTmpData") != null){
    		imgFromTmpData = (boolean) b.get("imgFromTmpData");
    	}
    }

    LinearLayout ll_add = (LinearLayout) findViewById(R.id.ll_add);
    LinearLayout ll_search = (LinearLayout) findViewById(R.id.ll_search);
    Bitmap bm = BitmapFactory
      .decodeResource(getResources(), R.drawable.striped);
    BitmapDrawable bmDrawable = new BitmapDrawable(getResources(), bm);
    ll_add.setBackground(new CurvedAndTiled(bmDrawable.getBitmap(), 25));
    ll_search.setBackground(new CurvedAndTiled(bmDrawable.getBitmap(), 25));

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
    myselfUserInfo = new UserInfo(this, currentUser.get("ecardId").toString(), "", "", true, false, imgFromTmpData);
    
    mAdapter = new MyPagerAdapter(getSupportFragmentManager(), myselfUserInfo);

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
      intent.putExtra("userinfo", myselfUserInfo);
	  startActivityForResult(intent, EDIT_CARD);
      return true;
    case R.id.log_out:
      ParseUser.logOut();
      intent = new Intent(this, ActivityPreLogin.class);
      startActivity(intent);
      this.finish();
      return true;    
    case R.id.test_notif:
      sendPush();
      return true;   
    default:
      return super.onOptionsItemSelected(item);
    }
  }
  
  @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(requestCode == EDIT_CARD && resultCode == RESULT_OK ){
			Bundle extras = data.getExtras();
			UserInfo updatedUserInfo = (UserInfo) extras.getParcelable("userinfo");
			// Update the UserInfo for refreshing fragments
			((MyPagerAdapter) mPager.getAdapter()).setMyselfUserInfo(updatedUserInfo);	
			// Refreshing fragments
			mPager.getAdapter().notifyDataSetChanged();
			// Update the UserInfo being holded in ActivityMain
			myselfUserInfo = updatedUserInfo;
		}		
	  
  }
  
  private void showActionBar() {
		LayoutInflater inflator = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = inflator.inflate(R.layout.layout_actionbar_main, null);
		ImageView btnNotif = (ImageView) v.findViewById(R.id.btn_notifications);
		btnNotif.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(getApplicationContext(), ActivityConversations.class);
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
  
  
  
  public void sendPush(){
		// Send push to the other party according to their ecardId recorded in an installation
		ParseQuery pushQuery = ParseInstallation.getQuery();
		pushQuery.whereEqualTo("ecardId", "CRuumzPcTN");
		JSONObject jsonObject = new JSONObject();
		try {
			jsonObject.put("alert", "Hi, I'm " + currentUser.get("ecardId").toString() + ", save my card now");
			jsonObject.put("link", "https://ecard.parseapp.com/search?id="+currentUser.get("ecardId").toString()+"&fn=Udayan&ln=Banerji");
			jsonObject.put("action", "EcardOpenConversations");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		
		ParsePush push = new ParsePush();
		push.setQuery(pushQuery);
		push.setData(jsonObject);
		push.sendInBackground();
		
//		// Meanwhile, create a record in conversations -- so web app can check since it cannot receive notification
//		ParseObject object = new ParseObject("Conversations");
//		object.put("partyA", currentUser.get("ecardId").toString());
//		object.put("partyB", objId.getText().toString());
//		object.put("read", false);
//		object.saveInBackground();
	}
}
