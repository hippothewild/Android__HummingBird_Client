package com.example.week04.adapter;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.example.week04.ArticleActivity;
import com.example.week04.R;
import com.example.week04.info.DBHelper;
import com.example.week04.info.ArticleRowInfo;

public class ArticleRowAdapter extends ArrayAdapter<ArticleRowInfo> {

	private Context mContext;
	private int mResource;
	private ArrayList<ArticleRowInfo> mList;
	private LayoutInflater mInflater;
	private DBHelper mHelper;
	
	// Touch event variables.
	private int action_down_x = 0;
	
	public ArticleRowAdapter(Context context, int resource, ArrayList<ArticleRowInfo> objects) {
		super(context, resource, objects);
		this.mContext = context;
		this.mResource = resource;
		this.mList = objects;
		this.mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent){
		final ArticleRowInfo articleRow = mList.get(position);

		if(convertView == null){
			convertView = mInflater.inflate(mResource, null);
		}
		
		final View tempView = convertView;
		final int pos = position;
		
		if(articleRow != null){
			Log.i("ArticleRowAdapter", "row loading...");
			TextView viewTitle = (TextView) convertView.findViewById(R.id.article_title);
			TextView viewTime = (TextView) convertView.findViewById(R.id.article_time);
			TextView viewContent = (TextView) convertView.findViewById(R.id.article_content);
			
			// Set contents into textViews.
			viewTitle.setText(articleRow.getTitle());
			viewTime.setText(articleRow.getNews() + ", " + articleRow.getDate());
			viewContent.setText(articleRow.getContent());		
			
			// Set remove button to remove items.
			TextView buttonRemove = (TextView) convertView.findViewById(R.id.article_btn_remove);
			if(articleRow.isButtonVisible()) {
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
					alert.setMessage("Really delete this article?");

					alert.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							// Remove item from DB.
							int deleteId = mList.get(pos).getId();
							mHelper = new DBHelper(mContext);
							SQLiteDatabase db = mHelper.getWritableDatabase();
							String query = "DELETE FROM ARTICLES WHERE ID=" + deleteId + ";";
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
		
		convertView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(articleRow.getLink()));
				mContext.startActivity(intent);
			}
		});

		return convertView;
	}
}
