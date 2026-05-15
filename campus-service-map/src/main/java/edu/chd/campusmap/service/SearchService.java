package edu.chd.campusmap.service;

import edu.chd.campusmap.dao.BuildingDAO;
import edu.chd.campusmap.model.Building;

import java.util.List;

public class SearchService {
    private final BuildingDAO buildingDAO;

    public SearchService() {
        this.buildingDAO = new BuildingDAO();
    }

    public List<Building> searchByName(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return buildingDAO.findAll();
        }
        return buildingDAO.searchByName(keyword.trim());
    }

    public List<Building> findByCategory(String category) {
        if (category == null || category.isEmpty() || "ALL".equals(category)) {
            return buildingDAO.findAll();
        }
        return buildingDAO.findByCategory(category);
    }

    public List<Building> search(String keyword, String category) {
        List<Building> result = searchByName(keyword);
        if (category != null && !category.isEmpty() && !"ALL".equals(category)) {
            result.removeIf(b -> !category.equals(b.getCategory()));
        }
        return result;
    }
}
