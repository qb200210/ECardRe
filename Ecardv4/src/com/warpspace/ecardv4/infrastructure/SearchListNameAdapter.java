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

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.nhaarman.listviewanimations.ArrayAdapter;
import com.nhaarman.listviewanimations.itemmanipulation.swipedismiss.undo.UndoAdapter;
import com.warpspace.ecardv4.R;

import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;

public class SearchListNameAdapter extends ArrayAdapter<String> implements
    UndoAdapter, StickyListHeadersAdapter {

  private final Context mContext;

  public SearchListNameAdapter(final Context context) {
    mContext = context;
    for (int i = 0; i < 1000; i++) {
      add(mContext.getString(R.string.row_number, i));
    }
  }

  @Override
  public long getItemId(final int position) {
    return getItem(position).hashCode();
  }

  @Override
  public boolean hasStableIds() {
    return true;
  }

  @Override
  public View getView(final int position, final View convertView,
    final ViewGroup parent) {
    TextView view = (TextView) convertView;
    if (view == null) {
      view = (TextView) LayoutInflater.from(mContext).inflate(
        R.layout.search_result_card, parent, false);
    }

    view.setText(getItem(position));

    return view;
  }

  @Override
  public View getHeaderView(final int position, final View convertView,
    final ViewGroup parent) {
    TextView view = (TextView) convertView;
    if (view == null) {
      view = (TextView) LayoutInflater.from(mContext).inflate(
        R.layout.search_result_header, parent, false);
    }

    view.setText("Hello");

    return view;
  }

  @Override
  public long getHeaderId(final int position) {
    return position / 10;
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