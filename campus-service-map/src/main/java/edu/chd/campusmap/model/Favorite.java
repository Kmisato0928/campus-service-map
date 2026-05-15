package edu.chd.campusmap.model;

import java.time.LocalDateTime;

public class Favorite {
    private int id;
    private int userId;
    private int buildingId;
    private LocalDateTime createdAt;

    private String buildingName;
    private String buildingCategory;

    public Favorite() {
    }

    public Favorite(int userId, int buildingId) {
        this.userId = userId;
        this.buildingId = buildingId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getBuildingId() { return buildingId; }
    public void setBuildingId(int buildingId) { this.buildingId = buildingId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getBuildingName() { return buildingName; }
    public void setBuildingName(String buildingName) { this.buildingName = buildingName; }

    public String getBuildingCategory() { return buildingCategory; }
    public void setBuildingCategory(String buildingCategory) { this.buildingCategory = buildingCategory; }
}
