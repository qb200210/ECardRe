package com.warpspace.ecardv4;

import com.warpspace.ecardv4.R;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class FragmentQrcode extends Fragment {

  private static final String ARG_SECTION_NUMBER = "section_number";

  public static FragmentQrcode newInstance(int sectionNumber) {
    FragmentQrcode fragment = new FragmentQrcode();
    return fragment;
  }

  public FragmentQrcode() {

  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
    Bundle savedInstanceState) {
    View rootView = inflater.inflate(R.layout.fragment_qr, container, false);
    setHasOptionsMenu(true);
    return rootView;
  }
}
