package edu.chd.campusmap.view;

import edu.chd.campusmap.controller.MapController;
import edu.chd.campusmap.model.Building;
import edu.chd.campusmap.pattern.factory.BuildingMarkerFactory;
import edu.chd.campusmap.service.NavigationService;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.util.List;
import java.util.function.BiConsumer;

public class MainMapView {
    private final BorderPane root;
    private final MapController mapController;
    private final CanvasMapRenderer mapRenderer;
    private final NavigationService navigationService;
    private final List<Building> allBuildings;
    private Label statusLabel;
    private final Button userCenterBtn = new Button("个人中心");
    private final Button logoutBtn = new Button("退出");
    private final Label userNameLabel = new Label();

    public MainMapView(MapController mapController, int userId) {
        this.mapController = mapController;
        this.root = new BorderPane();
        this.allBuildings = userId > 0
            ? mapController.getMapService().getAllBuildingsForUser(userId)
                : mapController.getMapService().getAllBuildings();
        this.mapRenderer = new CanvasMapRenderer();
        this.navigationService = new NavigationService();
        startOsmImport();
        initUI();
     registerObserver();
        addAllMarkers();
    }

    private void initUI() {
        statusLabel = new Label("就绪 | 长安大学渭水校区");
        statusLabel.setStyle("-fx-background-color: #f0f0f0; -fx-padding: 4 10;");

        VBox topBar = createTopBar();
        root.setTop(topBar);
        root.setCenter(mapRenderer);
        root.setBottom(statusLabel);

     mapRenderer.setOnMarkerClick(marker -> {
            Building building = findBuildingById(marker.id);
         if (building != null) {
                System.out.println("[Canvas] Marker clicked: " + building.getName());
                mapController.selectBuilding(building);
            }
        });
    }

    private void addAllMarkers() {
        if (allBuildings.isEmpty()) {
            statusLabel.setText("暂无建筑数据，请检查数据库连接");
          return;
        }
      for (Building b : allBuildings) {
            String colorHex = BuildingMarkerFactory.getColorHex(b.getCategory());
            Color color = Color.web(colorHex);
            mapRenderer.addMarker(b.getId(), b.getName(), b.getLatitude(), b.getLongitude(), b.getCategory(), color);
        }
    }

    private void registerObserver() {
        mapController.addObserver(building -> {
            statusLabel.setText("已选择: " + building.getName());
            // 不再自动瞬移，由用户通过"定位"按钮平滑移动
        });
    }

    private Building findBuildingById(int id) {
        for (Building b : allBuildings) {
       if (b.getId() == id) return b;
        }
    return null;
    }

    public void updateBuildingInList(Building building) {
     for (int i = 0; i < allBuildings.size(); i++) {
        if (allBuildings.get(i).getId() == building.getId()) {
           allBuildings.set(i, building);
                break;
            }
        }
    }

    public void updateMarkerOnMap(Building building) {
      mapRenderer.clearMarkers();
        addAllMarkers();
    }

    public void enterDragMode(int buildingId, BiConsumer<Double, Double> onDrag) {
        mapRenderer.enterDragMode(buildingId, onDrag);
    }

    public void exitDragMode() {
        mapRenderer.exitDragMode();
    }

    public void animateCenter(double lat, double lon) {
        mapRenderer.animateCenter(lat, lon);
    }

    public List<double[]> calculateRoute(Building from, Building to) {
        List<edu.chd.campusmap.model.PathNode> path = navigationService.findPath(from.getId(), to.getId());
        if (path.isEmpty()) return java.util.Collections.emptyList();
        List<double[]> latLonPath = new java.util.ArrayList<>();
        for (edu.chd.campusmap.model.PathNode n : path) {
            latLonPath.add(new double[]{n.getLat(), n.getLon()});
        }
        return latLonPath;
    }

    public double calculateRouteDistance(List<double[]> path) {
        double total = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            double[] a = path.get(i);
            double[] b = path.get(i + 1);
            total += haversine(a[0], a[1], b[0], b[1]);
        }
        return total;
    }

    private static double haversine(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    public void showNavigationPath(List<double[]> path) {
        mapRenderer.setNavigationPath(path);
    }

    public void clearNavigation() {
        mapRenderer.clearNavigationPath();
    }

    public NavigationService getNavigationService() {
        return navigationService;
    }

    public void updateStatus(String msg) {
        statusLabel.setText(msg);
    }

    public void setUserName(String name) {
        userNameLabel.setText(" | " + name);
    }

    public void setUserControlsVisible(boolean visible) {
        userCenterBtn.setVisible(visible);
        logoutBtn.setVisible(visible);
    }

    public Button getUserCenterBtn() {
        return userCenterBtn;
    }

    public Button getLogoutBtn() {
        return logoutBtn;
    }

    private void startOsmImport() {
        edu.chd.campusmap.util.OverpassImporter.setOnProgress(msg -> statusLabel.setText(msg));
        edu.chd.campusmap.util.OverpassImporter.setOnComplete(() -> {
            navigationService.reloadGraph();
            statusLabel.setText("路网数据已加载，导航可使用真实道路");
        });
        new Thread(() -> {
            edu.chd.campusmap.util.OverpassImporter.importIfEmpty();
        }, "osm-importer").start();
    }

    private VBox createTopBar() {
        Label title = new Label("长安大学校园服务地图系统");
        title.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 18));
        title.setStyle("-fx-text-fill: #1a5276;");

      userNameLabel.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 14;");

        userCenterBtn.setStyle("-fx-background-color: #2e86c1; -fx-text-fill: white;");
        logoutBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
    userCenterBtn.setVisible(false);
        logoutBtn.setVisible(false);

        HBox topBar = new HBox(10, title, userNameLabel);
        topBar.setPadding(new Insets(10, 15, 10, 15));
    topBar.setStyle("-fx-background-color: #d4e6f1; -fx-border-color: #aed6f1; -fx-border-width: 0 0 1 0;");

        HBox rightBar = new HBox(8, userCenterBtn, logoutBtn);
        rightBar.setStyle("-fx-alignment: center-right;");
        HBox.setHgrow(rightBar, javafx.scene.layout.Priority.ALWAYS);
        topBar.getChildren().add(rightBar);

        VBox container = new VBox(topBar);
        return container;
    }

    public BorderPane getRoot() {
      return root;
    }
}
