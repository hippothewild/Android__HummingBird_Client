package com.example.week04;

import java.util.ArrayList;

import com.example.week04.adapter.ArticleRowAdapter;
import com.example.week04.info.DBHelper;
import com.example.week04.info.ArticleRowInfo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;

public class ArticleActivity extends Activity {
	
	private ArticleRowAdapter rowAdapter;
	private ArrayList<ArticleRowInfo> articleList;
	private ListView articleListView;
	private DBHelper mHelper;
	
	private String keyword;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_article);
		
		// Get keyword from Bundle.
		Bundle b = getIntent().getExtras();
		keyword = b.getString("keyword");
		
		// Set listview of keywords.
		articleListView = (ListView)findViewById(R.id.article_list);
		articleList = new ArrayList<ArticleRowInfo>();
		rowAdapter = new ArticleRowAdapter(this, R.layout.article_row, articleList);
		articleListView.setAdapter(rowAdapter);
		
		// Update information of keyword list.
		setArticleList();
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
					Intent intent = new Intent(ArticleActivity.this, LoginActivity.class);
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
        	Toast.makeText(ArticleActivity.this, "Setting not implemented!", Toast.LENGTH_SHORT).show();
        	break;
        default:
            return super.onOptionsItemSelected(item);
	    }
	    return true;
	}
	
	private void setArticleList()  {
		articleList.clear();
		
		mHelper = new DBHelper(this);
		SQLiteDatabase db = mHelper.getReadableDatabase();
		Cursor cursor = db.rawQuery("SELECT ID, TITLE, NEWS, DATE, CONTENT, LINK FROM ARTICLES \r"
								  + "WHERE KEYWORD = '" + keyword + "' AND VISIBLE = 1 ORDER BY ID DESC;", null);
		
		while(cursor.moveToNext()) {
			ArticleRowInfo newRow = new ArticleRowInfo();
			int id = cursor.getInt(0);
			String title = cursor.getString(1);
			String news = cursor.getString(2);
			String date = cursor.getString(3);
			String content = cursor.getString(4);
			String link = cursor.getString(5);
			
			newRow.setId(id);
			newRow.setTitle(title);
			newRow.setNews(news);
			newRow.setDate(date);
			newRow.setContent(content);
			newRow.setLink(link);
			newRow.setKeyword(keyword);

			articleList.add(newRow);
		}
		
		mHelper.close();
		rowAdapter.notifyDataSetChanged();
	}
}
