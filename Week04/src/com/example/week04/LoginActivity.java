package com.example.week04;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import com.example.week04.info.Settings;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class LoginActivity extends Activity {
	// Project number of API Console for GCM pushing.
	String SENDER_ID = "402485531539";
	GoogleCloudMessaging gcm;
	String regId = "";
	Context mContext;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = getApplicationContext();
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		// get GCM token in background.
		if (checkPlayServices()) {
			gcm = GoogleCloudMessaging.getInstance(LoginActivity.this);
			regId = getRegistrationId(mContext);
		}
        if (regId.isEmpty()) {
            registerInBackground();
        }
		
		// Check preference for autoLogin check. if there is. try to login.
		SharedPreferences appPref = getSharedPreferences("appPref", 0);
		String autoLoginPassword = appPref.getString("loginPassword", ""); 
		if( !autoLoginPassword.isEmpty() && !regId.isEmpty() ) {
			String autoLoginId = appPref.getString("loginID", "");
			try {
				String loginResult = new doLogin().execute(autoLoginId, autoLoginPassword, regId).get();
				if( loginResult.equals("success") ) {
					startApp();
				}
			} catch (Exception e) { }
		}
			
		setContentView(R.layout.activity_login);

		final EditText email = (EditText) findViewById(R.id.login_email);
		final EditText password = (EditText) findViewById(R.id.login_password);
		final Button loginButton = (Button) findViewById(R.id.btnLogin);
		final CheckBox checkBox = (CheckBox) findViewById(R.id.autoLogin);
		final TextView registerView = (TextView) findViewById(R.id.link_to_register);
		
		// Set onclicklistener to login button.
		loginButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String emailInput = email.getText().toString();
				String passInput = password.getText().toString();
				
				try {
					// connect to server and verify id and password.
					if( emailInput.isEmpty() ) {
						Toast.makeText(LoginActivity.this, "Login failed :: Blanked ID field!", Toast.LENGTH_SHORT).show();
					}
					else if( regId.isEmpty() ) {
						Toast.makeText(LoginActivity.this, "Login failed :: Blanked Token field!", Toast.LENGTH_SHORT).show();
					}
					else {
						String loginResult = new doLogin().execute(emailInput, passInput).get();						
						if( loginResult.equals("success") ) {
							SharedPreferences appPref = getSharedPreferences("appPref", 0);
							SharedPreferences.Editor edit = appPref.edit();
							edit.putString("loginID", emailInput);
							if(checkBox.isChecked()) {
								edit.putString("loginPassword", passInput);
							}
							edit.commit();
							
							startApp();
						}
						else {
							Toast.makeText(LoginActivity.this, loginResult, Toast.LENGTH_SHORT).show();
						}
					}
				}
				catch(Exception e) {
					e.printStackTrace();
				}
			}
		});

		// Set onclicklistener to register link.
		registerView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {		
				Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
				startActivity(intent);
			}
		});
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
            Log.i("Getting GCM reg-ID", "Registration not found. try to get from GCM server.");
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion = prefs.getInt("appVersion", Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            Log.i("Getting GCM reg-ID", "App version changed. try to get from GCM server.");
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
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				try {
					if (gcm == null) {
						gcm = GoogleCloudMessaging.getInstance(mContext);
					}
					regId = gcm.register(SENDER_ID);

					// Persist the regID - no need to register again.
					SharedPreferences appPref = getSharedPreferences("appPref", 0);
					SharedPreferences.Editor edit = appPref.edit();
					edit.putString("gcmToken", regId);
					edit.putInt("appVersion", getAppVersion(mContext));
					edit.commit();
				} catch (IOException ex) {
					ex.printStackTrace();
					Log.i("Connecting GCM server", "Error :" + ex.getMessage());
					// If there is an error, don't just keep trying to register.
					// Require the user to click a button again, or perform
					// exponential back-off.
				}
				return null;
			}
		}.execute(null, null, null);
    }
	
	void startApp() {
		// Check whether preference exists. if true, directly go to main activity.
		Intent intent;
		if( getSharedPreferences("appPref", 0).getBoolean("HasPreference", false) ) {
			intent = new Intent(LoginActivity.this, MainActivity.class);  
		}
		else {
			intent = new Intent(LoginActivity.this, IntroActivity.class);
		}
		startActivity(intent);
        finish();
	}
	
	public class doLogin extends AsyncTask<String, Void, String> {
		@Override
		protected String doInBackground(String... arg) {
			String email = new String(arg[0]);
			String password = new String(arg[1]);
				
			String serverURL = new Settings().getServerURL();
			String URL = serverURL + "login";
				
			DefaultHttpClient client = new DefaultHttpClient();
			try {
				// Make NameValuePair for password to server. use POST method.
				ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
				nameValuePairs.add(new BasicNameValuePair("useremail", email));
				nameValuePairs.add(new BasicNameValuePair("password", password));
				nameValuePairs.add(new BasicNameValuePair("regid", regId));
				
				// Make connection to server.
				HttpParams connectionParams = client.getParams();
				HttpConnectionParams.setConnectionTimeout(connectionParams, 5000);
				HttpConnectionParams.setSoTimeout(connectionParams, 5000);
				HttpPost httpPost = new HttpPost(URL);
				
				// Send NameValuePair to server.
				UrlEncodedFormEntity entityRequest = new UrlEncodedFormEntity(nameValuePairs, "EUC-KR");
				httpPost.setEntity(entityRequest);
				
				// Get response and parse entity.
				HttpResponse responsePost = client.execute(httpPost);
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
	}
}