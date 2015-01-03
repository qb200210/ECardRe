package com.warpspace.ecardv4.utils;

import android.app.ActionBar;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.google.zxing.client.android.CaptureActivity;
import com.warpspace.ecardv4.R;

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
}
