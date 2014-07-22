package com.example.week04.info;

public class ArticleRowInfo {
	private int id;
	private String title;
	private String news;
	private String date;
	private String content;
	private String link;
	private String keyword;
	private boolean isButtonVisible;

	// Data viewing functions.
	public int getId() {
		return this.id;
	}
	
	public String getTitle() {
		return this.title;
	}
	
	public String getNews() {
		return this.news;
	}

	public String getDate() {
		return this.date;
	}

	public String getContent() {
		return this.content;
	}

	public String getLink() {
		return this.link;
	}

	public String getKeyword() {
		return this.keyword;
	}

	public boolean isButtonVisible() {
		return this.isButtonVisible;
	}
	
	// Data modification functions.
	public void setId(int id) {
		this.id = id;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	public void setNews(String news) {
		this.news = news;
	}
	
	public void setDate(String date) {
		this.date = date;
	}
	
	public void setContent(String content) {
		this.content = content;
	}
	
	public void setLink(String link) {
		this.link = link;
	}
	
	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}
	
	public void setButtonVisible(boolean isButtonVisible) {
		this.isButtonVisible = isButtonVisible;
	}
}
