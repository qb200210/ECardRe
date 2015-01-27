package com.warpspace.ecardv4.utils;

public class OfflineData {
	private int stored;
	private String ecardID;
	private String notePath;
	
	public OfflineData(){}
	
	public OfflineData(String ecardID, String notePath){
		super();
		this.stored = 0;
		this.ecardID = ecardID;
		this.notePath = notePath;
	}	
	
	public int getStored() {
		return stored;
	}

	public void setStored(int stored) {
		this.stored = stored;
	}

	public String getEcardID() {
		return ecardID;
	}

	public void setEcardID(String userID) {
		this.ecardID = userID;
	}

	public String getNotePath() {
		return notePath;
	}

	public void setNotePath(String notePath) {
		this.notePath = notePath;
	}

	@Override
	public String toString(){
		return "Data [stored=" + stored + ", ecardID=" + ecardID + ", notePath=" + notePath; 
	}
	
}


