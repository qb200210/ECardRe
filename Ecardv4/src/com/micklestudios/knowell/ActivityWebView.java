package com.micklestudios.knowell;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class ActivityWebView extends ActionBarActivity {
  private WebView myWebView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.layout_webview);
    
    showActionBar();
    
    Bundle b = getIntent().getExtras();
    String url = b.get("url").toString();
    TextView myTextView = (TextView) findViewById(R.id.simpletext);
    myWebView = (WebView) findViewById(R.id.webview);
    myWebView.setWebViewClient(new WebViewClient());
    WebSettings webSettings = myWebView.getSettings();
    webSettings.setJavaScriptEnabled(true);
    webSettings.setBuiltInZoomControls(true);
    webSettings.setSupportZoom(true);
    
    if (!url.startsWith("http://") && !url.startsWith("https://")
      && !url.startsWith("ftp://")) {
      myTextView.setVisibility(View.VISIBLE);
      myWebView.setVisibility(View.GONE);
      myTextView.setText(url);
    } else {
      myWebView.loadUrl(url);
    }
  }
  
  @SuppressLint("InflateParams")
  private void showActionBar() {
    LayoutInflater inflator = (LayoutInflater) this
      .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    View v = inflator.inflate(R.layout.layout_actionbar_search, null);
    LinearLayout btnBack = (LinearLayout) v.findViewById(R.id.btn_back);
    TextView title = (TextView) v.findViewById(R.id.search_actionbar_title);
    title.setText("QR Content");
    btnBack.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {
        onBackPressed();
      }

    });
    if (getSupportActionBar() != null) {
      ActionBar actionBar = getSupportActionBar();
      actionBar.setDisplayHomeAsUpEnabled(false);
      actionBar.setDisplayShowHomeEnabled(false);
      actionBar.setDisplayShowCustomEnabled(true);
      actionBar.setDisplayShowTitleEnabled(false);
      actionBar.setCustomView(v);
    }
  }
  
  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
      // Check if the key event was the Back button and if there's history
      if ((keyCode == KeyEvent.KEYCODE_BACK) && myWebView.canGoBack()) {
          myWebView.goBack();
          return true;
      }
      // If it wasn't the Back key or there's no web page history, bubble up to the default
      // system behavior (probably exit the activity)
      return super.onKeyDown(keyCode, event);
  }
}
