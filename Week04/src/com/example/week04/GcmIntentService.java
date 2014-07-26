/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.week04;

import java.text.SimpleDateFormat;
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

import com.example.week04.info.DBHelper;
import com.example.week04.info.Settings;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

/**
 * This {@code IntentService} does the actual handling of the GCM message.
 * {@code GcmBroadcastReceiver} (a {@code WakefulBroadcastReceiver}) holds a
 * partial wake lock for this service while the service does its work. When the
 * service is finished, it calls {@code completeWakefulIntent()} to release the
 * wake lock.
 */
public class GcmIntentService extends IntentService {
    public static final int NOTIFICATION_ID = 1;
    private NotificationManager mNotificationManager;
    NotificationCompat.Builder builder;

    public GcmIntentService() {
        super("GcmIntentService");
    }
    public static final String TAG = "GCM intent";

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);

        if (!extras.isEmpty()) {  // has effect of unparcelling Bundle
            /*
             * Filter messages based on message type. Since it is likely that GCM will be
             * extended in the future with new message types, just ignore any message types you're
             * not interested in, or that you don't recognize.
             */
            if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
                sendNotification("Send error: " + extras.toString());
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
                sendNotification("Deleted messages on server: " + extras.toString());
            // If it's a regular GCM message, do some work.
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                // TODO :: check wifi-stat                
                // Receive articles, and post notification of received message.
                String keyword = extras.getString("new data");
                getArticleInBackground(keyword);                
                
                
                Log.i(TAG, "Received: " + extras.toString());
            }
        }
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    // Put the message into a notification and post it.
    // This is just one simple example of what you might choose to do with
    // a GCM message.
    private void sendNotification(String msg) {
        mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
        .setSmallIcon(R.drawable.hummingbird_icon_small)
        .setAutoCancel(true)
        .setVibrate(new long[] { 0, 500, 200, 500 })
        .setContentTitle("Hummingbird")
        .setStyle(new NotificationCompat.BigTextStyle()
        .bigText(msg))
        .setContentText(msg);

        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }
    
	private void getArticleInBackground(final String keyword) {
		new AsyncTask<Void, Void, Void>() {
        	private String resultMessage = "";
        	 
			@Override
			protected Void doInBackground(Void... params) {
				String serverURL = new Settings().getServerURL();
				String URL = serverURL + "getArticle/" + keyword;
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
	    			Log.i("Connection", "Some error in server!");
	    			result = "";
	    		}
	    		
	    		client.getConnectionManager().shutdown();	// Disconnect.
	    		
	    		if( !result.isEmpty() ) {
					try {
						JSONArray articleArray = new JSONArray(result);
						int arrayLength = articleArray.length();
						int updatedRow = 0;
						
						DBHelper mHelper = new DBHelper(getApplicationContext());
						SQLiteDatabase db = mHelper.getWritableDatabase();
						for(int i = 0; i < arrayLength; i++) {
							JSONObject articleObject = articleArray.getJSONObject(i);
							String title = articleObject.getString("Title");
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
								Log.i("SQL inserting", "SQL exception in " + i + "th row : duplicated?");
							}
							
						}
						
						String thisTime = getThisTime();
						String query = "UPDATE KEYWORDS SET LASTUPDATE = '" + thisTime + "' WHERE KEYWORD = '" + keyword + "';";
						db.execSQL(query);
						
						mHelper.close();
						
						if(updatedRow > 0) {
							resultMessage = "Article loading complete!";
							sendNotification("'" + keyword + "'에 대한 새로운 소식이 " + updatedRow + "건 있습니다!");
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
	    		Log.i("JSON parsing", resultMessage);
				return null;
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