package edu.chd.campusmap.model;

public class PathNode {
    private int id;
    private double lat;
    private double lon;
    private String name;
    private Integer buildingId;

    public PathNode() {}

    public PathNode(double lat, double lon, String name, Integer buildingId) {
        this.lat = lat;
        this.lon = lon;
        this.name = name;
        this.buildingId = buildingId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public double getLat() { return lat; }
    public void setLat(double lat) { this.lat = lat; }
    public double getLon() { return lon; }
    public void setLon(double lon) { this.lon = lon; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getBuildingId() { return buildingId; }
    public void setBuildingId(Integer buildingId) { this.buildingId = buildingId; }
}
