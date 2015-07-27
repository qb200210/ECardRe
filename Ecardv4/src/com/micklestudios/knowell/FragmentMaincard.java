package com.micklestudios.knowell;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.micklestudios.knowell.infrastructure.UserInfo;
import com.micklestudios.knowell.utils.ECardUtils;
import com.micklestudios.knowell.utils.UpdateableFragment;

public class FragmentMaincard extends Fragment implements UpdateableFragment {

  private static final String ARG_SECTION_NUMBER = "section_number";

  public static FragmentMaincard newInstance(int sectionNumber) {
    Log.i("maincard", "newinstance");
    FragmentMaincard fragment = new FragmentMaincard();
    Bundle args = new Bundle();
    args.putInt(ARG_SECTION_NUMBER, sectionNumber);
    fragment.setArguments(args);
    return fragment;
  }

  private View rootView;

  public FragmentMaincard() {

  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
    Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Bundle bundle = this.getArguments();

    Log.i("maincard", "oncreateview");
    if (getArguments().getInt(ARG_SECTION_NUMBER, 1) == 1) {
      // if this is to create maincard fragment
      rootView = inflater.inflate(R.layout.fragment_maincard, container, false);

      // display the main card
      if (ActivityMain.myselfUserInfo != null) {
        displayCard(rootView, ActivityMain.myselfUserInfo);
      }
      setHasOptionsMenu(true);
      return rootView;
    }
    if (getArguments().getInt(ARG_SECTION_NUMBER, 1) == 2) {
      View rootView = inflater.inflate(R.layout.fragment_qr, container, false);
      if (ActivityMain.myselfUserInfo != null) {
        ImageView qrCode = (ImageView) rootView.findViewById(R.id.qr_container);
        qrCode.setImageBitmap(ActivityMain.myselfUserInfo.getQRCode());
      }
      setHasOptionsMenu(true);
      return rootView;
    }
    return null;
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
