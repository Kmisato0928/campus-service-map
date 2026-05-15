package edu.chd.campusmap.model;

public class Building {
    private int id;
    private String name;
    private String category;
    private double latitude;
    private double longitude;
    private String description;
    private String imageUrl;
    private boolean editedByAdmin;

    public Building() {
    }

    public Building(String name, String category, double latitude, double longitude, String description) {
        this.name = name;
        this.category = category;
        this.latitude = latitude;
        this.longitude = longitude;
        this.description = description;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

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

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public boolean isEditedByAdmin() { return editedByAdmin; }
    public void setEditedByAdmin(boolean editedByAdmin) { this.editedByAdmin = editedByAdmin; }
}
