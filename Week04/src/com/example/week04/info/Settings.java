package com.example.week04.info;

public class Settings {
	// Server URLS.
	private final String mainServerURL = "http://blooming-castle-2040.herokuapp.com/";
	private final String testServerURL = "http://nameless-everglades-4124.herokuapp.com/";
	private boolean isDebug = true;
	
	// Default preference when first loading app.
	private boolean CrawlOnlyInWifi = true;
	private int CrawlIntervalInMinutes = 1440;
	private int DayTimeStart = 7;
	private int DayTimeEnd = 23;
	private boolean NotifyOnlyInDaytime = true;
	
	public String getServerURL() {
		return (isDebug ? testServerURL : mainServerURL);
	}
	
	public boolean getCrawlStatus() {
		return CrawlOnlyInWifi;
	}
	
	public int getCrawlInterval() {
		return CrawlIntervalInMinutes;
	}
	
	public int getDayTimeStart() {
		return DayTimeStart;
	}
	
	public int getDayTimeEnd() {
		return DayTimeEnd;
	}
	
	public boolean getNotifyType() {
		return NotifyOnlyInDaytime;
	}
	public Settings() {
		
	}
}
