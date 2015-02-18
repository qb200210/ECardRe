package com.warpspace.ecardv4.infrastructure;

/*
 * Copyright 2014 Niek Haarman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.nhaarman.listviewanimations.ArrayAdapter;
import com.nhaarman.listviewanimations.itemmanipulation.swipedismiss.undo.UndoAdapter;
import com.warpspace.ecardv4.R;

import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;

public class SearchListAdapter extends ArrayAdapter<UserInfo> implements
    UndoAdapter, StickyListHeadersAdapter {

  private final Context mContext;
  ArrayList<UserInfo> localUserList;
  private boolean sortModeName = true;

  public SearchListAdapter(final Context context, ArrayList<UserInfo> names) {
    mContext = context;
    localUserList = names;
    for (int i = 0; i < localUserList.size(); i++) {
      add(localUserList.get(i));
    }
  }

  @Override
  public long getItemId(final int position) {
    return getItem(position).hashCode();
  }

  @Override
  public UserInfo getItem(final int position) {
    return localUserList.get(position);
  }

  @Override
  public boolean hasStableIds() {
    return false;
  }

  public void reSortName(boolean ascending) {
    sortModeName = true;
    Collections.sort(localUserList, new UserInfoNameComparator());

    if (ascending == false) {
      Collections.reverse(localUserList);
    }
  }

  public void reSortDate(boolean ascending) {
    sortModeName = false;
    Collections.sort(localUserList, new UserInfoDateComparator());

    if (ascending == false) {
      Collections.reverse(localUserList);
    }
  }

  @SuppressLint("NewApi")
@Override
  public View getView(final int position, View convertView,
    final ViewGroup parent) {
    if (convertView == null) {
      convertView = LayoutInflater.from(mContext).inflate(
        R.layout.search_result_card, parent, false);
    }

    TextView tv = (TextView) convertView
      .findViewById(R.id.list_row_draganddrop_textview);

    tv.setText(localUserList.get(position).getFirstName());
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
    UserInfo localUser = localUserList.get(position);

    if (sortModeName) {
      String first = localUser.getFirstName();
      if(first!=null && first!=""){
    	  headerText.setText(first.toUpperCase(Locale.ENGLISH).toCharArray(), 0, 1);
      } else{
    	  headerText.setText("null");
      }
    } else {
      headerText.setText(dateToHeaderString(localUser.getCreated()));
    }
    return convertView;
  }

  @Override
  public long getHeaderId(final int position) {
    if (sortModeName) {
      if(localUserList.get(position).getFirstName() != null && localUserList.get(position).getFirstName() != ""){
        return localUserList.get(position).getFirstName().toCharArray()[0];
      } else{
    	  Log.i("getHeaderId", "empty first name");
    	  return 'N';
      }
    } else {
      return dateToHeaderString(localUserList.get(position).getCreated())
        .length();
    }
  }

  private String dateToHeaderString(String date) {
    SimpleDateFormat format = new SimpleDateFormat("MMM d, yyyy, HH:mm",
      Locale.ENGLISH);
    try {
      Date addedDate = format.parse(date);
      Calendar c = Calendar.getInstance();
      Date current = c.getTime();
      String plural = "";
      int yearDiff = current.getYear() - addedDate.getYear();
      int monthDiff = current.getMonth() - addedDate.getMonth();
      int dayDiff = current.getDay() - addedDate.getDay();

      if (yearDiff > 0) {
        plural = yearDiff == 1 ? "" : "s";
        return String.valueOf(yearDiff) + " year" + plural + " ago";
      } else if (monthDiff > 0) {
        plural = monthDiff == 1 ? "" : "s";
        return String.valueOf(monthDiff) + " month" + plural + " ago";
      } else if (dayDiff > 0) {
        plural = dayDiff == 1 ? "" : "s";
        return String.valueOf(dayDiff) + " day" + plural + " ago";
      } else {
        return "Today";
      }
    } catch (ParseException e) {
      return "Unspecified";
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