package com.micklestudios.knowells.utils;

import java.util.ArrayList;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.micklestudios.knowells.R;

public class MySimpleListViewAdapter extends BaseAdapter {

  private Context context;
  private String listValues[];
  private ArrayList<Integer> gridResources;

  public MySimpleListViewAdapter(Context context, String listValues[],
    ArrayList<Integer> gridResources) {
    this.context = context;
    this.listValues = listValues;
    this.gridResources = gridResources;
    Log.i("adapter", gridResources.toString());
  }

  public View getView(int position, View convertView, ViewGroup parent) {

    LayoutInflater inflater = (LayoutInflater) context
      .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    View row = convertView;
    final MyViewHolder holder;

    if (row == null) {

      row = inflater.inflate(R.layout.layout_dialog_addmore_perrow, null);
      holder = new MyViewHolder();

      holder.tv = (TextView) row.findViewById(R.id.dialog_item_text);
      holder.icon = (ImageView) row.findViewById(R.id.dialog_addinfo_icon);
      row.setTag(holder);

    } else {
      holder = (MyViewHolder) row.getTag();
    }
    // Log.i("adapter", position + "  "+ listValues[position]+ "  " +
    // listValues.length);
    holder.tv.setText(listValues[position]);
    holder.icon.setImageResource(gridResources.get(position));

    return row;
  }

  @Override
  public int getCount() {
    // TODO Auto-generated method stub
    return listValues.length;
  }

  @Override
  public Object getItem(int position) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public long getItemId(int position) {
    // TODO Auto-generated method stub
    return 0;
  }

  static class MyViewHolder {
    TextView tv;
    ImageView icon;
  }

}
