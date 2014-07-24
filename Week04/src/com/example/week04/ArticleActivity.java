package com.example.week04;

import java.util.ArrayList;

import com.example.week04.adapter.ArticleRowAdapter;
import com.example.week04.info.DBHelper;
import com.example.week04.info.ArticleRowInfo;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.ListView;

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
	
	private void setArticleList()  {
		articleList.clear();
		
		mHelper = new DBHelper(this);
		SQLiteDatabase db = mHelper.getReadableDatabase();
		Cursor cursor = db.rawQuery("SELECT ID, TITLE, NEWS, DATE, CONTENT, LINK FROM ARTICLES WHERE KEYWORD = '" + keyword + "';", null);
		
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
