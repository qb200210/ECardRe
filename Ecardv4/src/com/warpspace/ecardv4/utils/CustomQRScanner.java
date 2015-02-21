package com.warpspace.ecardv4.utils;

import java.util.HashMap;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.Result;
import com.google.zxing.client.android.CaptureActivity;
import com.warpspace.ecardv4.ActivityScanned;
import com.warpspace.ecardv4.R;
import com.warpspace.ecardv4.infrastructure.UserInfo;

public class CustomQRScanner extends CaptureActivity {
	private static final long SCAN_TIMEOUT = 5000;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_qrscanner);

		Button scan_return = (Button) findViewById(R.id.btn_scanner_exit);
		scan_return.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				finish();
			}
		});
	}

	@Override
	public void handleDecode(Result rawResult, Bitmap barcode, float scaleFactor) {
		super.handleDecode(rawResult, barcode, scaleFactor);
		// Create an instance of the dialog fragment and show it
		final Dialog dialog = new Dialog(this);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(R.layout.layout_dialog_scanned_process);

		TextView dialog_text = (TextView) dialog.findViewById(R.id.dialog_status);
		ImageView progress_image = (ImageView) dialog.findViewById(R.id.process_dialog_image);

		HashMap<String, String> valuesMap = ECardUtils.parseQRString(getApplicationContext(), rawResult.toString());

		if (valuesMap == null) {
			dialog_text.setText("The scanned QR is invalid");
			progress_image.setBackgroundResource(R.drawable.ic_action_cancel);

			dialog.show();

			// Hide after some seconds
			final Handler handler = new Handler();
			final Runnable runnable = new Runnable() {
				@Override
				public void run() {
					if (dialog.isShowing()) {
						dialog.dismiss();
					}

					onPause();
					onResume();
				}
			};

			handler.postDelayed(runnable, 2000);
		} else {
			// dialog_text.setText("Successfully Identified QR Code. Processing...");
			// progress_image.setBackgroundResource(R.drawable.ic_action_done);

			final String scannedId = valuesMap.get("id");
			final String firstName = valuesMap.get("fn");
			final String lastName = valuesMap.get("ln");

			// Will be hanging if cannot pull from Parse, since this is not done in
			// background thread

			// add new card asynchronically
			if (ECardUtils.isNetworkAvailable(this)) {
				final SyncDataTaskScanQR scanQR = new SyncDataTaskScanQR(this, scannedId, firstName, lastName);
				scanQR.execute();
				final Runnable myCancellable = new Runnable() {

					@Override
					public void run() {
						if (scanQR.getStatus() == AsyncTask.Status.RUNNING) {
							Toast.makeText(getApplicationContext(), "Add New Card Timed Out", Toast.LENGTH_SHORT).show();
							// network poor, turn to offline mode for card collection
							UserInfo newUser = new UserInfo(getBaseContext(), scannedId, firstName, lastName, false, false, false);
							// upon failed network, dismiss dialog
							if (dialog.isShowing()) {
								dialog.dismiss();
							}
							Intent intent = new Intent(getBaseContext(), ActivityScanned.class);
							// passing UserInfo is made possible through Parcelable
							intent.putExtra("userinfo", newUser);
							startActivity(intent);
							scanQR.cancel(true);
							finish();
						}
					}
				};
				final Handler handlerScanQR = new Handler();
				handlerScanQR.postDelayed(myCancellable, SCAN_TIMEOUT);

				// upon back button press, cancel both the scanQR AsyncTask and the timed handler
				progress_image.setBackgroundResource(R.drawable.progress);
				dialog_text.setText("Processing");
				dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {
						Toast.makeText(getApplicationContext(), "canceled", Toast.LENGTH_SHORT).show();
						scanQR.cancel(true);
						handlerScanQR.removeCallbacks(myCancellable);
						onPause();
						onResume();
					}
				});
				dialog.show();
			} else {
				// no network, directly switch to offline card collection mode
				UserInfo newUser = new UserInfo(getBaseContext(), scannedId, firstName, lastName, false, false, false);
				if (dialog.isShowing()) {
					dialog.dismiss();
				}
				Toast.makeText(getApplicationContext(), "Network unavailable", Toast.LENGTH_SHORT).show();

				Intent intent = new Intent(getBaseContext(), ActivityScanned.class);
				// passing UserInfo is made possible through Parcelable
				intent.putExtra("userinfo", newUser);
				startActivity(intent);
				finish();
			}

		}
	}

	private class SyncDataTaskScanQR extends AsyncTask<String, Void, UserInfo> {

		private Context context;
		private String scannedId;
		private String firstName;
		private String lastName;

		public SyncDataTaskScanQR(Context context, String scannedId, String firstName, String lastName) {
			this.context = context;
			this.scannedId = scannedId;
			this.firstName = firstName;
			this.lastName = lastName;

		}

		@Override
		protected UserInfo doInBackground(String... url) {
			// Create new userInfo class based on the scannedId.
			// Will pull info from Parse
			UserInfo newUser = new UserInfo(context, scannedId, firstName, lastName, false, true, false);
			return newUser;
		}

		@Override
		protected void onPostExecute(UserInfo newUser) {
			// upon successful scan and pull of info
			Intent intent = new Intent(getBaseContext(), ActivityScanned.class);
			// passing UserInfo is made possible through Parcelable
			intent.putExtra("userinfo", newUser);
			startActivity(intent);
			finish();
		}

	}
}
