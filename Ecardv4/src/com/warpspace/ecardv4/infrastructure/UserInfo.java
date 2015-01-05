package com.warpspace.ecardv4.infrastructure;

import java.util.Locale;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.warpspace.ecardv4.R;

/**
 * Created by Udayan on 12/13/2014.
 */
public class UserInfo {
  String objId;
  String firstName;
  String lastName;

  String company;
  Context context;

  // Private.
  UserInfo() {

  }

  UserInfo(Context context, String objId, String firstName, String lastName) {
    this.context = context;
    this.objId = objId;
    this.firstName = firstName;
    this.lastName = lastName;
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
    String website = context.getString(R.string.base_website_user);

    // Always compare in lower case.
    String qrStringLower = qrString.toLowerCase(Locale.getDefault());

    int index = qrStringLower.indexOf(website);

    // We require the website to be in the beginning. Otherwise,
    // the string is invalid.
    if (index != 0) {
      return null;
    }

    String restOfString = qrStringLower.substring(website.length());

    String[] values = restOfString.split(".");

    // We should get 3 values.
    if (values.length != 3) {
      return null;
    }

    return new UserInfo(context, values[0], values[1], values[2]);
  }

  public Bitmap getQRCode() {
    return getQRCode(context, objId, firstName, lastName);
  }

}
