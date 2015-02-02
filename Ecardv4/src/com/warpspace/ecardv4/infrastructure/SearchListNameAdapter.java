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

import java.util.ArrayList;
import java.util.Collections;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.nhaarman.listviewanimations.ArrayAdapter;
import com.nhaarman.listviewanimations.itemmanipulation.swipedismiss.undo.UndoAdapter;
import com.warpspace.ecardv4.R;
import com.warpspace.ecardv4.utils.SquareLayoutSpecial;

import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;

public class SearchListNameAdapter extends ArrayAdapter<UserInfo> implements
    UndoAdapter, StickyListHeadersAdapter {

  private final Context mContext;
  ArrayList<UserInfo> localUserList;

  public SearchListNameAdapter(final Context context, ArrayList<UserInfo> names) {
    mContext = context;
    localUserList = names;
    for (int i = 0; i < localUserList.size(); i++) {
      add(localUserList.get(i));
    }
  }

  @Override
  public long getItemId(final int position) {
    return localUserList.get(position).hashCode();
  }

  @Override
  public boolean hasStableIds() {
    return false;
  }

  public void reSortName(boolean ascending) {
    Collections.sort(localUserList, new UserNameComparator());

    if (ascending == false) {
      Collections.reverse(localUserList);
    }
  }

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
    int position2 = (int) getHeaderId(position);
    String first = Character.toString((char) position2);
    headerText.setText(first.toCharArray(), 0, 1);

    return convertView;
  }

  @Override
  public long getHeaderId(final int position) {
    return localUserList.get(position).getFirstName().charAt(0);
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