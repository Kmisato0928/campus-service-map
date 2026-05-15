package edu.chd.campusmap.pattern.factory;

public class BuildingMarkerFactory {

    public static BuildingMarker getMarker(String category) {
        switch (category) {
            case "TEACHING":
                return new TeachingBuildingMarker();
            case "CANTEEN":
                return new CanteenMarker();
            case "LIBRARY":
                return new LibraryMarker();
            case "DORM":
                return new DormMarker();
            default:
                return new DefaultMarker();
        }
    }

    public static String getColorHex(String category) {
        return getMarker(category).getColorHex();
    }

    private static class TeachingBuildingMarker implements BuildingMarker {
        @Override
        public String getColorHex() {
            return "#4682B4";
        }
    }

    private static class CanteenMarker implements BuildingMarker {
        @Override
        public String getColorHex() {
            return "#DC5050";
        }
    }

    private static class LibraryMarker implements BuildingMarker {
        @Override
        public String getColorHex() {
            return "#3CB371";
        }
    }

    private static class DormMarker implements BuildingMarker {
        @Override
        public String getColorHex() {
            return "#DAA520";
        }
    }

    private static class DefaultMarker implements BuildingMarker {
        @Override
        public String getColorHex() {
            return "#808080";
        }
    }
}
