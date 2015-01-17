package com.warpspace.ecardv4;

import com.parse.ParseUser;
import com.warpspace.ecardv4.infrastructure.UserInfo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;

public class ActivityPostScanBuffer extends Activity {

	@Override
	public void onCreate(Bundle SavedInstances) {
		super.onCreate(SavedInstances);
	}

	@Override
	protected void onStart() {
		// The purpose of this activity is to pre-grab ecardInfo from Parse and store them in UserInfo
		// Then when it gets passed to ActivityScanned, the info can be displayed immediately
		// If pulling ecardInfo inside ActivityScanned, then there will be 1-3 sec lag before actual info will show
		super.onStart();

		// Udayan ::: placeholder to be hooked with QRscanner activity
		// These parameters should be passed from scanned string -- do you want error tolerance here or inside UserInfo?
		String scannedID = "CRuumzPcTN";
		String firstName = "Jack";
		String lastName = "Rose";

		// Create new userInfo class based on the scannedId. Will pull info from Parse
		// Will be hanging if cannot pull from Parse, since this is not done in background thread
		UserInfo newUser = new UserInfo(this, scannedID, firstName, lastName, false, isNetworkAvailable());

		Intent intent = new Intent(getBaseContext(), ActivityScanned.class);
		// passing UserInfo is made possible through Parcelable
		intent.putExtra("userinfo", newUser);
		startActivity(intent);
		finish();
	}

	private boolean isNetworkAvailable() {
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
		return activeNetworkInfo != null && activeNetworkInfo.isConnected();
	}
}
