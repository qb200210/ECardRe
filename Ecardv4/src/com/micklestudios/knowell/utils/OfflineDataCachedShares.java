package com.micklestudios.knowell.utils;

public class OfflineDataCachedShares {
	private int stored;
	private String partyA;
	private String partyB;
	
	public OfflineDataCachedShares(){}
	
	public OfflineDataCachedShares(String partyA, String partyB){
		super();
		this.stored = 0;
		this.partyA = partyA;
		this.partyB = partyB;
	}		

	public int getStored() {
		return stored;
	}

	public void setStored(int stored) {
		this.stored = stored;
	}

	public String getPartyA() {
		return partyA;
	}

	public void setPartyA(String partyA) {
		this.partyA = partyA;
	}

	public String getPartyB() {
		return partyB;
	}

	public void setPartyB(String partyB) {
		this.partyB = partyB;
	}

	@Override
	public String toString(){
		return "Data [stored=" + stored + ", partyA=" + partyA + ", partyB=" + partyB; 
	}
	
}


