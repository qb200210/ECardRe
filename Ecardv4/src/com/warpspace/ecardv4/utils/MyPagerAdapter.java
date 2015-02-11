/**
 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */

package com.warpspace.ecardv4.utils;

import java.util.Locale;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.util.Log;
import android.widget.Toast;

import com.viewpagerindicator.IconPagerAdapter;
import com.warpspace.ecardv4.FragmentMaincard;
import com.warpspace.ecardv4.infrastructure.UserInfo;

public class MyPagerAdapter extends FragmentPagerAdapter implements
    IconPagerAdapter {


private UserInfo myselfUserInfo;



public UserInfo getMyselfUserInfo() {
	return myselfUserInfo;
}

public void setMyselfUserInfo(UserInfo myselfUserInfo) {
	this.myselfUserInfo = myselfUserInfo;
	  Log.i("setMyselfUserInfo", myselfUserInfo.getFirstName());
}

public MyPagerAdapter(FragmentManager fm, UserInfo myselfUserInfo) {
    super(fm);
	this.myselfUserInfo = myselfUserInfo;
  }

  @Override
  public Fragment getItem(int position) {
    // getItem is called to instantiate the fragment for the given page.
    // Return a PlaceholderFragment (defined as a static inner class
    // below).
	  Log.i("getItem", myselfUserInfo.getFirstName());
    switch (position) {
    case 0:
      return FragmentMaincard.newInstance(1, myselfUserInfo);
    case 1:
      return FragmentMaincard.newInstance(2, myselfUserInfo);
    default:
      return FragmentMaincard.newInstance(2, myselfUserInfo);
    }
  }

  @Override
  public int getCount() {
    // Show 2 total pages.
    return 2;
  }

  @Override
  public CharSequence getPageTitle(int position) {
    Locale l = Locale.getDefault();
    // switch (position) {
    // case 0:
    // return getString(R.string.title_section1).toUpperCase(l);
    // case 1:
    // return getString(R.string.title_section2).toUpperCase(l);
    // }
    return null;
  }

  @Override
  public int getItemPosition(Object object) {
	 FragmentMaincard f = (FragmentMaincard) object;
     if (f != null) {
    	// this myselfUserInfo should have been set to new data
        f.update(myselfUserInfo);
     }
    return super.getItemPosition(object);
  }

  @Override
  public int getIconResId(int index) {
    // TODO Auto-generated method stub
    return 0;
  }
}