package com.warpspace.ecardv4;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class FragmentMaincard extends Fragment {

  private static final String ARG_SECTION_NUMBER = "section_number";

  public static FragmentMaincard newInstance(int sectionNumber) {
    FragmentMaincard fragment = new FragmentMaincard();
    Bundle args = new Bundle();
    args.putInt(ARG_SECTION_NUMBER, sectionNumber);
    fragment.setArguments(args);
    return fragment;
  }

  public FragmentMaincard() {

  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
    Bundle savedInstanceState) {
    View rootView = inflater.inflate(R.layout.fragment_maincard, container,
      false);
    setHasOptionsMenu(true);
    return rootView;
  }
}
