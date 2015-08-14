/**
 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */

package com.micklestudios.knowells.utils;

import java.util.Locale;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.util.Log;
import android.widget.Toast;

import com.micklestudios.knowells.ActivityMain;
import com.micklestudios.knowells.FragmentMaincard;
import com.micklestudios.knowells.infrastructure.UserInfo;

public class MyPagerAdapter extends FragmentPagerAdapter {

  public MyPagerAdapter(FragmentManager fm) {
    super(fm);
  }

  @Override
  public Fragment getItem(int position) {
    // getItem is called to instantiate the fragment for the given page.
    // Return a PlaceholderFragment (defined as a static inner class
    // below).
    switch (position % 2) {
    case 0:
      return FragmentMaincard.newInstance(1);
    case 1:
      return FragmentMaincard.newInstance(2);
    default:
      return FragmentMaincard.newInstance(2);
    }
  }

  @Override
  public int getCount() {
    // Show 2 total pages.
    return Integer.MAX_VALUE;
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
      f.update(ActivityMain.myselfUserInfo);
    }
    return super.getItemPosition(object);
  }
}