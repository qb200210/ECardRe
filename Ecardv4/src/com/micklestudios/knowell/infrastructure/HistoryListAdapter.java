package com.micklestudios.knowell.infrastructure;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.micklestudios.knowell.ActivityConversations;
import com.micklestudios.knowell.ActivityHistory;
import com.micklestudios.knowell.ActivityMain;
import com.micklestudios.knowell.ActivitySearch;
import com.nhaarman.listviewanimations.ArrayAdapter;
import com.nhaarman.listviewanimations.itemmanipulation.swipedismiss.undo.UndoAdapter;
import com.parse.ParseObject;
import com.micklestudios.knowell.R;

import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;

public class HistoryListAdapter extends ArrayAdapter<ParseObject> implements
    UndoAdapter, StickyListHeadersAdapter {

  private final Context mContext;
  private boolean sortModeName = true;
  private List<ParseObject> historyObjects;
  protected static final int SHARE_QR_MSG = 1002;
  protected static final int SHARE_QR_EMAIL = 1003;
  protected static final int SHARE_DOC = 1004;

  public HistoryListAdapter(final Context context, List<ParseObject> historyObjects) {
    mContext = context;
    this.historyObjects = historyObjects;
    for (int i = 0; i < historyObjects.size(); i++) {
      add(historyObjects.get(i));
    }
  }

  @Override
  public long getItemId(final int position) {
    return getItem(position).hashCode();
  }

  @Override
  public ParseObject getItem(final int position) {
    return historyObjects.get(position);
  }

  @Override
  public boolean hasStableIds() {
    return false;
  }
  

  @SuppressLint("NewApi")
  @Override
  public View getView(final int position, View convertView,
    final ViewGroup parent) {
    if (convertView == null) {
      convertView = LayoutInflater.from(mContext).inflate(
        R.layout.history_card, parent, false);
    }
    
    ImageView iconView = (ImageView) convertView.findViewById(R.id.history_image);
      Integer typeCode = historyObjects.get(position).getInt("type");
      switch(typeCode){
      case SHARE_QR_MSG:
        iconView.setImageResource(R.drawable.message);
        break;
      case SHARE_QR_EMAIL:
        iconView.setImageResource(R.drawable.mail);
        break;
      case SHARE_DOC:
        iconView.setImageResource(R.drawable.doc_lite);
        break;
        default:
          iconView.setImageResource(R.drawable.history_lite);
          break;
      }

    TextView nameView = (TextView) convertView
      .findViewById(R.id.history_name);
    if(historyObjects.get(position).get("fullName")!=null){
      nameView.setText(historyObjects.get(position).get("fullName").toString());
    } else {
      if(historyObjects.get(position).get("email")!=null){
        nameView.setText(historyObjects.get(position).get("email").toString());
      } else {
        if(historyObjects.get(position).get("message")!=null){
          nameView.setText(historyObjects.get(position).get("message").toString());
        }
      }
    }
    TextView msgView = (TextView) convertView.findViewById(R.id.history_msg);
    if(historyObjects.get(position).get("notes") !=null){
      msgView.setText(historyObjects.get(position).get("notes").toString());
    }
    TextView updatedAt = (TextView) convertView.findViewById(R.id.history_date);
    if(historyObjects.get(position).getObjectId() == null){
      Date date = new Date();
      updatedAt.setText(android.text.format.DateFormat.format("MMM",
        date)
        + " "
        + android.text.format.DateFormat.format("dd", date));
    } else {
      updatedAt.setText(android.text.format.DateFormat.format("MMM",
        historyObjects.get(position).getCreatedAt())
        + " "
        + android.text.format.DateFormat.format("dd", historyObjects.get(position).getCreatedAt()));
    }
    return convertView;
  }
  
  public void reSortDate(boolean ascending) {

    Comparator<ParseObject> comparer = new HistoryDateComparator();

    if (ascending == false) {
      comparer = Collections.reverseOrder(comparer);
    }
    Collections.sort(historyObjects, comparer);
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

  @Override
  public View getHeaderView(int position, View convertView, ViewGroup parent) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public long getHeaderId(int position) {
    // TODO Auto-generated method stub
    return 0;
  }
}