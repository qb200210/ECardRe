package com.warpspace.ecardv4.utils;

import java.util.HashMap;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

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

    ActionBar actionBar = getActionBar();
    // actionBar.show();
  }

  @Override
  public void handleDecode(Result rawResult, Bitmap barcode, float scaleFactor) {
    super.handleDecode(rawResult, barcode, scaleFactor);

    HashMap<String, String> valuesMap = ECardUtils.parseQRString(
      getApplicationContext(), rawResult.toString());

    if (valuesMap == null) {
      Toast.makeText(this, "UInfo is null", Toast.LENGTH_SHORT).show();
      return;
    } else {
      Toast.makeText(this, valuesMap.get("ln"), Toast.LENGTH_SHORT).show();
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
