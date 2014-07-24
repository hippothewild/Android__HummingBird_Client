package com.example.week04;

import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.example.week04.adapter.KeywordRowAdapter;
import com.example.week04.info.KeywordRowInfo;
import com.example.week04.info.DBHelper;
import com.example.week04.info.Settings;

import android.util.Log;

public class MainActivity extends Activity {

	// Server URL.
	private String serverURL;
	
	private KeywordRowAdapter rowAdapter;
	private ArrayList<KeywordRowInfo> keywordList;
	private ListView keywordListView;
	private TextView newKeywordView;
	private DBHelper mHelper;
	
	View mainView;
	
	private static String regid = "";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// Set debug or mainrun mode.
		serverURL = new Settings().getServerURL();
		
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
		
		// Set GCM to device.
		
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
			int notifyNumber = cursor.getInt(2);
			newRow.setKeyword(keywordName);
			newRow.setLastUpdate(lastUpdate);
			newRow.setNotifyNumber(notifyNumber);
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
					String value = input.getText().toString().trim();
					if( !value.isEmpty() ) {
						// Add input keyword to server inBackground.
						sendKeywordRegistrationIdToBackend(value);
						
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
	
	private void sendKeywordRegistrationIdToBackend(final String newKeyword) {
		if(regid.isEmpty()) {
			SharedPreferences appPref = getSharedPreferences("appPref", 0);
			regid = appPref.getString("gcmToken", "");
			if(regid.isEmpty()) {
				finish();
			}
		}
    	new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... params) {
				String URL = serverURL + "addId/" + regid + "/" + newKeyword;
				DefaultHttpClient client = new DefaultHttpClient();
	    		try {
	    
	    			// Make connection to server.
	    			HttpParams connectionParams = client.getParams();
	    			HttpConnectionParams.setConnectionTimeout(connectionParams, 5000);
	    			HttpConnectionParams.setSoTimeout(connectionParams, 5000);
	    			HttpGet httpGet = new HttpGet(URL);
	    			
	    			// Get response and parse entity.
	    			HttpResponse responsePost = client.execute(httpGet);
	    			HttpEntity resEntity = responsePost.getEntity();
	    			
	    			// Parse result to string.
	    			String result = EntityUtils.toString(resEntity);
	    			client.getConnectionManager().shutdown();
	    			return result;
	    			
	    		} catch (Exception e) {
	    			e.printStackTrace();
	    			client.getConnectionManager().shutdown();	// Disconnect.
	    			return "";
	    		}
			}
    		
    	}.execute(null, null, null);
	}
}
