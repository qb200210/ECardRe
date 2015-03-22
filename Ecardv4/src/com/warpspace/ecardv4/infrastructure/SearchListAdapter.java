package com.warpspace.ecardv4.infrastructure;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.nhaarman.listviewanimations.ArrayAdapter;
import com.nhaarman.listviewanimations.itemmanipulation.swipedismiss.undo.UndoAdapter;
import com.warpspace.ecardv4.ActivitySearch;
import com.warpspace.ecardv4.R;

public class SearchListAdapter extends ArrayAdapter<UserInfo> implements
    UndoAdapter, StickyListHeadersAdapter {

  private final Context mContext;
  private boolean sortModeName = true;

  public SearchListAdapter(final Context context, ArrayList<UserInfo> users) {
    mContext = context;
    refreshData(users);
  }

  public void refreshData(ArrayList<UserInfo> users) {
    clear();
    for (int i = 0; i < ActivitySearch.filteredUsers.size(); i++) {
      add(ActivitySearch.filteredUsers.get(i));
    }
  }

  @Override
  public long getItemId(final int position) {
    return getItem(position).hashCode();
  }

  @Override
  public UserInfo getItem(final int position) {
    return ActivitySearch.filteredUsers.get(position);
  }

  @Override
  public boolean hasStableIds() {
    return false;
  }

  // Resorts the list based on ActivitySearch.currentSortMode
  public void reSort() {
    switch (ActivitySearch.currentSortMode) {
    case ActivitySearch.SORT_MODE_NAME_ASC:
      reSortName(true);
      break;
    case ActivitySearch.SORT_MODE_NAME_DSC:
      reSortName(false);
      break;
    case ActivitySearch.SORT_MODE_DATE_ASC:
      reSortDate(true);
      break;
    case ActivitySearch.SORT_MODE_DATE_DSC:
      reSortDate(false);
      break;
    }
  }

  public void reSortName(boolean ascending) {
    sortModeName = true;
    Comparator<UserInfo> comparer = new UserInfoNameComparator();

    if (ascending == false) {
      comparer = Collections.reverseOrder(comparer);
    }
    Collections.sort(ActivitySearch.filteredUsers, comparer);
  }

  public void reSortDate(boolean ascending) {
    sortModeName = false;

    Comparator<UserInfo> comparer = new UserInfoDateComparator();

    if (ascending == false) {
      comparer = Collections.reverseOrder(comparer);
    }
    Collections.sort(ActivitySearch.filteredUsers, comparer);
  }

  @SuppressLint("NewApi")
  @Override
  public View getView(final int position, View convertView,
    final ViewGroup parent) {
    if (convertView == null) {
      convertView = LayoutInflater.from(mContext).inflate(
        R.layout.search_result_card, parent, false);
    }

    ImageView portraitImg = (ImageView) convertView
      .findViewById(R.id.search_image);
    if (ActivitySearch.filteredUsers.get(position).getPortrait() != null) {
      portraitImg.setImageBitmap(ActivitySearch.filteredUsers.get(position)
        .getPortrait());
    }

    TextView tv = (TextView) convertView
      .findViewById(R.id.list_row_draganddrop_textview);

    tv.setText(ActivitySearch.filteredUsers.get(position).getFirstName());
    tv.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);

    return convertView;
  }

  @Override
  public View getHeaderView(final int position, View convertView,
    final ViewGroup parent) {
    if (convertView == null) {
      convertView = LayoutInflater.from(mContext).inflate(
        R.layout.search_result_header, parent, false);
    }

    TextView headerText = (TextView) convertView.findViewById(R.id.text_header);
    UserInfo localUser = ActivitySearch.filteredUsers.get(position);

    if (sortModeName) {
      String first = localUser.getFirstName();
      if (first != null && first != "") {
        headerText.setText(first.toUpperCase(Locale.ENGLISH).toCharArray(), 0,
          1);
      } else {
        headerText.setText("null");
      }
    } else {
      headerText.setText(dateToHeaderString(localUser.getCreatedAt()));
    }
    return convertView;
  }

  @Override
  public long getHeaderId(final int position) {
    if (sortModeName) {
      if (ActivitySearch.filteredUsers.get(position).getFirstName() != null
        && ActivitySearch.filteredUsers.get(position).getFirstName() != "") {
        return ActivitySearch.filteredUsers.get(position).getFirstName()
          .toUpperCase(Locale.ENGLISH).toCharArray()[0];
      } else {
        Log.i("getHeaderId", "empty first name");
        return 'N';
      }
    } else {
      return dateToHeaderString(
        ActivitySearch.filteredUsers.get(position).getCreatedAt()).length();
    }
  }

  private String dateToHeaderString(Date addedDate) {
    Log.e("Dates parsed", "Created at " + addedDate);

    final int SEC = 1000;
    final int MIN = SEC * 60;
    final int HOUR = MIN * 60;
    final int DAY = HOUR * 24;
    final long WEEK = DAY * 7;
    final long YEAR = WEEK * 52;

    Calendar c = Calendar.getInstance();
    Date current = c.getTime();
    long currentTimeMS = current.getTime();
    long addedTimeMS = addedDate.getTime();

    long dateDiffMS = currentTimeMS - addedTimeMS;

    String plural = "";
    int yearDiff = (int) (dateDiffMS / YEAR);
    int dayDiff = (int) (dateDiffMS / DAY);
    int weekDiff = (int) (dateDiffMS / WEEK);

    Log.e("Dates parsed", "Today is " + current + " dd " + dayDiff + " wd "
      + weekDiff + " yd " + yearDiff);

    if (dayDiff == 0) {
      return "Today";
    } else if (dayDiff < 7) {
      plural = dayDiff == 1 ? "" : "s";
      return String.valueOf(dayDiff) + " day" + plural + " ago";
    } else if (weekDiff < 52) {
      plural = weekDiff == 1 ? "" : "s";
      return String.valueOf(weekDiff) + " week" + plural + " ago";
    } else {
      plural = yearDiff == 1 ? "" : "s";
      return String.valueOf(yearDiff) + " year" + plural + " ago";
    }
  }

  @Override
  public View getUndoClickView(View arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public View getUndoView(int arg0, View arg1, ViewGroup arg2) {
    // TODO Auto-generated method stub
    return null;
  }
}