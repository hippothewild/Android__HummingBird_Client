package com.example.week04.adapter;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
	private static final int SCROLL_ACTION_THRESHOLD = 60;
	private static final int SCROLL_MAX_OFF_PATH = 200;
    private boolean isScrolling = false;
    private int scrollDistance = 0;
	
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
		final int pos = position;
		
		if(convertView == null){
			convertView = mInflater.inflate(mResource, null);
		}
		
		if(articleRow != null){
			Log.i("ArticleRowAdapter", "row loading...");
			TextView viewTitle = (TextView) convertView.findViewById(R.id.article_title);
			TextView viewTime = (TextView) convertView.findViewById(R.id.article_time);
			TextView viewContent = (TextView) convertView.findViewById(R.id.article_content);
			
			// Set contents into textViews.
			viewTitle.setText(articleRow.getTitle());
			viewTime.setText(articleRow.getNews() + ", " + articleRow.getDate());
			viewContent.setText(articleRow.getContent());		
		}
		
		// Set gesture listener on each row(swipe, click)
		View articleHolder = convertView.findViewById(R.id.article_holder);
		final ImageView scrapImage = (ImageView) convertView.findViewById(R.id.article_btn_scrap);
		final ImageView scrapDelete = (ImageView) convertView.findViewById(R.id.article_btn_remove);
		final GestureDetector gestureDetector = new GestureDetector(mContext, new MyGestureDetector(scrapImage, scrapDelete));
	    View.OnTouchListener gestureListener = new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
            	if (gestureDetector.onTouchEvent(event)) {
                    return true;
                }
				if (event.getAction() == MotionEvent.ACTION_UP) {
					if (isScrolling) {
						if (scrapImage.getVisibility() == View.VISIBLE) {
							// Insert article into scraped DB.
							mHelper = new DBHelper(mContext);
							SQLiteDatabase db = mHelper.getWritableDatabase();
							String query = "INSERT OR IGNORE INTO SCRAPS(KEYWORD, TITLE, NEWS, DATE, CONTENT, LINK) VALUES('"
									+ articleRow.getKeyword().replaceAll("'|&lt;|&quot;|&gt;", "''") + "', '"
									+ articleRow.getTitle().replaceAll("'|&lt;|&quot;|&gt;", "''") + "', '"
									+ articleRow.getNews().replaceAll("'|&lt;|&quot;|&gt;", "''") + "', '"
									+ articleRow.getDate().replaceAll("'|&lt;|&quot;|&gt;", "''") + "', '"
									+ articleRow.getContent().replaceAll("'|&lt;|&quot;|&gt;", "''") + "', '"
									+ articleRow.getLink().replaceAll("'|&lt;|&quot;|&gt;", "''") + "');";
							db.execSQL(query);
							mHelper.close();
							
							// Make toast for user.
							Toast.makeText(mContext, "Scrap complete!", Toast.LENGTH_SHORT).show();
						}
						else if (scrapDelete.getVisibility() == View.VISIBLE) {
							AlertDialog.Builder alert = new AlertDialog.Builder(mContext);

							alert.setTitle("Alert!");
							alert.setMessage("Really delete this article?");

							alert.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {
									// Make row invisible in DB.
									mHelper = new DBHelper(mContext);
									SQLiteDatabase db = mHelper.getWritableDatabase();
									String query = "UPDATE ARTICLES SET VISIBLE = 0 WHERE ID = " + articleRow.getId() + ";";
									db.execSQL(query);
									mHelper.close();
									
									// Delete row from listview.
									mList.remove(pos);
									notifyDataSetChanged();
								}
							});
							alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {
									// nothing hpppens.
								}
							});
							
							alert.show();
						}
						scrapImage.setVisibility(View.GONE);
						scrapDelete.setVisibility(View.GONE);
						isScrolling = false;
						scrollDistance = 0;
					} else {
						// Touch event.
						Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(articleRow.getLink()));
						mContext.startActivity(intent);
						isScrolling = false;
						scrollDistance = 0;
					}
				}
				return false;
			}
        };
        articleHolder.setOnTouchListener(gestureListener);
		
		return convertView;
	}
	
	class MyGestureDetector extends SimpleOnGestureListener {
		ImageView myScrapView;
		ImageView myDeleteView;
		float firstX;
		
		public MyGestureDetector(ImageView scrapView, ImageView deleteView)
		{
			myScrapView = scrapView;
			myDeleteView = deleteView;
			firstX = 0;
		}
		
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            try {
            	isScrolling = true;
                scrollDistance = (int) (e1.getX() - e2.getX());
            	if (Math.abs(e1.getY() - e2.getY()) > SCROLL_MAX_OFF_PATH) {
                    return true;
            	}
            	
            	if(scrollDistance > SCROLL_ACTION_THRESHOLD) {
            		myScrapView.setVisibility(View.GONE);
            		myDeleteView.setVisibility(View.VISIBLE);
            	}
            	else if(scrollDistance < -SCROLL_ACTION_THRESHOLD) {
            		myScrapView.setVisibility(View.VISIBLE);
            		myDeleteView.setVisibility(View.GONE);
            	}
            	else {
            		myScrapView.setVisibility(View.GONE);
            		myDeleteView.setVisibility(View.GONE);
            	}
      
            } catch (Exception e) {
                // nothing
            }
            return false;
        }

        @Override
        public boolean onDown(MotionEvent e) {
        	firstX = e.getX();
            return true;
        }
    }

}
