package com.example.week04;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.week04.adapter.KeywordRowAdapter;
import com.example.week04.info.KeywordRowInfo;
import com.example.week04.info.DBHelper;
import com.example.week04.info.Settings;

import android.util.Log;

public class MainActivity extends Activity {

	// Server URL.
	private String serverURL;
	
	// Keyword list components.
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
		
		// Set debug or mainrun mode.
		serverURL = new Settings().getServerURL();
		
		// Set listview of keywords.
		keywordListView = (ListView)findViewById(R.id.keyword_list);
		keywordList = new ArrayList<KeywordRowInfo>();
		rowAdapter = new KeywordRowAdapter(this, R.layout.keyword_row, keywordList);
		keywordListView.setAdapter(rowAdapter);
		
		// Make listview scrollable.
		keywordListView.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				int action = event.getAction();
				switch (action) {
				case MotionEvent.ACTION_DOWN:
					// Disallow ScrollView to intercept touch events.
					v.getParent().requestDisallowInterceptTouchEvent(true);
					break;

				case MotionEvent.ACTION_UP:
					// Allow ScrollView to intercept touch events.
					v.getParent().requestDisallowInterceptTouchEvent(false);
					break;
				}

				// Handle ListView touch events.
				v.onTouchEvent(event);
				return true;
			}
		});
		
		// Update information of keyword list.
		setKeywordList();
		
		// Set onclicklistener for Add button.
		newKeywordView = (TextView) findViewById(R.id.keyword_new);
		newKeywordView.setOnClickListener(newItemMaker);
	}
	
	@Override
	protected void onResume() {
	   super.onResume();
	   setKeywordList();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    // Inflate the menu items for use in the action bar
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.actionbar_buttons, menu);
	    return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle presses on the action bar items
	    switch (item.getItemId()) {
	        case R.id.menubutton_scrap:
	        	Intent intent = new Intent(this, ScrapActivity.class);
				startActivity(intent);
				break;
	        case R.id.menubutton_logout:
	        	AlertDialog.Builder alert = new AlertDialog.Builder(this);

				alert.setTitle("Alert!");
				alert.setMessage("Logout from Hummingbird?");

				alert.setPositiveButton("Logout", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						SharedPreferences appPref = getSharedPreferences("appPref", 0);
						SharedPreferences.Editor edit = appPref.edit();
						edit.putString("loginPassword", "");
						edit.commit();
						Intent intent = new Intent(MainActivity.this, LoginActivity.class);
						intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						startActivity(intent); 
						finish();
					}
				});
				alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// nothing happens.
					}
				});
				
				alert.show();
				break;
	        case R.id.menubutton_settings:
	        	Toast.makeText(MainActivity.this, "Setting not implemented!", Toast.LENGTH_SHORT).show();
	        	break;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	    return true;
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
						// Add input keyword to server inBackground and crawl data.
						sendKeywordRegistrationIdToBackend(value);
						KeywordRowInfo newRow = new KeywordRowInfo();
						getArticleInBackground(value, newRow);
						
						// Add input keyword into DB.
						SQLiteDatabase db = mHelper.getWritableDatabase();
						String query = "INSERT INTO KEYWORDS(KEYWORD) VALUES('" + value + "');";
						db.execSQL(query);
						
						// Add keyword to ListView.
						newRow.setKeyword(value);
						newRow.setLastUpdate("Not updated yet");
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
		SharedPreferences appPref = getSharedPreferences("appPref", 0);
		final String loginId = appPref.getString("loginID", "");
		
    	new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... params) {
				String URL = serverURL + "addId/" + loginId + "/" + newKeyword;
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
	
	private void getArticleInBackground(final String keyword, final KeywordRowInfo keywordRow) {
		new AsyncTask<Void, Void, String>() {
        	private String resultMessage;
        	 
			@Override
			protected String doInBackground(Void... params) {
				String URL = serverURL + "getArticle/" + keyword;
				Log.i("URL", URL);
				DefaultHttpClient client = new DefaultHttpClient();
				String result;
	    		try {
	    			// Make connection to server.
	    			Log.i("Connection", "Make connection to server");
	    			HttpParams connectionParams = client.getParams();
	    			HttpConnectionParams.setConnectionTimeout(connectionParams, 5000);
	    			HttpConnectionParams.setSoTimeout(connectionParams, 5000);
	    			HttpGet httpGet = new HttpGet(URL);
	    			
	    			// Get response and parse entity.
	    			Log.i("Connection", "Get response and parse entity.");
	    			HttpResponse responsePost = client.execute(httpGet);
	    			HttpEntity resEntity = responsePost.getEntity(); 			
	    			
	    			// Parse result to string.
	    			Log.i("Connection", "Parse result to string.");
	    			result = EntityUtils.toString(resEntity);
	    			result = result.replaceAll("'|&lt;|&quot;|&gt;", "''");
	    		} catch (Exception e) {
	    			e.printStackTrace();
	    			Toast.makeText(MainActivity.this.getApplicationContext(), "Some error in server!", Toast.LENGTH_SHORT).show();
	    			result = "";
	    		}
	    		Log.i("Connection", "connection complete");
	    		
	    		client.getConnectionManager().shutdown();	// Disconnect.
	    		return result;
			}

			@Override
		    protected void onPostExecute(String result) {
				if( !result.isEmpty() ) {
					try {
						JSONArray articleArray = new JSONArray(result);
						int arrayLength = articleArray.length();
						int updatedRow = 0;
						mHelper = new DBHelper(MainActivity.this.getApplicationContext());
						SQLiteDatabase db = mHelper.getWritableDatabase();
						for(int i = 0; i < arrayLength; i++) {
							Log.i("Connection", i + "th JSONObject");
							JSONObject articleObject = articleArray.getJSONObject(i);
							String title = articleObject.getString("Title");
							Log.i("Connection", title);
							String link = articleObject.getString("Link");
							String date = articleObject.getString("Date");
							String news = articleObject.getString("News");
							String content = articleObject.getString("Head");
							String query = "INSERT INTO ARTICLES(KEYWORD, TITLE, NEWS, DATE, CONTENT, LINK) VALUES('"
										 + keyword + "', '" + title + "', '" + news + "', '" + date + "', '" + content + "', '" + link + "');";
							try {
								updatedRow++;
								db.execSQL(query);
							}
							catch(SQLException e) {
								updatedRow--;
								Log.i("SQL inserting", "SQL exception : duplicate row?");
							}
						}
						
						String thisTime = getThisTime();
						String query = "UPDATE KEYWORDS SET LASTUPDATE = '" + thisTime + "' WHERE KEYWORD = '" + keyword + "';";
						db.execSQL(query);
						
						mHelper.close();
						
						// Count new article and set data into view.
						keywordRow.setLastUpdate(thisTime);
						keywordRow.setNotifyNumber(updatedRow);
						
						if(updatedRow > 0) {
							resultMessage = "Article loading complete!";
						}
						else {
							resultMessage = "Loading complete - No fresh news.";
						}
					} catch (JSONException e) {
						e.printStackTrace();
						resultMessage = "Error in article loading - problem in JSONArray?";
					}
				}
				else {
					resultMessage = "Error in receiving articles!";
				}
				Toast.makeText(MainActivity.this.getApplicationContext(), resultMessage, Toast.LENGTH_SHORT).show();
			}
        }.execute(null, null, null);
    }
	
	private String getThisTime() {
		Date from = new Date();
		SimpleDateFormat transFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String to = transFormat.format(from);
		return to;
	}
}
