package edu.chd.campusmap.model;

public class PathEdge {
    private int id;
    private int startNodeId;
    private int endNodeId;
    private double distanceMeters;
    private String roadName;

    public PathEdge() {}

    public PathEdge(int startNodeId, int endNodeId, double distanceMeters, String roadName) {
        this.startNodeId = startNodeId;
        this.endNodeId = endNodeId;
        this.distanceMeters = distanceMeters;
        this.roadName = roadName;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getStartNodeId() { return startNodeId; }
    public void setStartNodeId(int startNodeId) { this.startNodeId = startNodeId; }
    public int getEndNodeId() { return endNodeId; }
    public void setEndNodeId(int endNodeId) { this.endNodeId = endNodeId; }
    public double getDistanceMeters() { return distanceMeters; }
    public void setDistanceMeters(double distanceMeters) { this.distanceMeters = distanceMeters; }
    public String getRoadName() { return roadName; }
    public void setRoadName(String roadName) { this.roadName = roadName; }
}
