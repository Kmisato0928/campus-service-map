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
            return "#2F6FED";
        }
    }

    private static class CanteenMarker implements BuildingMarker {
        @Override
        public String getColorHex() {
            return "#5E84C9";
        }
    }

    private static class LibraryMarker implements BuildingMarker {
        @Override
        public String getColorHex() {
            return "#1F4F9E";
        }
    }

    private static class DormMarker implements BuildingMarker {
        @Override
        public String getColorHex() {
            return "#7A97C8";
        }
    }

    private static class DefaultMarker implements BuildingMarker {
        @Override
        public String getColorHex() {
            return "#94A8C7";
        }
    }
}
