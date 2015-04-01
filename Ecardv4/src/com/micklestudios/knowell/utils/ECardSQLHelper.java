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

public class ECardSQLHelper extends SQLiteOpenHelper {

	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_NAME = "OfflineDataDB";
	
	// OfflineData table name
    private static final String TABLE_NAME = "TableData";
    // Books Table Columns names
    private static final String KEY_ID = "id";
    private static final String KEY_STORED = "stored";
    private static final String KEY_ECARDID = "ecardID";
    private static final String KEY_WHEREMET = "where_met";
    private static final String KEY_EVENTMET = "event_met";
    private static final String KEY_NOTES = "notes";
    private static final String KEY_VOICENOTE = "voiceNotes";
    private static final String TABLE_CREATE =
                "CREATE TABLE " + TABLE_NAME + " (" +
                KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                KEY_STORED + " INTEGER, " +
                KEY_ECARDID + " TEXT, " +
                KEY_WHEREMET + " TEXT, " +
                KEY_EVENTMET + " TEXT, " +
                KEY_NOTES + " TEXT, " + KEY_VOICENOTE + " TEXT)";

    private static final String[] COLUMNS = {KEY_ID, KEY_STORED, KEY_ECARDID, KEY_WHEREMET,KEY_EVENTMET,KEY_NOTES,KEY_VOICENOTE};
    
	public ECardSQLHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
    }
	
	public void addData(OfflineData olData){
		Log.d("addData", olData.toString());
		SQLiteDatabase db = this.getWritableDatabase();
		
		ContentValues values = new ContentValues();
		values.put(KEY_STORED, olData.getStored());
		values.put(KEY_ECARDID, olData.getEcardID());
		values.put(KEY_WHEREMET, olData.getWhereMet());
		values.put(KEY_EVENTMET, olData.getEventMet());
		values.put(KEY_NOTES, olData.getNotes());
		values.put(KEY_VOICENOTE, olData.getVoiceNote());
		
		db.insert(TABLE_NAME, null, values);
		db.close();
	}
	
	public List<OfflineData> getData(String column, String value){
		// column = KEY_WORD for search, value = value to be searched in that column
		List<OfflineData> olDatas = new LinkedList<OfflineData>();
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.query(TABLE_NAME, COLUMNS, " " + column + " = ?", new String[] {value}, null, null, null, null);
		if(cursor != null){
			OfflineData olData = null;			
			if(cursor.moveToFirst()){
				do{
					olData = new OfflineData();
					// getString(0) is the id inside the db
					olData.setStored(Integer.parseInt(cursor.getString(1)));
					olData.setEcardID(cursor.getString(2));
					olData.setWhereMet(cursor.getString(3));
					olData.setEventMet(cursor.getString(4));
					olData.setNotes(cursor.getString(5));
					olData.setVoiceNote(cursor.getString(6));
					olDatas.add(olData);
				} while (cursor.moveToNext());
			}			
		}
		Log.d("getData(" + column + " = " + value + ") ", olDatas.toString());
		return olDatas;
	}
	
	public List<OfflineData> getAllData(){
		List<OfflineData> olDatas = new LinkedList<OfflineData>();
		String query = "SELECT * FROM " + TABLE_NAME;
		
		SQLiteDatabase db = this.getWritableDatabase();
		Cursor cursor = db.rawQuery(query, null);
		
		OfflineData olData = null;
		if(cursor.moveToFirst()){
			do{
				olData = new OfflineData();
				olData.setStored(Integer.parseInt(cursor.getString(1)));
				olData.setEcardID(cursor.getString(2));
				olData.setWhereMet(cursor.getString(3));
				olData.setEventMet(cursor.getString(4));
				olData.setNotes(cursor.getString(5));
				olData.setVoiceNote(cursor.getString(6));
				olDatas.add(olData);
			} while (cursor.moveToNext());
		}
		
		Log.d("getAllDatas()", olDatas.toString());
		return olDatas;
	}
	
	public int updataData(OfflineData olData){
		SQLiteDatabase db = this.getWritableDatabase();
		
		ContentValues values = new ContentValues();
		values.put(KEY_STORED, olData.getStored());
		values.put(KEY_ECARDID, olData.getEcardID());
		values.put(KEY_WHEREMET, olData.getWhereMet());
		values.put(KEY_EVENTMET, olData.getEventMet());
		values.put(KEY_NOTES, olData.getNotes());
		values.put(KEY_VOICENOTE, olData.getVoiceNote());
		
		int i = db.update(TABLE_NAME, values, KEY_ECARDID+" = ?", new String[]{olData.getEcardID()});
		db.close();
		
		return i;
	}
	
	public void deleteData(OfflineData olData){
		SQLiteDatabase db = this.getWritableDatabase();
		db.delete(TABLE_NAME, KEY_ECARDID+" =?", new String[] {olData.getEcardID()});
		db.close();
		Log.d("deleteData", olData.toString());
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(ECardSQLHelper.class.getName(),
		        "Upgrading database from version " + oldVersion + " to "
		            + newVersion + ", which will destroy all old data");
		    db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
		    onCreate(db);
	}

}
