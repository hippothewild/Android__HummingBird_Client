package com.example.week04;

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
import org.json.JSONObject;

import com.example.week04.info.Settings;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class RegisterActivity extends Activity {
		
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_register);
		
		final EditText regEmail = (EditText)findViewById(R.id.reg_email);
		final EditText regPassword = (EditText)findViewById(R.id.reg_password);
		Button regButton = (Button) findViewById(R.id.btnRegister);
		TextView loginView = (TextView) findViewById(R.id.link_to_login);
		
		regButton.setOnClickListener(new View.OnClickListener() {
	    	@Override
	    	public void onClick(View v) {
	    		
	    		String emailInput = regEmail.getText().toString();
	    		String passInput = regPassword.getText().toString();
	    		
	    		try
	    		{
	        		String result = new doRegister().execute(emailInput, passInput).get();
	        		if( result.equals("success") ) {
	        			Toast.makeText(RegisterActivity.this, "Register success!", Toast.LENGTH_SHORT).show();
	        			Intent intent = new Intent(RegisterActivity.this, LoginActivity.class); 
	        			startActivity(intent);
	        	        finish();
					}
					else {
						Toast.makeText(RegisterActivity.this, "Register failed : " + result, Toast.LENGTH_SHORT).show();
					}
	    		} catch(Exception e) {
	    			e.printStackTrace();
	    		}
	    	}
	    });
	    
	    loginView.setOnClickListener(new View.OnClickListener() {
	    	@Override
	    	public void onClick(View v) {
	            finish();
	    	}
	    });
	}
	
	private class doRegister extends AsyncTask<String, Void, String> {
		@Override
    	protected String doInBackground(String... arg) {
    		String email = new String(arg[0]);
    		String password = new String(arg[1]);

    		String serverURL = new Settings().getServerURL();
    		String URL = serverURL + "join";
    		
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
}