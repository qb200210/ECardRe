package com.warpspace.ecardv4.utils;

import java.util.HashMap;
import java.util.StringTokenizer;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.warpspace.ecardv4.R;

public class ECardUtils {
  public static HashMap<String, String> parseQRString(Context context,
    String qrString) {
    /*
     * The string is of the format:
     * http://ecard.parseapp.com/search?id=<obj-id>&fn=<fname>&ln=<lname>
     */

    String website = context.getString(R.string.base_website_user);

    int index = qrString.indexOf(website);

    // We require the website to be in the beginning. Otherwise,
    // the string is invalid.
    if (index != 0) {
      return null;
    }

    String restOfString = qrString.substring(website.length());

    HashMap<String, String> valuesMap = new HashMap<String, String>();

    StringTokenizer st = new StringTokenizer(restOfString, "&");
    while (st.hasMoreTokens()) {
      String thisToken = st.nextToken();
      StringTokenizer st2 = new StringTokenizer(thisToken, "=");

      valuesMap.put(st2.nextToken(), st2.nextToken());
    }

    return valuesMap;
  }

  public static boolean isNetworkAvailable(Activity activity) {
    ConnectivityManager connectivityManager = (ConnectivityManager) activity
      .getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
    return activeNetworkInfo != null && activeNetworkInfo.isConnected();
  }
}
