package com.example.week04.info;

public class KeywordRowInfo {
	private String keyword;
	private String lastUpdate;
	private int notifyNumber;
	private boolean isButtonVisible;
	
	public String getKeyword() {
		return this.keyword;
	}
	
	public String getLastUpdate() {
		return this.lastUpdate;
	}
	
	public int getNotifyNumber() {
		return this.notifyNumber;
	}
	
	public boolean isButtonVisible() {
		return this.isButtonVisible;
	}
	
	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}
	
	public void setLastUpdate(String lastUpdate) {
		this.lastUpdate = lastUpdate;
	}
	
	public void setNotifyNumber(int notifyNumber) {
		this.notifyNumber = notifyNumber;
	}
	
	public void setButtonVisible(boolean isButtonVisible) {
		this.isButtonVisible = isButtonVisible;
	}
}
