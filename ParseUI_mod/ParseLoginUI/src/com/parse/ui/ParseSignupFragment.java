package com.parse.ui;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.android.Contents;
import com.parse.ParseACL;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.parse.SignUpCallback;

/**
 * Fragment for the user signup screen.
 */
public class ParseSignupFragment extends ParseLoginFragmentBase implements OnClickListener {
  public static final String USERNAME = "com.parse.ui.ParseSignupFragment.USERNAME";
  public static final String PASSWORD = "com.parse.ui.ParseSignupFragment.PASSWORD";

  private EditText usernameField;
  private EditText passwordField;
  private EditText confirmPasswordField;
  private EditText emailField;
  private EditText nameField;
  private Button createAccountButton;
  private ParseOnLoginSuccessListener onLoginSuccessListener;

  private ParseLoginConfig config;
  private int minPasswordLength;

  private static final String LOG_TAG = "ParseSignupFragment";
  private static final int DEFAULT_MIN_PASSWORD_LENGTH = 6;
  private static final String USER_OBJECT_NAME_FIELD = "name";
  protected static final String MY_PREFS_NAME = "KnoWellSyncParams";

  public static ParseSignupFragment newInstance(Bundle configOptions, String username, String password) {
    ParseSignupFragment signupFragment = new ParseSignupFragment();
    Bundle args = new Bundle(configOptions);
    args.putString(ParseSignupFragment.USERNAME, username);
    args.putString(ParseSignupFragment.PASSWORD, password);
    signupFragment.setArguments(args);
    return signupFragment;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup parent,
                           Bundle savedInstanceState) {

    Bundle args = getArguments();
    config = ParseLoginConfig.fromBundle(args, getActivity());

    minPasswordLength = DEFAULT_MIN_PASSWORD_LENGTH;
    if (config.getParseSignupMinPasswordLength() != null) {
      minPasswordLength = config.getParseSignupMinPasswordLength();
    }

    String username = (String) args.getString(USERNAME);
    String password = (String) args.getString(PASSWORD);

    View v = inflater.inflate(R.layout.com_parse_ui_parse_signup_fragment,
        parent, false);
    ImageView appLogo = (ImageView) v.findViewById(R.id.app_logo);
    usernameField = (EditText) v.findViewById(R.id.signup_username_input);
    passwordField = (EditText) v.findViewById(R.id.signup_password_input);
    confirmPasswordField = (EditText) v
        .findViewById(R.id.signup_confirm_password_input);
    emailField = (EditText) v.findViewById(R.id.signup_email_input);
    // nameField = (EditText) v.findViewById(R.id.signup_name_input);
    createAccountButton = (Button) v.findViewById(R.id.create_account);

    usernameField.setText(username);
    passwordField.setText(password);

    if (appLogo != null && config.getAppLogo() != null) {
      appLogo.setImageResource(config.getAppLogo());
    }

    if (config.isParseLoginEmailAsUsername()) {
      usernameField.setHint(R.string.com_parse_ui_email_input_hint);
      usernameField.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
      if (emailField != null) {
        emailField.setVisibility(View.GONE);
      }
    }

    if (config.getParseSignupSubmitButtonText() != null) {
      createAccountButton.setText(config.getParseSignupSubmitButtonText());
    }
    createAccountButton.setOnClickListener(this);

    return v;
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    if (activity instanceof ParseOnLoginSuccessListener) {
      onLoginSuccessListener = (ParseOnLoginSuccessListener) activity;
    } else {
      throw new IllegalArgumentException(
          "Activity must implemement ParseOnLoginSuccessListener");
    }

    if (activity instanceof ParseOnLoadingListener) {
      onLoadingListener = (ParseOnLoadingListener) activity;
    } else {
      throw new IllegalArgumentException(
          "Activity must implemement ParseOnLoadingListener");
    }
  }

  @Override
  public void onClick(View v) {
    String username = usernameField.getText().toString();
    String password = passwordField.getText().toString();
    String passwordAgain = confirmPasswordField.getText().toString();

    String email = null;
    if (config.isParseLoginEmailAsUsername()) {
      email = usernameField.getText().toString();
    } else if (emailField != null) {
      email = emailField.getText().toString();
    }

    String name = null;
    if (nameField != null) {
      name = nameField.getText().toString();
    }

    if (username.length() == 0) {
      showToast(R.string.com_parse_ui_no_username_toast);
    } else if (password.length() == 0) {
      showToast(R.string.com_parse_ui_no_password_toast);
    } else if (password.length() < minPasswordLength) {
      showToast(getResources().getQuantityString(
          R.plurals.com_parse_ui_password_too_short_toast,
          minPasswordLength, minPasswordLength));
    } else if (passwordAgain.length() == 0) {
      showToast(R.string.com_parse_ui_reenter_password_toast);
    } else if (!password.equals(passwordAgain)) {
      showToast(R.string.com_parse_ui_mismatch_confirm_password_toast);
      confirmPasswordField.selectAll();
      confirmPasswordField.requestFocus();
    } else if (email != null && email.length() == 0) {
      showToast(R.string.com_parse_ui_no_email_toast);
    } else if (name != null && name.length() == 0) {
      showToast(R.string.com_parse_ui_no_name_toast);
    } else {
      ParseUser user = new ParseUser();
      // Set standard fields
      user.setUsername(username);
      user.setPassword(password);
      user.setEmail(email);

      // Set additional custom fields only if the user filled it out
      if (name != null && name.length() != 0) {
        user.put(USER_OBJECT_NAME_FIELD, name);
      }

      loadingStart();
      user.signUpInBackground(new SignUpCallback() {

        @Override
        public void done(ParseException e) {
          if (isActivityDestroyed()) {
            return;
          }

          if (e == null) {
            loadingFinish();
            // setting security: as of now parse has bug
            ParseUser currentUser = ParseUser.getCurrentUser();
            ParseACL userACL = new ParseACL(currentUser);
            userACL.setPublicReadAccess(false);
            userACL.setPublicWriteAccess(false);
            currentUser.setACL(userACL);
            
            // creating EcardInfo object, QR and portrait
            initializeMyCard(currentUser);
            
            signupSuccess();
          } else {
            loadingFinish();
            if (e != null) {
              debugLog(getString(R.string.com_parse_ui_login_warning_parse_signup_failed) +
                  e.toString());
              switch (e.getCode()) {
                case ParseException.INVALID_EMAIL_ADDRESS:
                  showToast(R.string.com_parse_ui_invalid_email_toast);
                  break;
                case ParseException.USERNAME_TAKEN:
                  showToast(R.string.com_parse_ui_username_taken_toast);
                  break;
                case ParseException.EMAIL_TAKEN:
                  showToast(R.string.com_parse_ui_email_taken_toast);
                  break;
                default:
                  showToast(R.string.com_parse_ui_signup_failed_unknown_toast);
              }
            }
          }
        }
      });
    }
  }
  
  private void initializeMyCard(ParseUser currentUser) {
    // clean up defaults for lastSynced time:
    SharedPreferences prefs = getActivity().getSharedPreferences(MY_PREFS_NAME,
      Context.MODE_PRIVATE);
    SharedPreferences.Editor prefEditor = prefs.edit();
    Date currentDate=new Date(0);
    prefEditor.putLong("DateConversationsSynced", currentDate.getTime());
    prefEditor.putLong("DateNoteSynced", currentDate.getTime());
    prefEditor.putLong("DateSelfSynced", currentDate.getTime());
    prefEditor.commit();
	  ParseObject object = new ParseObject("ECardInfo");
		// objectId is only created after the object is saved.
		// If use saveInBackground, .getObjectId gets nothing since object not saved yet
		try {
			object.save();
			Log.d("ParseSignUp","save EcardInfo successful");
		} catch (ParseException e) {
			e.printStackTrace();
		}
		currentUser.put("ecardId", object.getObjectId());
//		// get first and last name then upload
//		String fullName = (String) currentUser.get("name");		
//		String delims = "[ ,]";
//		String firstName= "";
//		String lastName= "";
//		String[] tokens = fullName.split(delims);
//		Integer size = tokens.length;
//		Integer i =0;
//		if(size == 1){
//			firstName = tokens[0];
//		} else{
//			firstName = tokens[0];
//			for(i=1;i< size-1; i++){
//				firstName = firstName + " " + tokens[i];
//			}
//			lastName = tokens[size -1];
//		}
//		object.put("firstName", firstName);
//		object.put("lastName", lastName);
//		object.put("fullName", fullName.toLowerCase(Locale.ENGLISH));
		// initialize portrait with blank one
		putBlankPortrait(object);
		object.put("userId", currentUser.getObjectId().toString());
		// createQRCode(object); // the EcardInfo and QR code both created
		object.saveInBackground();
		// If new on the server, should not have exist locally. So should make a local copy
		object.pinInBackground();
		currentUser.saveInBackground();
	}

	public void putBlankPortrait(ParseObject object) {
		FileOutputStream out = null;
		byte[] imgData;
		ParseFile file = null;
		Bitmap blankProfile = BitmapFactory.decodeResource(getResources(), R.drawable.emptyprofile);
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {        	        	
        	blankProfile.compress(Bitmap.CompressFormat.PNG, 100, stream);
            imgData = stream.toByteArray();         
            file = new ParseFile("portrait.jpg", imgData);
            try {
				file.save();
			} catch (ParseException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
        } catch (Exception ee) {
            ee.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException ee) {
                ee.printStackTrace();
            }
        }		
		object.put("portrait", file);
	}

  @Override
  protected String getLogTag() {
    return LOG_TAG;
  }

  private void signupSuccess() { 
    onLoginSuccessListener.onLoginSuccess();
  }
}
