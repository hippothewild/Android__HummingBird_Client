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
		String query = "CREATE TABLE KEYWORDS ( \r" 
					 + "KEYWORD CHAR(128) PRIMARY KEY, \r"
					 + "LASTUPDATE CHAR(64) DEFAULT 'Not updated yet', \r"
					 + "LASTVIEW CHAR(64) DEFAULT 'Not updated yet');"; 
		/* String query = "CREATE TABLE KEYWORDS ( \r" 
					 + "ID INTEGER PRIMARY KEY AUTOINCREMENT, \r"
					 + "KEYWORD CHAR(128) UNIQUE NOT NULL, \r"
					 + "LASTUPDATE CHAR(64) DEFAULT '2000-01-01', \r"
					 + "LASTVIEW CHAR(64) DEFAULT '2000-01-01');"; */
		db.execSQL(query);
		
		query = "CREATE TABLE ARTICLES ( \r"
			  + "ID INTEGER PRIMARY KEY AUTOINCREMENT, \r"
			  + "KEYWORD CHAR(128) REFERENCES KEYWORDS(KEYWORD), \r"
			  + "TITLE TEXT NOT NULL, \r"
			  + "NEWS CHAR(64) NOT NULL, \r"
			  + "DATE CHAR(64) NOT NULL, \r"
			  + "CONTENT TEXT NOT NULL, \r"
			  + "LINK TEXT NOT NULL);";
		/* query = "CREATE TABLE ARTICLES ( \r" 
			  + "ID INTEGER NOT NULL REFERENCES KEYWORDS(ID), \r"
			  + "CONTENT TEXT NOT NULL, \r"
			  + "LINK TEXT NOT NULL, \r"
			  + "PRIMARY KEY(ID));"; */
		db.execSQL(query);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		
	}
}