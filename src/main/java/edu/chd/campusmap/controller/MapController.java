package edu.chd.campusmap.controller;

import edu.chd.campusmap.model.Building;
import edu.chd.campusmap.service.MapService;

import java.util.ArrayList;
import java.util.List;

public class MapController {
    private final MapService mapService;
    private final List<MapObserver> observers;

    public interface MapObserver {
        void onBuildingSelected(Building building);
    }

    public MapController() {
        this.mapService = new MapService();
        this.observers = new ArrayList<>();
    }

    public MapService getMapService() {
        return mapService;
    }

    public void addObserver(MapObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(MapObserver observer) {
        observers.remove(observer);
    }

    public void selectBuilding(Building building) {
        for (MapObserver observer : observers) {
            observer.onBuildingSelected(building);
        }
    }
}
