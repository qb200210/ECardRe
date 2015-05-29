/**
 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */

package com.micklestudios.knowell.utils;

import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.util.Log;
import android.widget.Toast;

import com.micklestudios.knowell.ActivityMain;
import com.micklestudios.knowell.FragmentCollectedCard;
import com.micklestudios.knowell.FragmentInfoDisplay;
import com.micklestudios.knowell.FragmentMaincard;
import com.micklestudios.knowell.infrastructure.UserInfo;

public class DetailsPagerAdapter extends FragmentPagerAdapter {

private UserInfo newUser;
private Activity activity;

public DetailsPagerAdapter(FragmentManager fm, UserInfo newUser, Activity activity) {
    super(fm);
    this.activity = activity;
    this.newUser = newUser;
  }

  @Override
  public Fragment getItem(int position) {
    // getItem is called to instantiate the fragment for the given page.
    // Return a PlaceholderFragment (defined as a static inner class
    // below).
    switch (position % 2) {
    case 0:
      return FragmentCollectedCard.newInstance(1, newUser);
    case 1:
      return FragmentInfoDisplay.newInstance(2, newUser);
    default:
      return FragmentInfoDisplay.newInstance(2, newUser);
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
}