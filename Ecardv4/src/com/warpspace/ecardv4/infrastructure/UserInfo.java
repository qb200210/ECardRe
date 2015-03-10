package com.warpspace.ecardv4.infrastructure;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.warpspace.ecardv4.ActivityMain;
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
  String city;
  Bitmap portrait;
  String whereMet;
  String eventMet;
  Date createdAt;

  ArrayList<String> shownArrayList = new ArrayList<String>();
  ArrayList<Integer> infoIcon = new ArrayList<Integer>();
  ArrayList<String> infoLink = new ArrayList<String>();
  String[] allowedArray = { "about", "linkedin", "phone", "message", "email",
    "facebook", "twitter", "googleplus", "web" };

  private void setDefaults() {
    this.objId = "Unspecified";
    this.firstName = "Unspecified";
    this.lastName = "Unspecified";
    this.company = "Unspecified";
    this.title = "Unspecified";
    this.city = "Unspecified";
    this.whereMet = "Unspecified";
    this.eventMet = "Unspecified";
    this.createdAt = null;
  }

  public UserInfo(String objId, String firstName, String lastName,
    boolean localData, boolean networkAvailable, boolean imgFromTmpData) {
    setDefaults();
    this.objId = objId;
    this.firstName = firstName;
    this.lastName = lastName;
    populateUserInfoWithParseData(objId, localData, networkAvailable,
      imgFromTmpData);
  }

  public UserInfo(String objId) {
    setDefaults();
    this.objId = objId;
    populateUserInfoWithParseData(objId, true, false, false);
  }

  public UserInfo(Parcel source) {
    setDefaults();
    this.objId = source.readString();
    this.firstName = source.readString();
    this.lastName = source.readString();
    this.company = source.readString();
    this.title = source.readString();
    this.city = source.readString();
    this.portrait = (Bitmap) source.readParcelable(getClass().getClassLoader());
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
    dest.writeString(city);
    dest.writeParcelable(portrait, flags);
    dest.writeStringList(shownArrayList);
    dest.writeStringList(infoLink);
    dest.writeList(infoIcon);
  }

  private void populateUserInfoWithParseData(String objId, boolean localData,
    boolean networkAvailable, boolean imgFromTmpData) {
    if (localData || networkAvailable) {
      // If either this request is to build userInfo from localData
      // Or the request is to build userInfo from pulling data online, proceed
      // Otherwise do nothing
      ParseQuery<ParseObject> query = ParseQuery.getQuery("ECardInfo");
      if (localData) {
        // if flag true, means this is to pull data from localDataStore. Not
        // likely in the event of scanning new cards
        query.fromLocalDatastore();
      }
      try {
        // Must not use the getinbackground() thread method,
        // otherwise data won't be back before the UserInfo object is built
        // may want to implement a loading screen if taking too long?
        ParseObject object = query.get(objId);
        if (object != null) {
          if (!imgFromTmpData) {
            // get portrait from cached img
            ParseFile portraitFile = (ParseFile) object.get("portrait");
            if (portraitFile != null) {
              byte[] data = portraitFile.getData();
              portrait = BitmapFactory.decodeByteArray(data, 0, data.length);
            }
          } else {
            byte[] tmpImgData = (byte[]) object.get("tmpImgByteArray");
            portrait = BitmapFactory.decodeByteArray(tmpImgData, 0,
              tmpImgData.length);
          }

          // main card info
          firstName = object.getString("firstName");
          lastName = object.getString("lastName");
          company = object.getString("company");
          title = object.getString("title");
          city = object.getString("city");
          createdAt = object.getCreatedAt();

          Log.e("Dates!", "Created at " + createdAt);

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
    case "message":
      return R.drawable.message;
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

  public static Bitmap getQRCode(String objId, String firstName, String lastName) {
    String website = ActivityMain.applicationContext
      .getString(R.string.base_website_user);
    StringBuffer qrString = new StringBuffer(website);
    qrString.append("id=");
    qrString.append(objId);
    qrString.append("&fn=");
    qrString.append(firstName);
    qrString.append("&ln=");
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

  public static UserInfo getUserInfoFromQRString(String qrString,
    boolean networkAvailable) {

    HashMap<String, String> valuesMap = ECardUtils.parseQRString(qrString);

    // If the valuesMap is null, the string is ill-formed.
    if (valuesMap == null) {
      return null;
    }

    // Let's fetch these values now
    String id = valuesMap.get("id");
    String fname = valuesMap.get("fn");
    String lname = valuesMap.get("ln");

    Log.e("Got string", id + " " + fname + " " + lname);

    // TODO: Udayan:::
    // error tolerant: 1. if input string isn't ecard link, 2. if input objectId
    // doesn't exist
    // 3. if input objectId already collected, 4. if no network
    return new UserInfo(id, fname, lname, false, networkAvailable, false);
  }

  public Bitmap getQRCode() {
    return getQRCode(objId, firstName, lastName);
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

  public Bitmap getPortrait() {
    return portrait;
  }

  public void setPortrait(Bitmap portrait) {
    this.portrait = portrait;
  }

  public String getWhereMet() {
    return whereMet;
  }

  public void setWhereMet(String whereMet) {
    this.whereMet = whereMet;
  }

  public String getEventMet() {
    return eventMet;
  }

  public void setEventMet(String eventMet) {
    this.eventMet = eventMet;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
  }

  public String[] getAllowedArray() {
    return allowedArray;
  }

  public void setAllowedArray(String[] allowedArray) {
    this.allowedArray = allowedArray;
  }

  public void setObjId(String objId) {
    this.objId = objId;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public void setCompany(String company) {
    this.company = company;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public void setShownArrayList(ArrayList<String> shownArrayList) {
    this.shownArrayList = shownArrayList;
  }

  public void setInfoIcon(ArrayList<Integer> infoIcon) {
    this.infoIcon = infoIcon;
  }

  public void setInfoLink(ArrayList<String> infoLink) {
    this.infoLink = infoLink;
  }

  public String getCity() {
	return city;
}

public void setCity(String city) {
	this.city = city;
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
