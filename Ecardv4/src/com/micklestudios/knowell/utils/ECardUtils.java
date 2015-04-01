package com.micklestudios.knowell.utils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.StringTokenizer;

import org.apache.http.client.methods.HttpGet;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.micklestudios.knowell.ActivityMain;
import com.micklestudios.knowell.R;

public class ECardUtils {

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
