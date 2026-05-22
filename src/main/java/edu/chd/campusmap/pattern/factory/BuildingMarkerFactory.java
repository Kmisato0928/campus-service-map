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
            return "#7EC8E3";   // 浅蓝色
        }
    }

    private static class CanteenMarker implements BuildingMarker {
        @Override
        public String getColorHex() {
            return "#81C784";   // 浅绿色
        }
    }

    private static class LibraryMarker implements BuildingMarker {
        @Override
        public String getColorHex() {
            return "#CE93D8";   // 浅紫色
        }
    }

    private static class DormMarker implements BuildingMarker {
        @Override
        public String getColorHex() {
            return "#F48FB1";   // 浅粉色
        }
    }

    private static class DefaultMarker implements BuildingMarker {
        @Override
        public String getColorHex() {
            return "#B0BEC5";   // 浅灰色
        }
    }
}
