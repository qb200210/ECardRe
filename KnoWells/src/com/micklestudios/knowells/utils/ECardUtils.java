package com.micklestudios.knowells.utils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.ImageView;

import com.micklestudios.knowells.R;
import com.micklestudios.knowells.ActivityMain;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;

public class ECardUtils {

  private static boolean flagTemplateNull;

  public static boolean isValidEmail(String email) {
    String tokens[] = email.split("@");
    if (tokens.length != 2 || tokens[0].length() == 0
      || tokens[1].length() == 0)
      return false;
    tokens = tokens[1].split("\\.");
    if (tokens.length != 2 || tokens[0].length() == 0
      || tokens[1].length() == 0)
      return false;
    return true;
  }

  public static Long getNetworkLatency(Context context) {
    String host = "http://www.google.com";
    int timeOut = 2000;
    long[] time = new long[5];
    Boolean reachable = false;

    for (int i = 0; i < 5; i++) {
      long BeforeTime = System.currentTimeMillis();
      try {
        reachable = InetAddress.getByName(host).isReachable(timeOut);
      } catch (UnknownHostException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      long AfterTime = System.currentTimeMillis();
      Long TimeDifference = AfterTime - BeforeTime;
      time[i] = TimeDifference;
    }
    return null;

  }

  public static HashMap<String, String> parseQRString(String qrString) {
    /*
     * The string is of the format:
     * http://ecard.parseapp.com/search?id=<obj-id>&fn=<fname>&ln=<lname>
     */

    String website = ActivityMain.applicationContext
      .getString(R.string.base_website_user);

    int index = qrString.indexOf(website);

    // We require the website to be in the beginning. Otherwise,
    // the string is invalid. Open the website or display text
    if (index != 0) {
      return null;
    }

    String restOfString = qrString.substring(website.length());

    HashMap<String, String> valuesMap = new HashMap<String, String>();

    StringTokenizer st = new StringTokenizer(restOfString, "&");
    while (st.hasMoreTokens()) {
      String thisToken = st.nextToken();
      StringTokenizer st2 = new StringTokenizer(thisToken, "=");
      String keyString = null;
      String valueString = null;
      if (st2.hasMoreTokens()) {
        keyString = st2.nextToken();
      }
      if (st2.hasMoreTokens()) {
        valueString = st2.nextToken();
      }
      if (keyString != null && valueString != null) {
        valuesMap.put(keyString, valueString);
      }
    }

    // If the string is apparently ours but somehow messed up, should not
    // proceed displaying
    if (valuesMap.size() == 0) {
      valuesMap.put("wrong", "wrong");
      return valuesMap;
    }

    return valuesMap;
  }

  public static boolean isNetworkAvailable(Activity activity) {
    ConnectivityManager connectivityManager = (ConnectivityManager) activity
      .getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
    return activeNetworkInfo != null && activeNetworkInfo.isConnected();
  }

  public static void findAndSetLogo(Activity activity, final ImageView logoImg,
    String companyName, boolean checkOnline) {
    Log.i("find", companyName);
    ParseQuery<ParseObject> queryLocal = ParseQuery.getQuery("ECardTemplate");
    queryLocal.whereEqualTo("companyNameLC", companyName.toLowerCase(Locale.ENGLISH).trim());
    queryLocal.fromLocalDatastore();
    List<ParseObject> listTemplateObjectsLocal = null;
    try {
      listTemplateObjectsLocal = queryLocal.find();
    } catch (ParseException e2) {
      e2.printStackTrace();
    }
    if (listTemplateObjectsLocal != null
      && listTemplateObjectsLocal.size() != 0) {
      // from activityDesign save, there will be a new ecardtemplate with null obj. This is to skip it
      // tricky here. If there is a to-be-saved ecardtemplate, the list will not be null, but there is no real object yet
      flagTemplateNull = true;
      for(ParseObject obj: listTemplateObjectsLocal){
        if(obj.getObjectId()== null) continue;
        // at least one local valid ecardtemplate is found. Use it
        flagTemplateNull = false;
        ParseFile logoFile = (ParseFile) obj.get(
          "companyLogo");
        Log.i("found", companyName);
        if (logoFile != null && logoFile.isDataAvailable()) {
          byte[] data;
          try {
            data = logoFile.getData();
            if (data != null) {
              Bitmap logo = BitmapFactory.decodeByteArray(data, 0, data.length);
              logoImg.setImageBitmap(logo);
            } else {
              logoImg.setImageResource(R.drawable.emptylogo);
            }
          } catch (ParseException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
          }
        }
      }
      if(flagTemplateNull){
        // if all local ecardtemplates are not valid, go check online
        getTemplateFromParse(activity, companyName, logoImg);
      }
    } else {
      if (checkOnline) {
        // no found from localDataStore, need to pull from Parse
        getTemplateFromParse(activity, companyName, logoImg);
      }
    }
  }

  private static void getTemplateFromParse(Activity activity, String companyName, final ImageView logoImg) {
    if (isNetworkAvailable(activity)) {
      ParseQuery<ParseObject> queryOnline = ParseQuery
        .getQuery("ECardTemplate");
      queryOnline.whereEqualTo("companyNameLC", companyName.toLowerCase(Locale.ENGLISH).trim());
      queryOnline.findInBackground(new FindCallback<ParseObject>() {

        @Override
        public void done(List<ParseObject> objects, ParseException e) {
          if (e == null) {
            if (objects != null && objects.size() != 0) {
              ParseFile logoFile = (ParseFile) objects.get(0).get(
                "companyLogo");
              if (logoFile != null) {
                byte[] data;
                try {
                  data = logoFile.getData();
                  if (data != null) {
                    // sloppy: whatever found object, pin it to local
                    objects.get(0).pinInBackground();
                    Bitmap logo = BitmapFactory.decodeByteArray(data, 0,
                      data.length);
                    logoImg.setImageBitmap(logo);
                    Log.i("found2", objects.get(0).get("companyName")
                      .toString());
                  } else {
                    logoImg.setImageResource(R.drawable.emptylogo);
                  }
                } catch (ParseException e1) {
                  // TODO Auto-generated catch block
                  e1.printStackTrace();
                }
              }
            } else {
              logoImg.setImageResource(R.drawable.emptylogo);
            }
          } else {
            e.printStackTrace();
          }
        }
      });
    } else {
      // logo not found locally and network not available, flush it to be
      // empty
      logoImg.setImageResource(R.drawable.emptylogo);
    }
  }
}
