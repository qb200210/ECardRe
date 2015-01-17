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

import com.viewpagerindicator.IconPagerAdapter;
import com.warpspace.ecardv4.FragmentMaincard;
import com.warpspace.ecardv4.FragmentQrcode;

public class MyPagerAdapter extends FragmentPagerAdapter implements
    IconPagerAdapter {


public MyPagerAdapter(FragmentManager fm) {
    super(fm);
  }

  @Override
  public Fragment getItem(int position) {
    // getItem is called to instantiate the fragment for the given page.
    // Return a PlaceholderFragment (defined as a static inner class
    // below).
    switch (position) {
    case 0:
      return FragmentMaincard.newInstance(1);
    case 1:
      return FragmentQrcode.newInstance(2);
    default:
      return FragmentQrcode.newInstance(2);
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
  public int getIconResId(int index) {
    // TODO Auto-generated method stub
    return 0;
  }
}