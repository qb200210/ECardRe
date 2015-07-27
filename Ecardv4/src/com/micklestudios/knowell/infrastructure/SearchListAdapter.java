package com.micklestudios.knowell.infrastructure;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.text.Spannable;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.micklestudios.knowell.ActivityDetails;
import com.micklestudios.knowell.ActivitySearch;
import com.nhaarman.listviewanimations.ArrayAdapter;
import com.nhaarman.listviewanimations.itemmanipulation.swipedismiss.undo.UndoAdapter;
import com.micklestudios.knowell.R;
import com.micklestudios.knowell.utils.AppGlobals;

public class SearchListAdapter extends ArrayAdapter<UserInfo> implements
    UndoAdapter, StickyListHeadersAdapter {

  private final Context mContext;
  private boolean sortModeName = true;

  public SearchListAdapter(final Context context, ArrayList<Integer> users) {
    mContext = context;
    refreshData(users);
  }

  public void refreshData(ArrayList<Integer> users) {
    clear();
    for (int i = 0; i < ActivitySearch.filteredUsers.size(); i++) {
      add(AppGlobals.allUsers.get(ActivitySearch.filteredUsers.get(i)));
    }
  }

  @Override
  public long getItemId(final int position) {
    return getItem(position).hashCode();
  }

  @Override
  public UserInfo getItem(final int position) {
    return AppGlobals.allUsers.get(ActivitySearch.filteredUsers.get(position));
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
    Comparator<Integer> comparer = new UserInfoNameComparator();

    if (ascending == false) {
      comparer = Collections.reverseOrder(comparer);
    }
    Collections.sort(ActivitySearch.filteredUsers, comparer);
  }

  public void reSortDate(boolean ascending) {
    sortModeName = false;

    Comparator<Integer> comparer = new UserInfoDateComparator();

    if (ascending == false) {
      comparer = Collections.reverseOrder(comparer);
    }
    Collections.sort(ActivitySearch.filteredUsers, comparer);
  }

  public void addToSelectedUsers(UserInfo uInfo) {
    // If this is already selected, remove it.
    if (ActivitySearch.selectedUsers.remove(uInfo) == false) {
      ActivitySearch.selectedUsers.add(uInfo);
    } else {
      ActivitySearch.selectedUsers.remove(uInfo);
    }

    notifyDataSetChanged();
  }

  @SuppressLint("NewApi")
  @Override
  public View getView(final int position, View convertView,
    final ViewGroup parent) {
    if (convertView == null) {
      convertView = LayoutInflater.from(mContext).inflate(
        R.layout.search_result_card, parent, false);
    }

    final UserInfo uInfo = AppGlobals.allUsers.get(ActivitySearch.filteredUsers
      .get(position));

    ImageView portraitImg = (ImageView) convertView
      .findViewById(R.id.search_image);
    final CheckBox selectionBox = (CheckBox) convertView
      .findViewById(R.id.chk_contact_select);

    convertView.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        if (ActivitySearch.isSelectionMode == false) {
          ActivitySearch.showClickedUser(uInfo);
        } else {
          selectionBox.setChecked(!selectionBox.isChecked());
          addToSelectedUsers(uInfo);
        }
      }
    });

    convertView.setOnLongClickListener(new OnLongClickListener() {
      @Override
      public boolean onLongClick(View v) {
        addToSelectedUsers(uInfo);
        return false;
      }
    });

    selectionBox.setClickable(false);

    if (ActivitySearch.isSelectionMode == false) {
      selectionBox.setVisibility(View.INVISIBLE);
    } else {
      selectionBox.setVisibility(View.VISIBLE);
    }

    boolean isSelected = ActivitySearch.selectedUsers.contains(uInfo);
    selectionBox.setChecked(isSelected);

    if (uInfo.getPortrait() != null) {
      portraitImg.setImageBitmap(uInfo.getPortrait());
    }

    String nameString = uInfo.getFirstName() + " " + uInfo.getLastName();
    String jobString = uInfo.getTitle() + " at " + uInfo.getCompany();
    String cityString = uInfo.getCity();

    Integer fieldName = ActivitySearch.matchedFields
      .get(ActivitySearch.filteredUsers.get(position));
    String matchString = null;
    TextView matchTv = null;
    int matchStart = 0;
    int matchEnd = 0;

    TextView nameView = (TextView) convertView
      .findViewById(R.id.search_result_card_name);
    nameView.setText(uInfo.getFirstName() + " " + uInfo.getLastName());
    TextView textCompany = (TextView) convertView
      .findViewById(R.id.search_result_card_job_company);
    textCompany.setText(uInfo.getTitle() + " at " + uInfo.getCompany());
    TextView textDetail = (TextView) convertView
      .findViewById(R.id.search_result_card_address);
    textDetail.setText(uInfo.getCity());

    if (fieldName != null) {
      switch (fieldName) {
      case UserInfo.FIELD_TYPE.TYPE_FNAME:
        matchString = nameString;
        matchStart = 0;
        matchEnd = matchString.length();
        matchTv = nameView;
        break;
      case UserInfo.FIELD_TYPE.TYPE_TITLE:
        matchString = jobString;
        matchStart = 0;
        matchEnd = uInfo.getTitle().length();
        matchTv = textCompany;
        break;
      case UserInfo.FIELD_TYPE.TYPE_COMPANY:
        matchString = jobString;
        matchStart = uInfo.getTitle().length() + " at ".length();
        matchEnd = matchString.length();
        matchTv = textCompany;
        break;
      case UserInfo.FIELD_TYPE.TYPE_CITY:
        matchString = cityString;
        matchTv = textDetail;
        matchStart = 0;
        matchEnd = matchString.length();
        break;
      case UserInfo.FIELD_TYPE.TYPE_WHERE_MET:
        matchString = uInfo.getWhereMet();
        matchTv = textDetail;
        matchStart = 0;
        matchEnd = matchString.length();
        break;
      case UserInfo.FIELD_TYPE.TYPE_EVENT_MET:
        matchString = uInfo.getWhereMet();
        matchTv = textDetail;
        matchStart = 0;
        matchEnd = matchString.length();
        break;
      }
    }

    if (matchString != null) {
      Log.e("Knowell", "Matched string is " + matchString);
      Spannable spanText = Spannable.Factory.getInstance().newSpannable(
        matchString);
      spanText.setSpan(new BackgroundColorSpan(0xFFff8a65), matchStart,
        matchEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
      matchTv.setText(spanText);
    }

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
    UserInfo localUser = AppGlobals.allUsers.get(ActivitySearch.filteredUsers
      .get(position));

    if (sortModeName) {
      String first = localUser.getFirstName();
      if (first != null && !first.isEmpty()) {
        headerText.setText(first.toUpperCase(Locale.ENGLISH).toCharArray(), 0,
          1);
      } else {
        headerText.setText("null");
      }
    } else {
      headerText.setText(dateToHeaderString(localUser.getWhenMet()));
    }
    return convertView;
  }

  @Override
  public long getHeaderId(final int position) {
    UserInfo uInfo = AppGlobals.allUsers.get(ActivitySearch.filteredUsers
      .get(position));
    if (sortModeName) {
      String firstName = uInfo.getFirstName();
      if (firstName != null && !firstName.isEmpty()) {
        return firstName.toUpperCase(Locale.ENGLISH).toCharArray()[0];
      } else {
        Log.i("getHeaderId", "empty first name");
        return 'N';
      }
    } else {
      return dateToHeaderString(uInfo.getWhenMet()).length();
    }
  }

  private String dateToHeaderString(Date addedDate) {
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