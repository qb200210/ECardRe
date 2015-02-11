package com.warpspace.ecardv4;

import com.parse.ParseUser;
import com.warpspace.ecardv4.infrastructure.UserInfo;
import com.warpspace.ecardv4.utils.UpdateableFragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class FragmentMaincard extends Fragment implements UpdateableFragment {

  private static final String ARG_SECTION_NUMBER = "section_number";
  private static UserInfo myself;

  public static FragmentMaincard newInstance(int sectionNumber, UserInfo myselfUserInfo) {
	
	myself = myselfUserInfo;
	Log.i("fragmain", myself.getFirstName());
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
	  
	if(getArguments().getInt(ARG_SECTION_NUMBER, 1) == 1){
		// if this is to create maincard fragment
		rootView = inflater.inflate(R.layout.fragment_maincard, container,
			      false);
			    
	    // display the main card
		displayCard(rootView, myself);
	    setHasOptionsMenu(true);
	    return rootView;
	} 
	if(getArguments().getInt(ARG_SECTION_NUMBER, 1) == 2){
		View rootView = inflater.inflate(R.layout.fragment_qr, container, false);
	    ImageView qrCode = (ImageView) rootView.findViewById(R.id.qr_container);
	    qrCode.setImageBitmap(myself.getQRCode());    
	    setHasOptionsMenu(true);
	    return rootView;
	}
	return null;
    
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
		ImageView portraitImg = (ImageView) rootView.findViewById(R.id.my_portrait);
		portraitImg.setImageBitmap(newUser.getPortrait());

	}

	@Override
	public void update(UserInfo userInfo) {
		if(getArguments().getInt(ARG_SECTION_NUMBER, 1) == 1){
	      // update the main card
		  displayCard(rootView, userInfo);
		}
		if(getArguments().getInt(ARG_SECTION_NUMBER, 1) == 2){
			// For unknown reason, trying to rootView.findViewById(anything) will crash
			// Luckily QR code doesn't need to change noticeably. Next time app restarts
			// Changes in QR code can be reflected
		}
		
	}
}
