package com.warpspace.ecardv4.utils;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.warpspace.ecardv4.R;

public class MySimpleListViewAdapter extends BaseAdapter {

	private Context context;
	private String listValues[];

	public MySimpleListViewAdapter(Context context, String listValues[]) {
		this.context = context;
		this.listValues = listValues;
	}

	public View getView(int position, View convertView, ViewGroup parent) {

		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		View listView;

		if (convertView == null) {

			listView = new View(context);
			listView = inflater.inflate(R.layout.layout_dialog_addmore_perrow, null);

			TextView tv = (TextView) listView.findViewById(R.id.dialog_item_text);
			tv.setText(listValues[position]);

		} else {
			listView = (View) convertView;
		}

		return listView;
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
}
