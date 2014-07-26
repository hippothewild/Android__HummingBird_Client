package com.example.week04.adapter;

import android.database.SQLException;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ExecutionException;

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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.week04.ArticleActivity;
import com.example.week04.R;
import com.example.week04.info.DBHelper;
import com.example.week04.info.KeywordRowInfo;
import com.example.week04.info.Settings;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

public class KeywordRowAdapter extends ArrayAdapter<KeywordRowInfo> {

	// Server URL.
	private String serverURL;
	
	// Resources for building adapter.
	private Context mContext;
	private int mResource;
	private ArrayList<KeywordRowInfo> mList;
	private LayoutInflater mInflater;
	private DBHelper mHelper;
	
	// Touch event variables.
	private int action_down_x = 0;
	
	public KeywordRowAdapter(Context context, int resource, ArrayList<KeywordRowInfo> objects) {
		super(context, resource, objects);
		this.mContext = context;
		this.mResource = resource;
		this.mList = objects;
		this.mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.serverURL = new Settings().getServerURL();
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent){
		
		final KeywordRowInfo keywordRow = mList.get(position);

		if(convertView == null){
			convertView = mInflater.inflate(mResource, null);
		}
		final View tempView = convertView;
		final int pos = position;

		// Add 'swipe to delete' in the row.
		convertView.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(final View v, MotionEvent event) {
				switch( event.getAction() ) {
					case MotionEvent.ACTION_DOWN :
						action_down_x = (int) event.getX();
						break;
					case MotionEvent.ACTION_UP :
						final int difference = action_down_x - (int)event.getX();
						if (difference > 40) {
							tempView.findViewById(R.id.btn_remove).setVisibility(View.VISIBLE);
							mList.get(pos).setButtonVisible(true);
						}
						else if (difference < -40) {
							tempView.findViewById(R.id.btn_remove).setVisibility(View.GONE);
							mList.get(pos).setButtonVisible(false);
						}
						else if (difference < 5 && difference > -5)	 {
							Log.i("KeywordRowAdapter", "Click");
							Intent intent = new Intent(mContext, ArticleActivity.class);
							Bundle b = new Bundle();
							b.putString("keyword", keywordRow.getKeyword());
							intent.putExtras(b);
							mContext.startActivity(intent);
						}
						
						break;
				}
				return true;
		    }
		});
		
		if(keywordRow != null){
			final TextView keywordName = (TextView) convertView.findViewById(R.id.keyword_name);
			final TextView keywordLastUpdate = (TextView) convertView.findViewById(R.id.keyword_last_update);
			final TextView notifyCount = (TextView) convertView.findViewById(R.id.notify_count);
			
			final String thisKeyword = keywordRow.getKeyword();
			keywordName.setText(thisKeyword);
			keywordLastUpdate.setText("Last Update : " + keywordRow.getLastUpdate());
			if(keywordRow.getNotifyNumber() > 0) {
				notifyCount.setText(String.valueOf(keywordRow.getNotifyNumber()));
			}
			else {
				notifyCount.setText("");
			}
			
			// Set on-click listener to refresh button.
			ImageView buttonRefresh = (ImageView) convertView.findViewById(R.id.btn_refresh);
			buttonRefresh.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(final View v) {
					// Get articles from server and set into DB.
					getArticleInBackground(thisKeyword, keywordRow, notifyCount);	
				}
			});
			
			
			// Set remove button to remove items.
			TextView buttonRemove = (TextView) convertView.findViewById(R.id.btn_remove);
			if(keywordRow.isButtonVisible()) {
				buttonRemove.setVisibility(View.VISIBLE);
			}
			else {
				buttonRemove.setVisibility(View.GONE);
			}
			
			// Set on-click listener to button item.
			buttonRemove.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(final View v) {
					AlertDialog.Builder alert = new AlertDialog.Builder(mContext);

					alert.setTitle("Alert!");
					alert.setMessage("Really delete this keyword?");

					alert.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							String keyword = mList.get(pos).getKeyword();
							
							// Remove item from backend server.
							String backGroundResult = deleteKeywordBackground(keyword);
							if( backGroundResult.equals("done") ) {
								Toast.makeText(mContext, "Delete complte!", Toast.LENGTH_SHORT);
							}
							else {
								Toast.makeText(mContext, backGroundResult, Toast.LENGTH_SHORT);
							}
							
							// Remove item from DB.
							mHelper = new DBHelper(mContext);
							SQLiteDatabase db = mHelper.getWritableDatabase();
							String query = "DELETE FROM KEYWORDS WHERE KEYWORD='" + keyword + "';";
							db.execSQL(query);
							mHelper.close();
							
							// Remove item from Listview.
							mList.get(pos).setButtonVisible(false);
							mList.remove(pos);
							notifyDataSetChanged();
						}
					});
					
					alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							tempView.findViewById(R.id.btn_remove).setVisibility(View.GONE);
						}
					});
					
					alert.show(); 
				}
			});
			
		}

		return convertView;
	}
	
	private void getArticleInBackground(final String keyword, final KeywordRowInfo keywordRow, final TextView notifyCount) {
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
	    			Toast.makeText(mContext, "Some error in server!", Toast.LENGTH_SHORT).show();
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
						mHelper = new DBHelper(mContext);
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
						notifyCount.setText(String.valueOf(arrayLength));
						notifyDataSetChanged();
						
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
				Toast.makeText(mContext, resultMessage, Toast.LENGTH_SHORT).show();
			}
        }.execute(null, null, null);
    }
	
	private String deleteKeywordBackground(final String newKeyword) {
		SharedPreferences appPref = mContext.getSharedPreferences("appPref", 0);
		final String loginId = appPref.getString("loginID", "");
		
		String result = "";
		if(loginId.isEmpty()) {
			return "failed = unable to get token!";
		}
		
    	try {
			result = new AsyncTask<Void, Void, String>() {
				@Override
				protected String doInBackground(Void... params) {
					String URL = serverURL + "deleteId/" + loginId + "/" + newKeyword;
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
						String getString = EntityUtils.toString(resEntity);
						client.getConnectionManager().shutdown();
						return getString;
						
					} catch (Exception e) {
						e.printStackTrace();
						client.getConnectionManager().shutdown();	// Disconnect.
						return "failed";
					}
				}
				
			}.execute(null, null, null).get();
			return result;
		} catch (InterruptedException e) {
			e.printStackTrace();
			return "failed = some exception!";
		} catch (ExecutionException e) {
			e.printStackTrace();
			return "failed = some exception!";
		}
	}
	
	private String getThisTime() {
		Date from = new Date();
		SimpleDateFormat transFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String to = transFormat.format(from);
		return to;
	}
}
