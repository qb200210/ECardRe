package com.micklestudios.knowell;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;

import com.google.zxing.client.android.Intents;
import com.micklestudios.knowell.infrastructure.UserInfo;
import com.micklestudios.knowell.utils.CurvedAndTiled;
import com.micklestudios.knowell.utils.CustomQRScanner;
import com.micklestudios.knowell.utils.ECardUtils;
import com.micklestudios.knowell.utils.MyTag;
import com.micklestudios.knowell.utils.RobotoEditText;
import com.micklestudios.knowell.utils.SquareLayout;
import com.micklestudios.knowell.utils.UpdateableFragment;
import com.parse.ParseUser;
import com.micklestudios.knowell.R;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class FragmentCollectedCard extends Fragment implements UpdateableFragment {

  private static final String ARG_SECTION_NUMBER = "section_number";

  public static FragmentCollectedCard newInstance(int sectionNumber, UserInfo newUser) {
    Log.i("maincard", "newinstance");
    FragmentCollectedCard fragment = new FragmentCollectedCard();
    Bundle args = new Bundle();
    args.putInt(ARG_SECTION_NUMBER, sectionNumber);
    args.putParcelable("newUser", newUser);
    fragment.setArguments(args);
    return fragment;
  }

  private View rootView;
  

  public FragmentCollectedCard() {

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

    Log.i("maincard", "oncreateview");
    // if this is to create maincard fragment
    rootView = inflater.inflate(R.layout.fragment_maincard, container, false);

    // display the main card
    if (newUser != null) {
      displayCard(rootView, newUser);
    }
    setHasOptionsMenu(true);
    return rootView;

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
