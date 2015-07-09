package com.micklestudios.knowell.utils;

public class OfflineDataCachedIds {
  private int stored;
  private String ecardID;
  private String whereMet;
  private String eventMet;
  private String notes;
  private String voiceNote;

  public OfflineDataCachedIds() {
  }

  public OfflineDataCachedIds(String ecardID, String whereMet, String eventMet,
    String notes, String voiceNote) {
    super();
    this.stored = 0;
    this.ecardID = ecardID;
    this.whereMet = whereMet;
    this.eventMet = eventMet;
    this.notes = notes;
    this.voiceNote = voiceNote;
  }

  public String getWhereMet() {
    return whereMet;
  }

  public void setWhereMet(String whereMet) {
    this.whereMet = whereMet;
  }

  public String getEventMet() {
    return eventMet;
  }

  public void setEventMet(String eventMet) {
    this.eventMet = eventMet;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public String getVoiceNote() {
    return voiceNote;
  }

  public void setVoiceNote(String voiceNote) {
    this.voiceNote = voiceNote;
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

  @Override
  public String toString() {
    return "Data [stored=" + stored + ", ecardID=" + ecardID + ", whereMet="
      + whereMet + ", eventMet=" + eventMet + ", notes=" + notes
      + ", voiceNote=" + voiceNote;
  }

}
