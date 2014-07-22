package com.example.week04;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.example.week04.adapter.KeywordRowAdapter;
import com.example.week04.info.KeywordRowInfo;
import com.example.week04.info.DBHelper;

public class MainActivity extends Activity {

	private KeywordRowAdapter rowAdapter;
	private ArrayList<KeywordRowInfo> keywordList;
	private ListView keywordListView;
	private TextView newKeywordView;
	private DBHelper mHelper;
	
	View mainView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// Set listview of keywords.
		keywordListView = (ListView)findViewById(R.id.keyword_list);
		keywordList = new ArrayList<KeywordRowInfo>();
		rowAdapter = new KeywordRowAdapter(this, R.layout.keyword_row, keywordList);
		keywordListView.setAdapter(rowAdapter);
		
		// Update information of keyword list.
		setKeywordList();
		
		// Set onclicklistener for Add button.
		newKeywordView = (TextView) findViewById(R.id.keyword_new);
		newKeywordView.setOnClickListener(newItemMaker);
	}
	
	private void setKeywordList()  {
		keywordList.clear();
		
		mHelper = new DBHelper(this);
		SQLiteDatabase db = mHelper.getReadableDatabase();
		Cursor cursor = db.rawQuery("SELECT * FROM KEYWORDS", null);
		
		while(cursor.moveToNext()) {
			KeywordRowInfo newRow = new KeywordRowInfo();
			String keywordName = cursor.getString(0);
			String lastUpdate = cursor.getString(1);
			newRow.setKeyword(keywordName);
			newRow.setLastUpdate(lastUpdate);
			newRow.setNotifyNumber(42);
			newRow.setButtonVisible(false);

			keywordList.add(newRow);
		}
		
		mHelper.close();
		rowAdapter.notifyDataSetChanged();
	}
	
	private OnClickListener newItemMaker = new OnClickListener() {
		@Override
		public void onClick(View v) {
			AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);

			alert.setTitle("New Keyword");
			alert.setMessage("Type new keyword!");

			// Set an EditText view to get user input
			final EditText input = new EditText(MainActivity.this);
			alert.setView(input);
			
			alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					String value = input.getText().toString();
					value.toString();
					
					// Add input keyword into DB.
					SQLiteDatabase db = mHelper.getWritableDatabase();
					String query = "INSERT INTO KEYWORDS(KEYWORD) VALUES('" + value + "');";
					db.execSQL(query);
					
					// Add keyword to ListView.
					KeywordRowInfo newRow = new KeywordRowInfo();
					newRow.setKeyword(value);
					newRow.setLastUpdate(": Not updated yet");
					newRow.setNotifyNumber(0);
					newRow.setButtonVisible(false);
					keywordList.add(newRow);
					rowAdapter.notifyDataSetChanged();
				}
			});


			alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					// Canceled.
				}
			});
			
			alert.show(); 
		}	
	};
}
