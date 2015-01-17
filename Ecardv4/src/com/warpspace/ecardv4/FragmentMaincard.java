package com.warpspace.ecardv4;

import com.parse.ParseUser;
import com.warpspace.ecardv4.infrastructure.UserInfo;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class FragmentMaincard extends Fragment {

  private static final String ARG_SECTION_NUMBER = "section_number";
  ParseUser currentUser;

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
    
    currentUser = ParseUser.getCurrentUser();
    UserInfo myself = new UserInfo(getActivity().getApplicationContext(), currentUser.get("EcardID").toString(), "", "", true, true);
	// display the main card
	displayCard(rootView, myself);
    setHasOptionsMenu(true);
    return rootView;
  }
  
	public void displayCard(View rootView, UserInfo newUser) {

		TextView name = (TextView) rootView.findViewById(R.id.my_first_name);
		String tmpString = newUser.getFirstName();
		if (tmpString != null)
			name.setText(tmpString);
		name = (TextView) rootView.findViewById(R.id.my_last_name);
		tmpString = newUser.getLastName();
		if (tmpString != null)
			name.setText(tmpString);
		name = (TextView) rootView.findViewById(R.id.my_company);
		tmpString = newUser.getCompany();
		if (tmpString != null)
			name.setText(tmpString);
		name = (TextView) rootView.findViewById(R.id.my_job_title);
		tmpString = newUser.getTitle();
		if (tmpString != null)
			name.setText(tmpString);

	}
}
