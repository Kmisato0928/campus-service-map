package edu.chd.campusmap.controller;

import edu.chd.campusmap.model.Building;
import edu.chd.campusmap.service.SearchService;

import java.util.List;

public class SearchController {
    private final SearchService searchService;

    public SearchController() {
        this.searchService = new SearchService();
    }

    public List<Building> search(String keyword, String category) {
        return searchService.search(keyword, category);
    }

    public List<Building> searchByName(String keyword) {
        return searchService.searchByName(keyword);
    }

    public List<Building> findByCategory(String category) {
        return searchService.findByCategory(category);
    }
}
