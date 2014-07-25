package com.example.week04.info;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {

	public DBHelper(Context context) {
		super(context, "userdata.db", null, 1);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// Keyword table.
		String query = "CREATE TABLE KEYWORDS ( \r" 
					 + "KEYWORD CHAR(128) PRIMARY KEY, \r"
					 + "LASTUPDATE CHAR(64) DEFAULT 'Not updated yet', \r"
					 + "NOTIFYNUMBER INTEGER DEFAULT 0);"; 
		db.execSQL(query);
		
		// Article table.
		query = "CREATE TABLE ARTICLES ( \r"
			  + "ID INTEGER PRIMARY KEY AUTOINCREMENT, \r"
			  + "KEYWORD CHAR(128) REFERENCES KEYWORDS(KEYWORD), \r"
			  + "TITLE TEXT NOT NULL, \r"
			  + "NEWS CHAR(64) NOT NULL, \r"
			  + "DATE CHAR(64) NOT NULL, \r"
			  + "CONTENT TEXT NOT NULL, \r"
			  + "LINK TEXT UNIQUE NOT NULL, \r"
			  + "VISIBLE INTEGER(1) DEFAULT 1"
			  + ");";
		db.execSQL(query);
		
		// Scraped article table.
		query = "CREATE TABLE SCRAPS ( \r"
			  + "ID INTEGER PRIMARY KEY AUTOINCREMENT, \r"
			  + "KEYWORD CHAR(128) REFERENCES KEYWORDS(KEYWORD), \r"
			  + "TITLE TEXT NOT NULL, \r"
			  + "NEWS CHAR(64) NOT NULL, \r"
			  + "DATE CHAR(64) NOT NULL, \r"
			  + "CONTENT TEXT NOT NULL, \r"
			  + "LINK TEXT UNIQUE NOT NULL"
			  + ");";
		db.execSQL(query);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		
	}
}