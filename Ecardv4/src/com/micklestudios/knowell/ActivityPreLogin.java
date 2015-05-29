package com.micklestudios.knowell;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;

import com.parse.ParseAnalytics;
import com.parse.ParseFile;
import com.parse.ParseUser;
import com.parse.ui.ParseLoginActivity;
import com.parse.ui.ParseLoginBuilder;
import com.micklestudios.knowell.R;

import android.widget.Button;
import android.view.View;
import android.view.View.OnClickListener;

import android.widget.Toast;
import android.util.Log;

import com.linkedin.platform.APIHelper;
import com.linkedin.platform.LISession;
import com.linkedin.platform.LISessionManager;
import com.linkedin.platform.errors.LIApiError;
import com.linkedin.platform.errors.LIAuthError;
import com.linkedin.platform.listeners.ApiListener;
import com.linkedin.platform.listeners.ApiResponse;
import com.linkedin.platform.listeners.AuthListener;
import com.linkedin.platform.utils.Scope;


public class ActivityPreLogin extends Activity {
  private static final int LOGIN_REQUEST = 0;
  ParseFile file = null;
  ParseUser currentUser;
  
  @Override
  public void onCreate(Bundle SavedInstances) {
    super.onCreate(SavedInstances);
    //setContentView(R.layout.com_parse_ui_parse_login_third_party_section);
    
    ParseAnalytics.trackAppOpened(getIntent());
       
    //addListenerOnButton();
  }
    
  @Override
  protected void onStart() {
    super.onStart();

    currentUser = ParseUser.getCurrentUser();
    
    if (currentUser != null && currentUser.get("ecardId") != null) {
      // If already logged in, skip this check
      Intent intent = new Intent(getBaseContext(), ActivityBufferOpening.class);
      startActivity(intent);
      finish();
    } else {
      // If not loggin in, show Parse Login page
      ParseLoginBuilder loginBuilder = new ParseLoginBuilder(
        ActivityPreLogin.this);
      Intent parseLoginIntent = loginBuilder
        .setParseLoginEnabled(true)
        .setAppLogo(R.drawable.splash)
        .setParseLoginButtonText("Login")
        .setParseSignupButtonText("Register")
        .setParseLoginHelpText("Forgot password?")
        .setParseLoginInvalidCredentialsToastText(
          "You email and/or password is not correct")
        .setParseLoginEmailAsUsername(true)
        .setParseSignupSubmitButtonText("Submit registration")
        .setFacebookLoginEnabled(false).setTwitterLoginEnabled(false).build();
      
      startActivityForResult(loginBuilder.build(), LOGIN_REQUEST);
      // If successful, onStart will be called and there will be currentUser
      // If aborted, it will return to PreLoginActivity, which will be exited as
      // done below
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == LOGIN_REQUEST
      && resultCode != ParseLoginActivity.RESULT_CANCELED) {
      // Login or sign up successful
      currentUser = ParseUser.getCurrentUser();
      Intent intent = new Intent(getBaseContext(), ActivityBufferOpening.class);
      startActivity(intent);
      finish();
    } else {
      // Login or Sign up aborted, should quit the PreLoginActivity, so pressing
      // back in ParseLoginUI will not return to this page
      finish();
    }
  }
}
