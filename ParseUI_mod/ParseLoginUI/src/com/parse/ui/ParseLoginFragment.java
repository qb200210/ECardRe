/*
 *  Copyright (c) 2014, Facebook, Inc. All rights reserved.
 *
 *  You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
 *  copy, modify, and distribute this software in source code or binary form for use
 *  in connection with the web services and APIs provided by Facebook.
 *
 *  As with any software that integrates with the Facebook platform, your use of
 *  this software is subject to the Facebook Developer Principles and Policies
 *  [http://developers.facebook.com/policy/]. This copyright notice shall be
 *  included in all copies or substantial portions of the software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 *  FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 *  COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 *  IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 *  CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package com.parse.ui;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.util.Base64;


import com.linkedin.platform.LISession;
import com.linkedin.platform.LISessionManager;
import com.linkedin.platform.utils.Scope;
import com.linkedin.platform.APIHelper;
import com.linkedin.platform.errors.LIApiError;
import com.linkedin.platform.errors.LIAuthError;
import com.linkedin.platform.listeners.ApiListener;
import com.linkedin.platform.listeners.ApiResponse;
import com.linkedin.platform.listeners.AuthListener;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseTwitterUtils;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.parse.twitter.Twitter;


/**
 * Fragment for the user login screen.
 */
public class ParseLoginFragment extends ParseLoginFragmentBase {

  public interface ParseLoginFragmentListener {
    public void onSignUpClicked(String username, String password);

    public void onLoginHelpClicked();

    public void onLoginSuccess();
  }

  private static final String LOG_TAG = "ParseLoginFragment";
  private static final String USER_OBJECT_NAME_FIELD = "name";
  
  public static final String PACKAGE_MOBILE_ECARD_APP = "com.micklestudios.knowell";
  private static final String host = "api.linkedin.com";
  private static final String topCardUrl = "https://" + host + "/v1/people/~:(first-name,last-name,positions,email-address,location,picture-url)";


  private View parseLogin;
  private EditText usernameField;
  private EditText passwordField;
  private TextView parseLoginHelpButton;
  private Button parseLoginButton;
  private Button parseSignupButton;
  private Button facebookLoginButton;
  private Button twitterLoginButton;
  private Button linkedinLoginButton;
  private ParseLoginFragmentListener loginFragmentListener;
  private ParseOnLoginSuccessListener onLoginSuccessListener;

  private ParseLoginConfig config;

  public static ParseLoginFragment newInstance(Bundle configOptions) {
    ParseLoginFragment loginFragment = new ParseLoginFragment();
    loginFragment.setArguments(configOptions);
    return loginFragment;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup parent,
                           Bundle savedInstanceState) {
    config = ParseLoginConfig.fromBundle(getArguments(), getActivity());

    View v = inflater.inflate(R.layout.com_parse_ui_parse_login_fragment,
        parent, false);
    ImageView appLogo = (ImageView) v.findViewById(R.id.app_logo);
    parseLogin = v.findViewById(R.id.parse_login);
    usernameField = (EditText) v.findViewById(R.id.login_username_input);
    passwordField = (EditText) v.findViewById(R.id.login_password_input);
    parseLoginHelpButton = (Button) v.findViewById(R.id.parse_login_help);
    parseLoginButton = (Button) v.findViewById(R.id.parse_login_button);
    parseSignupButton = (Button) v.findViewById(R.id.parse_signup_button);
    facebookLoginButton = (Button) v.findViewById(R.id.facebook_login);
    twitterLoginButton = (Button) v.findViewById(R.id.twitter_login);
    linkedinLoginButton = (Button) v.findViewById(R.id.linkedin_login);
    

    if (appLogo != null && config.getAppLogo() != null) {
      appLogo.setImageResource(config.getAppLogo());
    }
    if (allowParseLoginAndSignup()) {
      setUpParseLoginAndSignup();
    }
    if (allowTwitterLogin()) {
      setUpTwitterLogin();
    }
    //linkedin login
    setupLinkedinLogin();
    
    return v;
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);

    if (activity instanceof ParseLoginFragmentListener) {
      loginFragmentListener = (ParseLoginFragmentListener) activity;
    } else {
      throw new IllegalArgumentException(
          "Activity must implemement ParseLoginFragmentListener");
    }

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
  protected String getLogTag() {
    return LOG_TAG;
  }

  private void setUpParseLoginAndSignup() {
    parseLogin.setVisibility(View.VISIBLE);

    if (config.isParseLoginEmailAsUsername()) {
      usernameField.setHint(R.string.com_parse_ui_email_input_hint);
      usernameField.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
    }

    if (config.getParseLoginButtonText() != null) {
      parseLoginButton.setText(config.getParseLoginButtonText());
    }

    parseLoginButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        String username = usernameField.getText().toString();
        String password = passwordField.getText().toString();

        if (username.length() == 0) {
          showToast(R.string.com_parse_ui_no_username_toast);
        } else if (password.length() == 0) {
          showToast(R.string.com_parse_ui_no_password_toast);
        } else {
          loadingStart(true);
          ParseUser.logInInBackground(username, password, new LogInCallback() {
            @Override
            public void done(ParseUser user, ParseException e) {
              if (isActivityDestroyed()) {
                return;
              }

              if (user != null) {
                loadingFinish();
                loginSuccess();
              } else {
                loadingFinish();
                if (e != null) {
                  debugLog(getString(R.string.com_parse_ui_login_warning_parse_login_failed) +
                      e.toString());
                  if (e.getCode() == ParseException.OBJECT_NOT_FOUND) {
                    if (config.getParseLoginInvalidCredentialsToastText() != null) {
                      showToast(config.getParseLoginInvalidCredentialsToastText());
                    } else {
                      showToast(R.string.com_parse_ui_parse_login_invalid_credentials_toast);
                    }
                    passwordField.selectAll();
                    passwordField.requestFocus();
                  } else {
                    showToast(R.string.com_parse_ui_parse_login_failed_unknown_toast);
                  }
                }
              }
            }
          });
        }
      }
    });

    if (config.getParseSignupButtonText() != null) {
      parseSignupButton.setText(config.getParseSignupButtonText());
    }

    parseSignupButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        String username = usernameField.getText().toString();
        String password = passwordField.getText().toString();

        loginFragmentListener.onSignUpClicked(username, password);
      }
    });

    if (config.getParseLoginHelpText() != null) {
      parseLoginHelpButton.setText(config.getParseLoginHelpText());
    }

    parseLoginHelpButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        loginFragmentListener.onLoginHelpClicked();
      }
    });
  }

  

  private void setUpTwitterLogin() {
    twitterLoginButton.setVisibility(View.VISIBLE);

    if (config.getTwitterLoginButtonText() != null) {
      twitterLoginButton.setText(config.getTwitterLoginButtonText());
    }

    twitterLoginButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        loadingStart(false); // Twitter login pop-up already has a spinner
        ParseTwitterUtils.logIn(getActivity(), new LogInCallback() {
          @Override
          public void done(ParseUser user, ParseException e) {
            if (isActivityDestroyed()) {
              return;
            }

            if (user == null) {
              loadingFinish();
              if (e != null) {
                showToast(R.string.com_parse_ui_twitter_login_failed_toast);
                debugLog(getString(R.string.com_parse_ui_login_warning_twitter_login_failed) +
                    e.toString());
              }
            } else if (user.isNew()) {
              Twitter twitterUser = ParseTwitterUtils.getTwitter();
              if (twitterUser != null
                  && twitterUser.getScreenName().length() > 0) {
                /*
                  To keep this example simple, we put the users' Twitter screen name
                  into the name field of the Parse user object. If you want the user's
                  real name instead, you can implement additional calls to the
                  Twitter API to fetch it.
                */
                user.put(USER_OBJECT_NAME_FIELD, twitterUser.getScreenName());
                user.saveInBackground(new SaveCallback() {
                  @Override
                  public void done(ParseException e) {
                    if (e != null) {
                      debugLog(getString(
                          R.string.com_parse_ui_login_warning_twitter_login_user_update_failed) +
                          e.toString());
                    }
                    loginSuccess();
                  }
                });
              }
            } else {
              loginSuccess();
            }
          }
        });
      }
    });
  }

  
  private void setupLinkedinLogin() {
	  
	  	setUpdateState();
	    //linkedinLoginButton.setVisibility(View.VISIBLE);
	    linkedinLoginButton.setOnClickListener(new OnClickListener() {
	      @Override
	      public void onClick(View v) {
	        loadingStart(false); // Twitter login pop-up already has a spinner
        	Activity thisActivity = getActivity();
            LISessionManager.getInstance(thisActivity.getApplicationContext()).init(thisActivity, buildScope(), new AuthListener() {
                @Override
                public void onAuthSuccess() {
                	Log.v("sccess", "before");
                    setUpdateState();
                    Log.v("sccess", "after");
                    /*-----
                    APIHelper apiHelper = APIHelper.getInstance(getActivity().getParent().getApplicationContext());
                    apiHelper.getRequest(getActivity().getParent(), topCardUrl, new ApiListener() {
                        @Override
                        public void onApiSuccess(ApiResponse s) {
                            Log.v("fetched string", s.toString());
                            Toast.makeText(getActivity().getParent().getApplicationContext(), "clicked", Toast.LENGTH_LONG).show();
                        }

                        @Override
                        public void onApiError(LIApiError error) {
                            Log.v("error string", error.toString());
                            Toast.makeText(getActivity().getParent().getApplicationContext(), error.toString(), Toast.LENGTH_LONG).show();
                        }
                    });
                    --*/
                    Toast.makeText(getActivity().getApplicationContext(), "success", Toast.LENGTH_LONG).show();
                }
                @Override
                public void onAuthError(LIAuthError error) {
                	Log.v("error string", "before");
                    setUpdateState();
                    Log.v("error string", error.toString());
                    Toast.makeText(getActivity().getApplicationContext(), "failed " + error.toString(), Toast.LENGTH_LONG).show();
                }
            }, true); 
            
           
            try {
                PackageInfo info = thisActivity.getPackageManager().getPackageInfo(
                        PACKAGE_MOBILE_ECARD_APP,
                        PackageManager.GET_SIGNATURES);
                for (Signature signature : info.signatures) {
                    MessageDigest md = MessageDigest.getInstance("SHA");
                    md.update(signature.toByteArray());

                    Log.v("packagename", info.packageName);
                    Log.v("encode", Base64.encodeToString(md.digest(), Base64.NO_WRAP));
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.d(LOG_TAG, e.getMessage(), e);
            } catch (NoSuchAlgorithmException e) {
                Log.d(LOG_TAG, e.getMessage(), e);
            }
          
	        //Toast.makeText(getActivity().getApplicationContext(), "clicked", Toast.LENGTH_LONG).show();
        	Log.v("event click", "clicked");

	      }
  });
	    
  }

  private void setUpdateState() {
      LISessionManager sessionManager = LISessionManager.getInstance(getActivity().getApplicationContext());
      LISession session = sessionManager.getSession();
      boolean accessTokenValid = session.isValid();
      if (accessTokenValid){
    	  Log.d(LOG_TAG, "valid");
      }
      else{
    	  Log.d(LOG_TAG, "invalid");
      }
      
  }

  private static Scope buildScope() {
      return Scope.build(Scope.R_BASICPROFILE, Scope.W_SHARE);
  }
  
  private boolean allowParseLoginAndSignup() {
    if (!config.isParseLoginEnabled()) {
      return false;
    }

    if (usernameField == null) {
      debugLog(R.string.com_parse_ui_login_warning_layout_missing_username_field);
    }
    if (passwordField == null) {
      debugLog(R.string.com_parse_ui_login_warning_layout_missing_password_field);
    }
    if (parseLoginButton == null) {
      debugLog(R.string.com_parse_ui_login_warning_layout_missing_login_button);
    }
    if (parseSignupButton == null) {
      debugLog(R.string.com_parse_ui_login_warning_layout_missing_signup_button);
    }
    if (parseLoginHelpButton == null) {
      debugLog(R.string.com_parse_ui_login_warning_layout_missing_login_help_button);
    }

    boolean result = (usernameField != null) && (passwordField != null)
        && (parseLoginButton != null) && (parseSignupButton != null)
        && (parseLoginHelpButton != null);

    if (!result) {
      debugLog(R.string.com_parse_ui_login_warning_disabled_username_password_login);
    }
    return result;
  }

  private boolean allowFacebookLogin() {
    if (!config.isFacebookLoginEnabled()) {
      return false;
    }

    if (facebookLoginButton == null) {
      debugLog(R.string.com_parse_ui_login_warning_disabled_facebook_login);
      return false;
    } else {
      return true;
    }
  }

  private boolean allowTwitterLogin() {
    if (!config.isTwitterLoginEnabled()) {
      return false;
    }

    if (twitterLoginButton == null) {
      debugLog(R.string.com_parse_ui_login_warning_disabled_twitter_login);
      return false;
    } else {
      return true;
    }
  }

  private void loginSuccess() {
    onLoginSuccessListener.onLoginSuccess();
  }

}
