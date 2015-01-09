/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxing.client.android;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.Result;
import com.google.zxing.ResultMetadataType;
import com.google.zxing.client.android.camera.CameraManager;
import com.google.zxing.client.android.history.HistoryActivity;
import com.google.zxing.client.android.history.HistoryItem;
import com.google.zxing.client.android.history.HistoryManager;
import com.google.zxing.client.android.share.ShareActivity;

import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.xmlpull.v1.XmlPullParserException;

/**
 * This activity opens the camera and does the actual scanning on a background
 * thread. It draws a viewfinder to help the user place the barcode correctly,
 * shows feedback as the image processing is happening, and then overlays the
 * results when a scan is successful.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 * @author Sean Owen
 */
public class CaptureActivity extends ActionBarActivity implements SurfaceHolder.Callback {

  private static final String TAG = CaptureActivity.class.getSimpleName();

  private static final String PACKAGE_NAME = "com.google.zxing.client.android";
  private static final String PRODUCT_SEARCH_URL_PREFIX = "http://www.google";
  private static final String PRODUCT_SEARCH_URL_SUFFIX = "/m/products/scan";
  private static final String[] ZXING_URLS = { "http://zxing.appspot.com/scan",
    "zxing://scan/" };

  public static final int HISTORY_REQUEST_CODE = 0x0000bacc;

  private static final Set<ResultMetadataType> DISPLAYABLE_METADATA_TYPES = EnumSet
    .of(ResultMetadataType.ISSUE_NUMBER, ResultMetadataType.SUGGESTED_PRICE,
      ResultMetadataType.ERROR_CORRECTION_LEVEL,
      ResultMetadataType.POSSIBLE_COUNTRY);

  private static final float VIEWFINDER_SIZE = 250;
  // Viewfinder height=width= xx dp

  private CameraManager cameraManager;
  private CaptureActivityHandler handler;
  private Result savedResultToShow;
  private ViewfinderView viewfinderView;
  private TextView statusView;
  private View resultView;
  private Result lastResult;
  private boolean hasSurface;
  private boolean copyToClipboard;
  private IntentSource source;
  private ScanFromWebPageManager scanFromWebPageManager;
  private Collection<BarcodeFormat> decodeFormats;
  private Map<DecodeHintType, ?> decodeHints;
  private String characterSet;
  private HistoryManager historyManager;
  private InactivityTimer inactivityTimer;
  private BeepManager beepManager;
  private AmbientLightManager ambientLightManager;

  ViewfinderView getViewfinderView() {
    return viewfinderView;
  }

  public Handler getHandler() {
    return handler;
  }

  CameraManager getCameraManager() {
    return cameraManager;
  }

  @Override
  protected void onCreate(Bundle icicle) {
    super.onCreate(icicle);

    Window window = getWindow();
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    setContentView(R.layout.capture);

    hasSurface = false;
    historyManager = new HistoryManager(this);
    historyManager.trimHistory();
    inactivityTimer = new InactivityTimer(this);
    beepManager = new BeepManager(this);
    ambientLightManager = new AmbientLightManager(this);

    PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

    showHelpOnFirstLaunch();

    ActionBar actionBar = getSupportActionBar();
    Resources res = getResources();
    try {
      actionBar.setBackgroundDrawable(Drawable.createFromXml(res, res.getXml(R.drawable.striped_background)));
    } catch (NotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (XmlPullParserException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @Override
  protected void onResume() {
    super.onResume();

    // CameraManager must be initialized here, not in onCreate(). This is
    // necessary because we don't
    // want to open the camera driver and measure the screen size if we're going
    // to show the help on
    // first launch. That led to bugs where the scanning rectangle was the wrong
    // size and partially
    // off screen.
    cameraManager = new CameraManager(getApplication());

    // QB: Here the size of the viewFinder is set
    Resources r = getResources();
    int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
      VIEWFINDER_SIZE, r.getDisplayMetrics());
    cameraManager.setManualFramingRect(px, px);

    viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
    viewfinderView.setCameraManager(cameraManager);

    resultView = findViewById(R.id.result_view);
    statusView = (TextView) findViewById(R.id.status_view);

    handler = null;
    lastResult = null;

    resetStatusView();

    SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
    SurfaceHolder surfaceHolder = surfaceView.getHolder();
    if (hasSurface) {
      // The activity was paused but not stopped, so the surface still exists.
      // Therefore
      // surfaceCreated() won't be called, so init the camera here.
      initCamera(surfaceHolder);
    } else {
      // Install the callback and wait for surfaceCreated() to init the camera.
      surfaceHolder.addCallback(this);
      surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    beepManager.updatePrefs();
    ambientLightManager.start(cameraManager);

    inactivityTimer.onResume();

    Intent intent = getIntent();

    SharedPreferences prefs = PreferenceManager
      .getDefaultSharedPreferences(this);
    copyToClipboard = prefs.getBoolean(
      PreferencesActivity.KEY_COPY_TO_CLIPBOARD, true)
      && (intent == null || intent.getBooleanExtra(Intents.Scan.SAVE_HISTORY,
        true));

    source = IntentSource.NONE;
    decodeFormats = null;
    characterSet = null;

    if (intent != null) {

      String action = intent.getAction();
      String dataString = intent.getDataString();

      if (Intents.Scan.ACTION.equals(action)) {

        // Scan the formats the intent requested, and return the result to the
        // calling activity.
        source = IntentSource.NATIVE_APP_INTENT;
        decodeFormats = DecodeFormatManager.parseDecodeFormats(intent);
        decodeHints = DecodeHintManager.parseDecodeHints(intent);

        if (intent.hasExtra(Intents.Scan.WIDTH)
          && intent.hasExtra(Intents.Scan.HEIGHT)) {
          int width = intent.getIntExtra(Intents.Scan.WIDTH, 0);
          int height = intent.getIntExtra(Intents.Scan.HEIGHT, 0);
          if (width > 0 && height > 0) {
            cameraManager.setManualFramingRect(width, height);
          }
        }

        String customPromptMessage = intent
          .getStringExtra(Intents.Scan.PROMPT_MESSAGE);
        if (customPromptMessage != null) {
          statusView.setText(customPromptMessage);
        }

      } else if (dataString != null
        && dataString.contains(PRODUCT_SEARCH_URL_PREFIX)
        && dataString.contains(PRODUCT_SEARCH_URL_SUFFIX)) {

        // Scan only products and send the result to mobile Product Search.
        source = IntentSource.PRODUCT_SEARCH_LINK;
        decodeFormats = DecodeFormatManager.PRODUCT_FORMATS;

      } else if (isZXingURL(dataString)) {

        // Scan formats requested in query string (all formats if none
        // specified).
        // If a return URL is specified, send the results there. Otherwise,
        // handle it ourselves.
        source = IntentSource.ZXING_LINK;
        Uri inputUri = Uri.parse(dataString);
        scanFromWebPageManager = new ScanFromWebPageManager(inputUri);
        decodeFormats = DecodeFormatManager.parseDecodeFormats(inputUri);
        // Allow a sub-set of the hints to be specified by the caller.
        decodeHints = DecodeHintManager.parseDecodeHints(inputUri);

      }

      characterSet = intent.getStringExtra(Intents.Scan.CHARACTER_SET);

    }
  }

  private static boolean isZXingURL(String dataString) {
    if (dataString == null) {
      return false;
    }
    for (String url : ZXING_URLS) {
      if (dataString.startsWith(url)) {
        return true;
      }
    }
    return false;
  }

  @Override
  protected void onPause() {
    if (handler != null) {
      handler.quitSynchronously();
      handler = null;
    }
    inactivityTimer.onPause();
    ambientLightManager.stop();
    cameraManager.closeDriver();
    if (!hasSurface) {
      SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
      SurfaceHolder surfaceHolder = surfaceView.getHolder();
      surfaceHolder.removeCallback(this);
    }
    super.onPause();
  }

  @Override
  protected void onDestroy() {
    inactivityTimer.shutdown();
    super.onDestroy();
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    switch (keyCode) {
    case KeyEvent.KEYCODE_BACK:
      if (source == IntentSource.NATIVE_APP_INTENT) {
        setResult(RESULT_CANCELED);
        finish();
        return true;
      }
      if ((source == IntentSource.NONE || source == IntentSource.ZXING_LINK)
        && lastResult != null) {
        restartPreviewAfterDelay(0L);
        return true;
      }
      break;
    case KeyEvent.KEYCODE_FOCUS:
    case KeyEvent.KEYCODE_CAMERA:
      // Handle these events so they don't launch the Camera app
      return true;
      // Use volume up/down to turn on light
    case KeyEvent.KEYCODE_VOLUME_DOWN:
      cameraManager.setTorch(false);
      return true;
    case KeyEvent.KEYCODE_VOLUME_UP:
      cameraManager.setTorch(true);
      return true;
    }
    return super.onKeyDown(keyCode, event);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.scanner_actionbar, menu);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    Intent intent = new Intent(Intent.ACTION_VIEW);
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
    int itemId = item.getItemId();
    if (itemId == R.id.menu_share) {
      intent.setClassName(this, ShareActivity.class.getName());
      startActivity(intent);
    } else if (itemId == R.id.menu_history) {
      intent.setClassName(this, HistoryActivity.class.getName());
      startActivityForResult(intent, HISTORY_REQUEST_CODE);
    } else if (itemId == R.id.menu_settings) {
      intent.setClassName(this, PreferencesActivity.class.getName());
      startActivity(intent);
    } else if (itemId == R.id.menu_help) {
      intent.setClassName(this, HelpActivity.class.getName());
      startActivity(intent);
    } else {
      return super.onOptionsItemSelected(item);
    }
    return true;
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent intent) {
    if (resultCode == RESULT_OK) {
      if (requestCode == HISTORY_REQUEST_CODE) {
        int itemNumber = intent.getIntExtra(Intents.History.ITEM_NUMBER, -1);
        if (itemNumber >= 0) {
          HistoryItem historyItem = historyManager.buildHistoryItem(itemNumber);
          decodeOrStoreSavedBitmap(null, historyItem.getResult());
        }
      }
    }
  }

  private void decodeOrStoreSavedBitmap(Bitmap bitmap, Result result) {
    // Bitmap isn't used yet -- will be used soon
    if (handler == null) {
      savedResultToShow = result;
    } else {
      if (result != null) {
        savedResultToShow = result;
      }
      if (savedResultToShow != null) {
        Message message = Message.obtain(handler, R.id.decode_succeeded,
          savedResultToShow);
        handler.sendMessage(message);
      }
      savedResultToShow = null;
    }
  }

  @Override
  public void surfaceCreated(SurfaceHolder holder) {
    if (holder == null) {
      Log.e(TAG, "*** WARNING *** surfaceCreated() gave us a null surface!");
    }
    if (!hasSurface) {
      hasSurface = true;
      initCamera(holder);
    }
  }

  @Override
  public void surfaceDestroyed(SurfaceHolder holder) {
    hasSurface = false;
  }

  @Override
  public void surfaceChanged(SurfaceHolder holder, int format, int width,
    int height) {

  }

  /**
   * A valid barcode has been found, so give an indication of success and show
   * the results.
   *
   * @param rawResult
   *          The contents of the barcode.
   * @param scaleFactor
   *          amount by which thumbnail was scaled
   * @param barcode
   *          A greyscale bitmap of the camera data which was decoded.
   */
  public void handleDecode(Result rawResult, Bitmap barcode, float scaleFactor) {
    inactivityTimer.onActivity();
    viewfinderView.drawResultBitmap(barcode);
    beepManager.playBeepSoundAndVibrate();
    Toast.makeText(getBaseContext(),
      rawResult.getBarcodeFormat().toString() + ":" + rawResult.getText(),
      Toast.LENGTH_SHORT).show();
    inactivityTimer.onActivity();
    lastResult = rawResult;
  }

  /**
   * We want the help screen to be shown automatically the first time a new
   * version of the app is run. The easiest way to do this is to check
   * android:versionCode from the manifest, and compare it to a value stored as
   * a preference.
   */
  private boolean showHelpOnFirstLaunch() {
    try {
      PackageInfo info = getPackageManager().getPackageInfo(PACKAGE_NAME, 0);
      int currentVersion = info.versionCode;
      SharedPreferences prefs = PreferenceManager
        .getDefaultSharedPreferences(this);
      int lastVersion = prefs.getInt(
        PreferencesActivity.KEY_HELP_VERSION_SHOWN, 0);
      if (currentVersion > lastVersion) {
        prefs.edit()
          .putInt(PreferencesActivity.KEY_HELP_VERSION_SHOWN, currentVersion)
          .commit();
        Intent intent = new Intent(this, HelpActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        // Show the default page on a clean install, and the what's new page on
        // an upgrade.
        String page = lastVersion == 0 ? HelpActivity.DEFAULT_PAGE
          : HelpActivity.WHATS_NEW_PAGE;
        intent.putExtra(HelpActivity.REQUESTED_PAGE_KEY, page);
        startActivity(intent);
        return true;
      }
    } catch (PackageManager.NameNotFoundException e) {
      Log.w(TAG, e);
    }
    return false;
  }

  private void initCamera(SurfaceHolder surfaceHolder) {
    if (surfaceHolder == null) {
      throw new IllegalStateException("No SurfaceHolder provided");
    }
    if (cameraManager.isOpen()) {
      Log.w(TAG,
        "initCamera() while already open -- late SurfaceView callback?");
      return;
    }
    try {
      cameraManager.openDriver(surfaceHolder);
      // Creating the handler starts the preview, which can also throw a
      // RuntimeException.
      if (handler == null) {
        handler = new CaptureActivityHandler(this, decodeFormats, decodeHints,
          characterSet, cameraManager);
      }
      decodeOrStoreSavedBitmap(null, null);
    } catch (IOException ioe) {
      Log.w(TAG, ioe);
      displayFrameworkBugMessageAndExit();
    } catch (RuntimeException e) {
      // Barcode Scanner has seen crashes in the wild of this variety:
      // java.?lang.?RuntimeException: Fail to connect to camera service
      Log.w(TAG, "Unexpected error initializing camera", e);
      displayFrameworkBugMessageAndExit();
    }
  }

  private void displayFrameworkBugMessageAndExit() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle(getString(R.string.app_name));
    builder.setMessage(getString(R.string.msg_camera_framework_bug));
    builder.setPositiveButton(R.string.button_ok, new FinishListener(this));
    builder.setOnCancelListener(new FinishListener(this));
    builder.show();
  }

  public void restartPreviewAfterDelay(long delayMS) {
    if (handler != null) {
      handler.sendEmptyMessageDelayed(R.id.restart_preview, delayMS);
    }
    resetStatusView();
  }

  private void resetStatusView() {
    // resultView.setVisibility(View.GONE);
    // statusView.setText(R.string.msg_default_status);
    // statusView.setVisibility(View.VISIBLE);
    viewfinderView.setVisibility(View.VISIBLE);
    lastResult = null;
  }

  public void drawViewfinder() {
    viewfinderView.drawViewfinder();
  }
}
