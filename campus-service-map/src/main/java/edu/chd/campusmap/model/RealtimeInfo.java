package edu.chd.campusmap.model;

public class RealtimeInfo {
    private int id;
    private int buildingId;
    private String infoType;
    private String status;
    private String timeSlot;
    private int dayOfWeek;

    public RealtimeInfo() {
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getBuildingId() { return buildingId; }
    public void setBuildingId(int buildingId) { this.buildingId = buildingId; }

    public String getInfoType() { return infoType; }
    public void setInfoType(String infoType) { this.infoType = infoType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTimeSlot() { return timeSlot; }
    public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }

    public int getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(int dayOfWeek) { this.dayOfWeek = dayOfWeek; }
}
