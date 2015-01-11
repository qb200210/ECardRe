package com.warpspace.ecardv4.utils;

import java.util.HashMap;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.zxing.Result;
import com.google.zxing.client.android.CaptureActivity;
import com.warpspace.ecardv4.ActivityScanned;
import com.warpspace.ecardv4.R;
import com.warpspace.ecardv4.infrastructure.UserInfo;

public class CustomQRScanner extends CaptureActivity {
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
    ImageView progress_image = (ImageView) dialog
      .findViewById(R.id.process_dialog_image);
    progress_image.setBackgroundResource(R.drawable.progress);

    // Start with the Processing status.
    dialog_text.setText("Processing");

    dialog.show();

    HashMap<String, String> valuesMap = ECardUtils.parseQRString(
      getApplicationContext(), rawResult.toString());

    if (valuesMap == null) {
      dialog_text.setText("The scanned QR is invalid");
      progress_image.setBackgroundResource(R.drawable.ic_action_cancel);

      onPause();
      dialog.show();
      onResume();

      // Hide after some seconds
      final Handler handler = new Handler();
      final Runnable runnable = new Runnable() {
        @Override
        public void run() {
          if (dialog.isShowing()) {
            dialog.dismiss();
          }
        }
      };

      handler.postDelayed(runnable, 2000);
    } else {
      dialog_text.setText("Successfully Identified QR Code. Processing...");
      progress_image.setBackgroundResource(R.drawable.ic_action_done);
    }

    String scannedID = valuesMap.get("id");
    String firstName = valuesMap.get("fn");
    String lastName = valuesMap.get("ln");

    // Create new userInfo class based on the scannedId. Will pull info from
    // Parse
    // Will be hanging if cannot pull from Parse, since this is not done in
    // background thread
    UserInfo newUser = new UserInfo(this, scannedID, firstName, lastName,
      ECardUtils.isNetworkAvailable(this));

    Intent intent = new Intent(getBaseContext(), ActivityScanned.class);
    // passing UserInfo is made possible through Parcelable
    intent.putExtra("userinfo", newUser);
    startActivity(intent);
    finish();
  }
}
