package com.warpspace.ecardv4;

import java.util.Timer;
import java.util.TimerTask;

import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.warpspace.ecardv4.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.widget.Toast;

public class ActivityBufferOpening extends Activity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
    if (getActionBar() != null) {
      getActionBar().hide();
    }
    setContentView(R.layout.activity_buffer_opening);

    // isFirstTimeLoginOnThisDevice();

    // the animation becomes laggy when object.save() is occupying the main
    // thread
    // ImageView imageCover = (ImageView)findViewById(R.id.imageCover);
    // Animation animation1 =
    // AnimationUtils.loadAnimation(getApplicationContext(), R.anim.zoomin);
    // imageCover.startAnimation(animation1);

    // Create/refresh local copy every time app opens
    ParseUser currentUser = ParseUser.getCurrentUser();
    createLocalSelfCopy(currentUser);
    timerToJump();
  }

  public void timerToJump() {
    int timeout = 1000;
    // make the activity visible for 3 seconds before transitioning
    // This is important when first time sign up or login on this device
    // allows time to create local self copy

    Timer timer = new Timer();
    timer.schedule(new TimerTask() {

      @Override
      public void run() {
        Intent intent = new Intent(getBaseContext(), ActivityMain.class);
        startActivity(intent);
      }
    }, timeout);

    int timeout1 = timeout + 500;
    timer.schedule(new TimerTask() {

      @Override
      public void run() {
        finish();
        // have to delayed finishing, so desktop don't show
      }
    }, timeout1);
  }

  public void createLocalSelfCopy(ParseUser currentUser) {
    // Refresh local copies of records upon login EACH TIME app opens

    ParseQuery<ParseObject> query = ParseQuery.getQuery("ECardInfo");
    query.getInBackground(currentUser.get("ecardId").toString(),
      new GetCallback<ParseObject>() {

        @Override
        public void done(ParseObject object, ParseException e) {
          if (e == null && object != null) {
            // ParseFile portraitFile = (ParseFile) object.get("portrait");
            // if(portraitFile ==null){
            // // if the portrait is empty, create dummy one
            // putBlankPortrait(object);
            // object.saveInBackground();
            // }
            // ParseFile QRcodeFile = (ParseFile) object.get("qrCode");
            // if(QRcodeFile ==null){
            // // if the portrait is empty, create dummy one
            // createQRCode(object);
            // object.saveInBackground();
            // }
            object.pinInBackground();
            Toast.makeText(getBaseContext(), "Local copy created!",
              Toast.LENGTH_SHORT).show();
          } else {
            // If no internet connection, no local copy can be saved
            Log.d("BufferOpening", "Cannot save self copy");
          }
        }
      });
  }
}
