package com.micklestudios.knowells.infrastructure;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class AutoCompleteAdapter extends ArrayAdapter<String> {
  private LayoutInflater mInflater;

  public AutoCompleteAdapter(Context context, int resource) {
    super(context, resource);
    mInflater = LayoutInflater.from(context);
  }

  @Override
  public View getView(final int position, final View convertView,
    final ViewGroup parent) {
    final TextView tv;
    if (convertView != null) {
      tv = (TextView) convertView;
    } else {
      tv = (TextView) mInflater.inflate(
        android.R.layout.simple_dropdown_item_1line, parent, false);
    }

    tv.setText(null);
    return tv;
  }

}
