package com.example.week04;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.example.week04.info.DBHelper;

import android.util.Log;

public class IntroActivity extends Activity {

	// Project number of API Console for GCM pushing.
	String SENDER_ID = "402485531539";
	GoogleCloudMessaging gcm;
	Context mContext;
	String regid;
	String inputText;
	
	private ViewFlipper viewFlipper;
    private float lastX;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = getApplicationContext();
		inputText = "";
		
		// No title bar at the intro-activity.
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		SharedPreferences appPref = getSharedPreferences("appPref", 0);
		boolean hasPreference = appPref.getBoolean("HasPreference", false);
		if(hasPreference) {
			Log.i("IntroActivity", "preference set!");
			
			// It's not the first use of app. just ignore intro and go to main activity.
			Intent intent = new Intent(IntroActivity.this, MainActivity.class);
            startActivity(intent);
            // Main page loading successed. intro activity is no use; finish it!
            finish();
		}
		else {
	
			// If it's first use of app, show intro and set default preferences.
			requestWindowFeature(Window.FEATURE_NO_TITLE);	// No title bar at intro.
			setContentView(R.layout.activity_intro);
			
			viewFlipper = (ViewFlipper) findViewById(R.id.intro_flipper);	
			Button startButton = (Button) findViewById(R.id.startButton);
			startButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					EditText editText = (EditText) findViewById(R.id.intro_first_keyword);
					inputText = editText.getText().toString().trim();
					if(inputText.compareTo("") == 0) {
						Toast.makeText(IntroActivity.this, "Give keyword for HummingBird, please!", Toast.LENGTH_SHORT).show();
					}
					else {
						if (checkPlayServices()) {
							gcm = GoogleCloudMessaging.getInstance(IntroActivity.this);
							regid = getRegistrationId(mContext);

				            if (regid.isEmpty()) {
				                registerInBackground();
				            }
				        } else {
				            Log.i("Google Play APK Checking", "No valid Google Play Services APK found.");
				            finish();
				        }
						
						// Make up app's first preference for user.
						appMakeUp(inputText);
						
						// Load main activity.
						Intent intent = new Intent(IntroActivity.this, MainActivity.class);
		                startActivity(intent);
		                finish();
					}
				}
			});
		}
	}
	
	@Override
    protected void onResume() {
        super.onResume();
        // Check device for Play Services APK.
        checkPlayServices();
    }	
	
	// Method to handle touch event like left to right swap and right to left swap
    public boolean onTouchEvent(MotionEvent touchevent) {
    	switch (touchevent.getAction()) {
    		case MotionEvent.ACTION_DOWN: 	// User attatched their thumb on screen.
    			lastX = touchevent.getX();
    			break;
    		case MotionEvent.ACTION_UP: 	// User detached their thumb screen.
    			float currentX = touchevent.getX();

    			if (lastX < currentX) { // left->right swipe.
    			
    				if (viewFlipper.getDisplayedChild() == 0) {
    					// No more view to flip. just break.
    					break;
    				}
    				viewFlipper.setInAnimation(this, R.anim.in_from_left);
    				viewFlipper.setOutAnimation(this, R.anim.out_to_right);
    				// Show the next screen.
    				viewFlipper.showNext();
    			}
    			else if (lastX > currentX) { // right->left swipe.
    				if (viewFlipper.getDisplayedChild() == 1) {
    					// No more view to flip. just break.
    					break;
    				}
    				viewFlipper.setInAnimation(this, R.anim.in_from_right);
    				viewFlipper.setOutAnimation(this, R.anim.out_to_left);
    				// Show the previous screen.
    				viewFlipper.showPrevious();
    			}
    			break;
    	}
    	return false;
    }
    
    // First make-up functions called only once in the app.
    private void appMakeUp(String value) {
    	// Make first preference of the app.
		SharedPreferences appPref = getSharedPreferences("appPref", 0);
		SharedPreferences.Editor edit = appPref.edit();
		edit.putBoolean("CrawlOnlyInWifi", true);
		edit.putInt("CrawlIntervalInMinutes", 1440);
		edit.putInt("DayTimeStart", 7);
		edit.putInt("DayTimeEnd", 23);
		edit.putBoolean("NotifyOnlyInDaytime", true);
		edit.putBoolean("HasPreference", true);
		edit.commit();
		
		// Make up DB.
		DBHelper mHelper = new DBHelper(this);
		SQLiteDatabase db = mHelper.getWritableDatabase();
		String query = "INSERT INTO KEYWORDS(KEYWORD) VALUES('" + value + "');";
		db.execSQL(query);
		mHelper.close();
    }
    
    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        9000).show();
            } else {
                Log.i("Play Service Check", "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }
    
    // Function getting GCM registration ID.
    private String getRegistrationId(Context context) {
        final SharedPreferences prefs = getSharedPreferences("appPref", 0);
        String registrationId = prefs.getString("gcmToken", "");
        if (registrationId.isEmpty()) {
            Log.i("Getting GCM reg-ID", "Registration not found.");
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion = prefs.getInt("appVersion", Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            Log.i("Getting GCM reg-ID", "App version changed.");
            return "";
        }
        return registrationId;
    }
    
    /**
     * @return Application's version code from the {@code PackageManager}.
     */
    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }
    
    /**
     * Registers the application with GCM servers asynchronously.
     * Stores the registration ID and the app versionCode in the application's
     * shared preferences.
     */
    private void registerInBackground() {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String msg = "";
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(mContext);
                    }
                    regid = gcm.register(SENDER_ID);
                    msg = "Device registered, registration ID=" + regid;

                    // You should send the registration ID to your server over HTTP, so it
                    // can use GCM/HTTP or CCS to send messages to your app.
                    sendRegistrationIdToBackend();

                    // For this demo: we don't need to send it because the device will send
                    // upstream messages to a server that echo back the message using the
                    // 'from' address in the message.

                    // Persist the regID - no need to register again.
                    SharedPreferences appPref = getSharedPreferences("appPref", 0);
            		SharedPreferences.Editor edit = appPref.edit();
            		edit.putString("gcmToken", regid);
            		edit.putInt("appVersion", getAppVersion(mContext));
            		edit.commit();
                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                    // If there is an error, don't just keep trying to register.
                    // Require the user to click a button again, or perform
                    // exponential back-off.
                }
                return msg;
            }

            @Override
            protected void onPostExecute(String msg) {
                Log.i("registering", msg);
            }
        }.execute(null, null, null);
    }
    
    /**
     * Sends the registration ID to your server over HTTP, so it can use GCM/HTTP or CCS to send
     * messages to your app. Not needed for this demo since the device sends upstream messages
     * to a server that echoes back the message using the 'from' address in the message.
     */
    private void sendRegistrationIdToBackend() {
    	new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... params) {
				String URL = "http://blooming-castle-2040.herokuapp.com/addId/" + regid + "/" + inputText;
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
