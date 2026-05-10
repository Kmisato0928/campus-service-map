package edu.chd.campusmap.model;

/**
 * 用户个人建筑覆盖数据
 * 普通用户编辑建筑后，修改仅自己可见，存于此表
 */
public class BuildingOverride {
    private int id;
    private int userId;
    private int buildingId;
    private String name;
    private String category;
    private double latitude;
    private double longitude;
    private String description;

    public BuildingOverride() {
    }

    public BuildingOverride(int userId, int buildingId, String name, String category,
                            double latitude, double longitude, String description) {
        this.userId = userId;
        this.buildingId = buildingId;
        this.name = name;
        this.category = category;
        this.latitude = latitude;
        this.longitude = longitude;
        this.description = description;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getBuildingId() { return buildingId; }
    public void setBuildingId(int buildingId) { this.buildingId = buildingId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
