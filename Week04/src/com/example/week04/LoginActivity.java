package com.example.week04;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import com.example.week04.info.Settings;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class LoginActivity extends Activity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		// Check preference for autoLogin check. if there is. try to login.
		SharedPreferences appPref = getSharedPreferences("appPref", 0);
		String autoLoginId = appPref.getString("loginID", "");
		if( !autoLoginId.isEmpty() ) {
			String autoLoginPassword = appPref.getString("loginPassword", "");
			try {
				String loginResult = new doLogin().execute(autoLoginId, autoLoginPassword).get();
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
					// TODO :: connect to server and verify id and password.
					
					String loginResult = new doLogin().execute(emailInput, passInput).get();
					if( loginResult.equals("success") ) {
						if(checkBox.isChecked()) {
							setAutoLogin(emailInput, passInput);
						}
						
						startApp();
					}
					else {
						Toast.makeText(LoginActivity.this, loginResult, Toast.LENGTH_SHORT).show();
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
	
	private class doLogin extends AsyncTask<String, Void, String> {
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
	
	void setAutoLogin(final String id, final String password) {
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				SharedPreferences appPref = getSharedPreferences("appPref", 0);
				SharedPreferences.Editor edit = appPref.edit();
				edit.putString("loginID", id);
				edit.putString("loginPassword", password);
				edit.commit();

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
}
