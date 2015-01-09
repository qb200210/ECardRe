package com.warpspace.ecardv4.infrastructure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.StringTokenizer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.warpspace.ecardv4.R;
import com.warpspace.ecardv4.utils.ECardUtils;

/**
 * Created by Udayan on 12/13/2014.
 */
public class UserInfo implements Parcelable {

  // Centralized space for ecard, should make a similar one for ecardNote
  // This is made Parcelable so a UserInfo object can be passed among activities

  String objId;
  String firstName;
  String lastName;
  String company;
  String title;
  Context context;

  ArrayList<String> shownArrayList = new ArrayList<String>();
  ArrayList<Integer> infoIcon = new ArrayList<Integer>();
  ArrayList<String> infoLink = new ArrayList<String>();
  String[] allowedArray = { "about", "linkedin", "phone", "email", "facebook",
    "twitter", "googleplus", "web" };

  public UserInfo(Context context, String objId, String firstName,
    String lastName, boolean networkAvailable) {
    this.context = context;
    this.objId = objId;
    this.firstName = firstName;
    this.lastName = lastName;
    if (networkAvailable) {
      populateUserInfoWithParseData(objId);
    }
  }

  public UserInfo(Parcel source) {
    this.objId = source.readString();
    this.firstName = source.readString();
    this.lastName = source.readString();
    this.company = source.readString();
    this.title = source.readString();
    source.readStringList(this.shownArrayList);
    source.readStringList(this.infoLink);
    source.readList(this.infoIcon, Integer.class.getClassLoader());
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(objId);
    dest.writeString(firstName);
    dest.writeString(lastName);
    dest.writeString(company);
    dest.writeString(title);
    dest.writeStringList(shownArrayList);
    dest.writeStringList(infoLink);
    dest.writeList(infoIcon);
  }

  private void populateUserInfoWithParseData(String objId) {
    ParseQuery<ParseObject> query = ParseQuery.getQuery("ECardInfo");
    try {
      // Must not use the getinbackground() thread method,
      // otherwise data won't be back before the UserInfo object is built
      // may want to implement a loading screen if taking too long?
      ParseObject object = query.get(objId);
      if (object != null) {
        // main card info
        firstName = object.getString("firstName");
        lastName = object.getString("lastName");
        company = object.getString("company");
        title = object.getString("title");
        // extra info
        infoIcon.clear();
        infoLink.clear();
        shownArrayList.clear();
        for (int i = 0; i < allowedArray.length; i++) {
          // the extra info item
          String item = allowedArray[i];
          // the value of this extra info item
          Object value = object.get(item);
          if (value != null && value.toString() != "") {
            infoIcon.add(iconSelector(item));
            infoLink.add(value.toString());
            // note down the existing extra info items
            shownArrayList.add(item);
          }
        }
      }
    } catch (ParseException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }
  }

  private Integer iconSelector(String key) {
    // input key to select corresponding icon to display on button
    switch (key) {
    case "email":
      return R.drawable.mail;
    case "facebook":
      return R.drawable.facebook;
    case "linkedin":
      return R.drawable.linkedin;
    case "twitter":
      return R.drawable.twitter;
    case "phone":
      return R.drawable.phone;
    case "about":
      return R.drawable.me;
    case "googleplus":
      return R.drawable.googleplus;
    case "web":
      return R.drawable.web;
    default:
      return R.drawable.ic_action_discard;
    }
  }

  /**
   * Writes the given Matrix on a new Bitmap object.
   *
   * @param matrix
   *          the matrix to write.
   * @return the new {@link Bitmap}-object.
   */
  private static Bitmap toBitmap(BitMatrix matrix) {
    int height = matrix.getHeight();
    int width = matrix.getWidth();
    Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        bmp.setPixel(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
      }
    }
    return bmp;
  }

  public static Bitmap getQRCode(Context context, String objId,
    String firstName, String lastName) {
    String website = context.getString(R.string.base_website_user);
    StringBuffer qrString = new StringBuffer(website);
    qrString.append("=");
    qrString.append(objId);
    qrString.append(".");
    qrString.append(firstName);
    qrString.append(".");
    qrString.append(lastName);

    QRCodeWriter writer = new QRCodeWriter();
    BitMatrix matrix = null;
    try {
      matrix = writer.encode(qrString.toString(), BarcodeFormat.QR_CODE, 400,
        400);
    } catch (WriterException e) {
      e.printStackTrace();
    }

    return toBitmap(matrix);
  }

  public static UserInfo getUserInfoFromQRString(Context context,
    String qrString) {

    HashMap<String, String> valuesMap = ECardUtils.parseQRString(context,
      qrString);

    // If the valuesMap is null, the string is ill-formed.
    if (valuesMap == null) {
      return null;
    }

    // Let's fetch these values now
    String id = valuesMap.get("id");
    String fname = valuesMap.get("fn");
    String lname = valuesMap.get("ln");

    Log.e("Got string", id + " " + fname + " " + lname);

    // Udayan:::
    // error tolerant: 1. if input string isn't ecard link, 2. if input objectId
    // doesn't exist
    // 3. if input objectId already collected, 4. if no network
    return new UserInfo(context, id, fname, lname, true);
  }

  public Bitmap getQRCode() {
    return getQRCode(context, objId, firstName, lastName);
  }

  @Override
  public int describeContents() {
    // TODO Auto-generated method stub
    return 0;
  }

  public String getObjId() {
    return objId;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public String getCompany() {
    return company;
  }

  public String getTitle() {
    return title;
  }

  public ArrayList<String> getShownArrayList() {
    return shownArrayList;
  }

  public ArrayList<Integer> getInfoIcon() {
    return infoIcon;
  }

  public ArrayList<String> getInfoLink() {
    return infoLink;
  }

  public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {

    @Override
    public UserInfo createFromParcel(Parcel source) {
      return new UserInfo(source);
    }

    @Override
    public UserInfo[] newArray(int size) {
      // TODO Auto-generated method stub
      return null;
    }

  };

}
