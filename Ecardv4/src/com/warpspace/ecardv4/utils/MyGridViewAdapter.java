package com.warpspace.ecardv4.utils;

import java.util.ArrayList;

import com.warpspace.ecardv4.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

public class MyGridViewAdapter extends BaseAdapter {

  private Context context;

  private ArrayList<String> gridKeys = new ArrayList<String>();
  private ArrayList<String> gridValues = new ArrayList<String>();
  private ArrayList<Integer> gridResources = new ArrayList<Integer>();

  public MyGridViewAdapter(Context context, ArrayList<String> gridKeys,
    ArrayList<String> gridValues, ArrayList<Integer> gridResources) {
    this.context = context;
    this.gridKeys = gridKeys;
    this.gridValues = gridValues;
    this.gridResources = gridResources;
  }

  public View getView(int position, View convertView, ViewGroup parent) {

    LayoutInflater inflater = (LayoutInflater) context
      .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    View gridView;

    if (convertView == null) {

      gridView = new View(context);

      // get layout from mobile.xml
      gridView = inflater.inflate(R.layout.layout_info_item, null);

      // set image based on selected text
      ImageView imageView = (ImageView) gridView
        .findViewById(R.id.grid_item_image);

      imageView.setImageResource(gridResources.get(position));
      gridView.setTag(new MyTag(gridKeys.get(position), gridValues
        .get(position)));
      // imageView.setImageResource(R.drawable.ic_launcher);

    } else {
      gridView = (View) convertView;
    }

    return gridView;
  }

  @Override
  public int getCount() {
    // TODO Auto-generated method stub
    return gridValues.size();
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

}
