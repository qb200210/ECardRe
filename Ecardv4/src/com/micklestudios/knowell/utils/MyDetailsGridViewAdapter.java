package com.micklestudios.knowell.utils;

import java.util.ArrayList;

import com.micklestudios.knowell.R;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.Toast;

public class MyDetailsGridViewAdapter extends BaseAdapter {

  private Context context;

  private ArrayList<String> gridKeys = new ArrayList<String>();
  private ArrayList<String> gridValues = new ArrayList<String>();
  private ArrayList<Integer> gridResources = new ArrayList<Integer>();

  public MyDetailsGridViewAdapter(Context context, ArrayList<String> gridKeys,
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
      gridView = inflater.inflate(R.layout.layout_info_item, null);

      ImageView imageView = (ImageView) gridView
        .findViewById(R.id.grid_item_image);

      imageView.setImageResource(gridResources.get(position));
      gridView.setTag(new MyTag(gridKeys.get(position), gridValues
        .get(position)));

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
