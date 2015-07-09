package com.micklestudios.knowell.utils;

import java.util.LinkedList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class ECardSQLHelperCachedShares extends SQLiteOpenHelper {

  private static final int DATABASE_VERSION = 1;
  private static final String DATABASE_NAME = "OfflineSharesDB";

  // OfflineData table name
  private static final String TABLE_NAME_CACHEDSHARES = "CachedShares";
  // Books Table Columns names
  private static final String KEY_ID = "id";
  private static final String KEY_STORED = "stored";
  private static final String KEY_PARTYA = "partyA";
  private static final String KEY_PARTYB = "partyB";
  private static final String TABLE_CREATE = "CREATE TABLE "
    + TABLE_NAME_CACHEDSHARES + " (" + KEY_ID
    + " INTEGER PRIMARY KEY AUTOINCREMENT, " + KEY_STORED + " INTEGER, "
    + KEY_PARTYA + " TEXT, " + KEY_PARTYB + " TEXT)";

  private static final String[] COLUMNS = { KEY_ID, KEY_STORED, KEY_PARTYA,
    KEY_PARTYB };

  public ECardSQLHelperCachedShares(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL(TABLE_CREATE);
  }

  public void addData(OfflineDataCachedShares olData) {
    Log.d("addData", olData.toString());
    SQLiteDatabase db = this.getWritableDatabase();

    ContentValues values = new ContentValues();
    values.put(KEY_STORED, olData.getStored());
    values.put(KEY_PARTYA, olData.getPartyA());
    values.put(KEY_PARTYB, olData.getPartyB());

    db.insert(TABLE_NAME_CACHEDSHARES, null, values);
    db.close();
  }

  public List<OfflineDataCachedShares> getData(String column, String value) {
    // column = KEY_WORD for search, value = value to be searched in that column
    List<OfflineDataCachedShares> olDatas = new LinkedList<OfflineDataCachedShares>();
    SQLiteDatabase db = this.getReadableDatabase();
    Cursor cursor = db.query(TABLE_NAME_CACHEDSHARES, COLUMNS, " " + column
      + " = ?", new String[] { value }, null, null, null, null);
    if (cursor != null) {
      OfflineDataCachedShares olData = null;
      if (cursor.moveToFirst()) {
        do {
          olData = new OfflineDataCachedShares();
          // getString(0) is the id inside the db
          olData.setStored(Integer.parseInt(cursor.getString(1)));
          olData.setPartyA(cursor.getString(2));
          olData.setPartyB(cursor.getString(3));
          olDatas.add(olData);
        } while (cursor.moveToNext());
      }
    }
    Log.d("getData(" + column + " = " + value + ") ", olDatas.toString());
    return olDatas;
  }

  public List<OfflineDataCachedShares> getAllData() {
    List<OfflineDataCachedShares> olDatas = new LinkedList<OfflineDataCachedShares>();
    String query = "SELECT * FROM " + TABLE_NAME_CACHEDSHARES;

    SQLiteDatabase db = this.getWritableDatabase();
    Cursor cursor = db.rawQuery(query, null);

    OfflineDataCachedShares olData = null;
    if (cursor.moveToFirst()) {
      do {
        olData = new OfflineDataCachedShares();
        olData.setStored(Integer.parseInt(cursor.getString(1)));
        olData.setPartyA(cursor.getString(2));
        olData.setPartyB(cursor.getString(3));
        olDatas.add(olData);
      } while (cursor.moveToNext());
    }

    Log.d("getAllDatas()", olDatas.toString());
    return olDatas;
  }

  public int updataData(OfflineDataCachedShares olData) {
    SQLiteDatabase db = this.getWritableDatabase();

    ContentValues values = new ContentValues();
    values.put(KEY_STORED, olData.getStored());
    values.put(KEY_PARTYA, olData.getPartyA());
    values.put(KEY_PARTYB, olData.getPartyB());

    int i = db.update(TABLE_NAME_CACHEDSHARES, values, KEY_PARTYB + " = ?",
      new String[] { olData.getPartyB() });
    db.close();

    return i;
  }

  public void deleteData(OfflineDataCachedShares olData) {
    SQLiteDatabase db = this.getWritableDatabase();
    db.delete(TABLE_NAME_CACHEDSHARES, KEY_PARTYB + " =?",
      new String[] { olData.getPartyB() });
    db.close();
    Log.d("deleteData", olData.toString());
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    Log.w(ECardSQLHelperCachedShares.class.getName(),
      "Upgrading database from version " + oldVersion + " to " + newVersion
        + ", which will destroy all old data");
    db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME_CACHEDSHARES);
    onCreate(db);
  }

}
