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

import java.util.Date;
import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
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
import android.content.Intent;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.parse.LogInCallback;
import com.parse.ParseACL;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseTwitterUtils;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.parse.SignUpCallback;
import com.parse.twitter.Twitter;

import org.json.*;



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
  public static int exception_flag = 1;
  private static String g_json_str = "";
  
  public static final String PACKAGE_MOBILE_ECARD_APP = "com.micklestudios.knowell";
  private static final String host = "api.linkedin.com";
  private static final String topCardUrl = "https://" + host + "/v1/people/~:(id,first-name,last-name,positions,location,picture-url,email-address)";
  private ParseObject object = null;
	

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
  protected static final String MY_PREFS_NAME = "KnoWellSyncParams";

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
                // clean up defaults for lastSynced time:
                SharedPreferences prefs = getActivity().getSharedPreferences(MY_PREFS_NAME,
                  Context.MODE_PRIVATE);
                SharedPreferences.Editor prefEditor = prefs.edit();
                Date currentDate=new Date(0);
                prefEditor.putLong("DateConversationsSynced", currentDate.getTime());
                prefEditor.putLong("DateNoteSynced", currentDate.getTime());
                prefEditor.putLong("DateSelfSynced", currentDate.getTime());
                prefEditor.commit();
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
                	setUpdateState();
                    
                    APIHelper apiHelper = APIHelper.getInstance(getActivity().getApplicationContext());
                    apiHelper.getRequest(getActivity(), topCardUrl, new ApiListener() {
                        @Override
                        public void onApiSuccess(ApiResponse s) {
                            try {
                            	Log.v("response", s.toString());
                            	JSONObject responseData = s.getResponseDataAsJson();
                            	
                            	String firstName = responseData.getString("firstName");
								String lastName = responseData.getString("lastName");
								String picURL = responseData.getString("pictureUrl");
								String location = responseData.getJSONObject("location").getString("name");
								String linkedin_id = responseData.getString("id");
								//String emailAddress = responseData.getString("emailAddress");
								//Log.v("email: ", emailAddress);
								
								JSONArray companyArray = responseData.getJSONObject("positions").getJSONArray("values");
								//Log.v("json String", firstName + " " + lastName + " " + picURL);
								for (int i = 0; i < companyArray.length(); i++){
									JSONObject company = companyArray.getJSONObject(i).getJSONObject("company");
									if ((Boolean) companyArray.getJSONObject(i).get("isCurrent")){
										String companyName = company.getString("name");
										String title = companyArray.getJSONObject(i).getString("title");				
										//Log.v("json String", firstName + " " + lastName + " " + companyName + " " + title);						
								        
										ParseUser user = new ParseUser();
										String sysGenUserName = firstName + lastName + linkedin_id;
										user.setUsername(sysGenUserName);
										//String password = Integer.toString(linkedin_id.hashCode());
										String password = linkedin_id + "jsdj32RIfd28UFaf2";
									    user.setPassword(password);
									    //user.setEmail(null);

									    g_json_str = firstName + "," + lastName + "," + companyName + "," + title + "," + location + "," + picURL;
										user.put(USER_OBJECT_NAME_FIELD, firstName + " " + lastName);
										
									    user.signUpInBackground(new SignUpCallback() {
											  @Override
									          public void done(ParseException e) {

									            if (e == null) {
										          if (isActivityDestroyed()) {
											             return;
											      }
										          
									              loadingFinish();
									              // setting security: as of now parse has bug
									              ParseUser currentUser = ParseUser.getCurrentUser();
									              ParseACL userACL = new ParseACL(currentUser);
									              userACL.setPublicReadAccess(false);
									              userACL.setPublicWriteAccess(false);
									              currentUser.setACL(userACL);
									              
									              // creating EcardInfo object, QR and portrait
									              //Log.v("before", "initialization");
									              initializeMyCard(currentUser);
									            
									              //Log.v("after", "initialization");
									              exception_flag = 0;
									              onLoginSuccessListener.onLoginSuccess();
									            }
									            else {
										    		  if (!isAdded()){
										    			  return;
										    		  }

									            	debugLog(getString(R.string.com_parse_ui_login_warning_parse_signup_failed) +
									                        e.toString());
													//Log.v("here exception",  Integer.toString(e.getCode()));
													exception_flag = 1;
												}
											  }
									    });
									    
									    if (exception_flag == 1) {
									    		  loadingFinish();									    		  
									    		  try{
									    			  ParseUser.logIn(user.getUsername(), password);
									    			  onLoginSuccessListener.onLoginSuccess();
									    		  }
									    		  catch(ParseException e){
									    			  Log.v("exception",  "login exception");
									    		  }
									              //onLoginSuccessListener.onLoginSuccess();
									           }
									    }
									    break;
								}			
							} catch (JSONException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
                        }

                        @Override
                        public void onApiError(LIApiError error) {
                        	Activity activity = getActivity();
                        	if (activity != null && isAdded()) {
                        		//Log.v("error string", error.toString());
                                Toast.makeText(getActivity().getApplicationContext(), error.toString(), Toast.LENGTH_LONG).show();    	
                        	}
                        }
                    });
                    
                    Toast.makeText(getActivity().getApplicationContext(), "Login Successfully, Please Wait..", Toast.LENGTH_LONG).show();
                }
                
                @Override
                public void onAuthError(LIAuthError error) {
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

                    Log.d("packagename", info.packageName);
                    Log.d("encode", Base64.encodeToString(md.digest(), Base64.NO_WRAP));
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.d(LOG_TAG, e.getMessage(), e);
            } catch (NoSuchAlgorithmException e) {
                Log.d(LOG_TAG, e.getMessage(), e);
            }
          
	        //Toast.makeText(getActivity().getApplicationContext(), "clicked", Toast.LENGTH_LONG).show();
        	//Log.v("event click", "clicked");

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
		object = new ParseObject("ECardInfo");
			// objectId is only created after the object is saved.
			// If use saveInBackground, .getObjectId gets nothing since object not saved yet
			try {
				object.put("userId", currentUser.getObjectId());
				object.save();
				Log.d("ParseSignUp","save EcardInfo successful");
			} catch (ParseException e) {
				e.printStackTrace();
			}
			currentUser.put("ecardId", object.getObjectId());
			// get first and last name then upload
			//String fullName = (String) currentUser.get("name");		
			String delims = ",";
			String[] tokens = g_json_str.split(delims);
			String picURL = tokens[6];
			Log.v("here", "before retrieve URL");
			// initialize portrait with blank one
			//putBlankPortrait(object);
			putPortrait(picURL);
			// createQRCode(object); // the EcardInfo and QR code both created
			currentUser.saveInBackground();
		}
  
    
	public void putPortrait(String imgURL) {
		new DownloadImageTask().execute(imgURL);
		
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

  private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
		byte[] imgData;
		ParseFile file = null;
			
		@Override
		protected Bitmap doInBackground(String... urls) {
			// TODO Auto-generated method stub
			String imgURL = urls[0];
			try{
				URL url = new URL(imgURL);
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				connection.setDoInput(true);
				Log.v("connect", "before");
				connection.connect();
				Log.v("connect", "after");
				Bitmap profileImage = BitmapFactory.decodeStream(connection.getInputStream());
				Log.v("connect", "get the image");
				return profileImage;
			} catch (Exception e0){
				e0.printStackTrace();
				Bitmap blankProfile = BitmapFactory.decodeResource(getResources(), R.drawable.emptyprofile);
				return blankProfile;
			} 
		}
		
		@Override
		protected void onPostExecute(Bitmap results){
			FileOutputStream out = null;		
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			Log.v("post exe", "portrait file done");
	        try {        	        	
	        	results.compress(Bitmap.CompressFormat.PNG, 100, stream);
	            imgData = stream.toByteArray();         
	            Log.v("compress", "portrait file converted");
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
			String delims = ",";
			String firstName= "";
			String lastName= "";
			String[] tokens = g_json_str.split(delims);
			firstName = tokens[0];
			lastName = tokens[1];
			String fullName = firstName + " " + lastName;
			String company = tokens[2];
			String title = tokens[3];		
			String location = tokens[4];
			
			// restore the name field
			object.put("firstName", firstName);
			object.put("lastName", lastName);
			object.put("fullName", fullName.toLowerCase(Locale.ENGLISH));
			object.put("company", company);
			object.put("title", title);
			object.put("city", location);
			object.put("linkedin", fullName);
			object.put("portrait", file);
			Log.v("here", "portrait file done");
			/*
			try {
				object.save();
				Log.d("update object","save EcardInfo successful");
			} catch (ParseException e) {
				e.printStackTrace();
			}
			*/
			object.saveInBackground();
			// If new on the server, should not have exist locally. So should make a local copy
			object.pinInBackground();
		}
	}
  
}

