package com.micklestudios.knowell;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.micklestudios.knowell.infrastructure.UserInfo;
import com.micklestudios.knowell.utils.ECardUtils;
import com.micklestudios.knowell.utils.ExpandableHeightGridView;
import com.micklestudios.knowell.utils.MyDetailsGridViewAdapter;
import com.micklestudios.knowell.utils.MyTag;
import com.micklestudios.knowell.utils.UpdateableFragment;

public class FragmentInfoDisplay extends Fragment implements UpdateableFragment {

  private static final String ARG_SECTION_NUMBER = "section_number";
  ArrayList<String> shownArrayList = new ArrayList<String>();
  ArrayList<Integer> infoIcon = new ArrayList<Integer>();
  ArrayList<String> infoLink = new ArrayList<String>();

  ExpandableHeightGridView gridView;

  public static FragmentInfoDisplay newInstance(int sectionNumber,
    UserInfo newUser) {
    FragmentInfoDisplay fragment = new FragmentInfoDisplay();
    Bundle args = new Bundle();
    args.putInt(ARG_SECTION_NUMBER, sectionNumber);
    args.putParcelable("newUser", newUser);
    fragment.setArguments(args);
    return fragment;
  }

  private View rootView;

  public FragmentInfoDisplay() {

  }

  @Override
  public void setUserVisibleHint(boolean isVisibleToUser) {
    super.setUserVisibleHint(isVisibleToUser);
    if (isVisibleToUser) {
      if (gridView != null) {
        gridView.setEnabled(true);
      }
    } else {
      if (gridView != null) {
        gridView.setEnabled(false);
      }
    }
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    Log.v("onsave", "In frag's on save instance state ");
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
    Bundle savedInstanceState) {
    Bundle bundle = this.getArguments();
    UserInfo newUser = bundle.getParcelable("newUser");
    rootView = inflater.inflate(R.layout.fragment_info_display, container,
      false);
    gridView = (ExpandableHeightGridView) rootView.findViewById(R.id.gridView2);
    setExtraInfo(newUser);
    setHasOptionsMenu(true);
    return rootView;

  }

  public void setExtraInfo(UserInfo newUser) {
    // display extra info
    infoIcon = newUser.getInfoIcon();
    infoLink = newUser.getInfoLink();
    shownArrayList = newUser.getShownArrayList();

    if (gridView != null) {
      gridView.setAdapter(new MyDetailsGridViewAdapter(getActivity(),
        shownArrayList, infoLink, infoIcon));
      gridView.setOnItemClickListener(new OnItemClickListener() {

        @SuppressLint("NewApi")
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
          long id) {

          MyTag tag = (MyTag) view.getTag();
          if (tag != null) {
            Intent intent;
            switch (((MyTag) view.getTag()).getKey().toString()) {
            case "phone":
              intent = new Intent(Intent.ACTION_VIEW, Uri.parse("tel:"
                + ((MyTag) view.getTag()).getValue().toString()));
              startActivity(intent);
              break;
            case "message":
              intent = new Intent(Intent.ACTION_VIEW, Uri.parse("smsto:"
                + ((MyTag) view.getTag()).getValue().toString()));
              startActivity(intent);
              break;
            case "email":
              intent = new Intent(Intent.ACTION_VIEW, Uri.parse("mailto:"
                + ((MyTag) view.getTag()).getValue().toString()));
              startActivity(intent);
              break;
            case "about":
              buildAboutMeDialog(getActivity(), view);
              break;
            default:
              String url = ((MyTag) view.getTag()).getValue().toString();
              if (!url.startsWith("http://") && !url.startsWith("https://")
                && !url.startsWith("ftp://")) {
                url = "http://www.google.com/#q=" + url;
              }
              intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
              startActivity(intent);
            }
          }

        }

      });
    }
  }

  @SuppressLint("NewApi")
  protected void buildAboutMeDialog(Activity activity, View view) {
    // Get the layout inflater
    LayoutInflater inflater = activity.getLayoutInflater();
    View dialogView = inflater.inflate(R.layout.layout_dialog_scanned_peritem,
      null);
    LinearLayout dialogHeader = (LinearLayout) dialogView
      .findViewById(R.id.dialog_header);
    final TextView dialogText = (TextView) dialogView
      .findViewById(R.id.dialog_text);
    TextView dialogTitle = (TextView) dialogView
      .findViewById(R.id.dialog_title);
    // // Set dialog header background with rounded corner
    // Bitmap bm = BitmapFactory
    // .decodeResource(getResources(), R.drawable.striped);
    // BitmapDrawable bmDrawable = new BitmapDrawable(getResources(), bm);
    // dialogHeader.setBackground(new CurvedAndTiled(bmDrawable.getBitmap(),
    // 5)); \n vvvvvvvv
    dialogHeader
      .setBackgroundColor(getResources().getColor(R.color.blue_extra));
    // Set dialog title and main EditText
    dialogTitle.setText("About Me");
    dialogText.setText(((MyTag) view.getTag()).getValue().toString());

    new AlertDialog.Builder(activity).setView(dialogView)
      .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int whichButton) {

        }
      }).show();

  }

  public void displayCard(View rootView, UserInfo newUser) {

    TextView name = (TextView) rootView.findViewById(R.id.my_name);
    String tmpString = newUser.getFirstName();
    String nameString = null;
    if (tmpString != null)
      nameString = tmpString;
    tmpString = newUser.getLastName();
    if (tmpString != null)
      nameString = nameString + " " + tmpString;
    if (nameString != null)
      name.setText(nameString);
    name = (TextView) rootView.findViewById(R.id.my_com);
    tmpString = newUser.getCompany();
    if (tmpString != null) {
      name.setText(tmpString);
      ImageView logoImg = (ImageView) rootView.findViewById(R.id.my_logo);
      // display logo
      ECardUtils.findAndSetLogo(getActivity(), logoImg, tmpString, true);
    }
    name = (TextView) rootView.findViewById(R.id.my_job_title);
    tmpString = newUser.getTitle();
    if (tmpString != null)
      name.setText(tmpString);
    name = (TextView) rootView.findViewById(R.id.my_add);
    tmpString = newUser.getCity();
    if (tmpString != null)
      name.setText(tmpString);
    ImageView portraitImg = (ImageView) rootView.findViewById(R.id.my_portrait);
    if (newUser.getPortrait() != null) {
      portraitImg.setImageBitmap(newUser.getPortrait());
    }

  }

  @Override
  public void update(UserInfo userInfo) {
    if (getArguments().getInt(ARG_SECTION_NUMBER, 1) == 1) {
      // update the main card
      displayCard(rootView, userInfo);
    }
    if (getArguments().getInt(ARG_SECTION_NUMBER, 1) == 2) {
      // For unknown reason, trying to rootView.findViewById(anything) will
      // crash
      // Luckily QR code doesn't need to change noticeably. Next time app
      // restarts
      // Changes in QR code can be reflected
    }

  }
}
