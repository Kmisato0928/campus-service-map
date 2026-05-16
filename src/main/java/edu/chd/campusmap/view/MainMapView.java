package edu.chd.campusmap.view;

import edu.chd.campusmap.controller.MapController;
import edu.chd.campusmap.model.Building;
import edu.chd.campusmap.pattern.factory.BuildingMarkerFactory;
import edu.chd.campusmap.service.NavigationService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import java.util.List;
import java.util.function.BiConsumer;

public class MainMapView {
    private final BorderPane root;
    private final MapController mapController;
    private final CanvasMapRenderer mapRenderer;
    private final NavigationService navigationService;
    private final List<Building> allBuildings;
    private Label statusLabel;
    private Label statusMetaLabel;
    private javafx.scene.control.ProgressBar progressBar;
    private final Button searchPanelBtn = new Button("搜索");
    private final Button detailPanelBtn = new Button("详情");
    private final Button routePanelBtn = new Button("路线");
    private final Button panelToggleBtn = new Button("收起面板");
    private final Button userCenterBtn = new Button("个人中心");
    private final Button logoutBtn = new Button("退出");
    private final Label userNameLabel = new Label("当前用户");

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
        root.getStyleClass().add("main-map-root");
        root.setMinHeight(0); // 确保 BorderPane 在 StackPane 中不被居中裁切

        statusLabel = new Label("就绪 | 渭水校区");
        statusLabel.getStyleClass().add("top-status-text");
        statusMetaLabel = new Label("可搜索 " + allBuildings.size() + " 个地点，支持搜索、详情与路线规划");
        statusMetaLabel.getStyleClass().add("top-status-meta");
        userNameLabel.getStyleClass().add("top-user-chip");

        progressBar = new javafx.scene.control.ProgressBar(0);
        progressBar.setVisible(false);
        progressBar.setPrefWidth(96);
        progressBar.getStyleClass().add("status-progress");

        VBox topBar = createTopBar();
        root.setTop(topBar);
        root.setCenter(createMapArea());
        root.setBottom(null);

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
            statusMetaLabel.setText("当前没有可展示的校园地点数据");
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
            statusMetaLabel.setText("类别：" + building.getCategory() + " | 可继续查看详情、收藏或规划到此路线");
            mapRenderer.selectMarker(building.getId());
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
        String displayName = (name == null || name.isBlank()) ? "当前用户" : name;
        userNameLabel.setText(displayName);
    }

    public void setUserControlsVisible(boolean visible) {
        panelToggleBtn.setVisible(visible);
        searchPanelBtn.setVisible(visible);
        detailPanelBtn.setVisible(visible);
        routePanelBtn.setVisible(visible);
        userCenterBtn.setVisible(visible);
        logoutBtn.setVisible(visible);
    }

    public void setSidePanelCollapsed(boolean collapsed) {
        panelToggleBtn.setText(collapsed ? "展开面板" : "收起面板");
    }

    public Button getPanelToggleBtn() {
        return panelToggleBtn;
    }

    public Button getSearchPanelBtn() {
        return searchPanelBtn;
    }

    public Button getDetailPanelBtn() {
        return detailPanelBtn;
    }

    public Button getRoutePanelBtn() {
        return routePanelBtn;
    }

    public Button getUserCenterBtn() {
        return userCenterBtn;
    }

    public Button getLogoutBtn() {
        return logoutBtn;
    }

    public void setActivePanel(String panelKey) {
        updatePrimaryNavButton(searchPanelBtn, "search".equals(panelKey));
        updatePrimaryNavButton(detailPanelBtn, "detail".equals(panelKey));
        updatePrimaryNavButton(routePanelBtn, "route".equals(panelKey));
    }

    private void startOsmImport() {
        edu.chd.campusmap.util.OverpassImporter.setOnProgress(msg -> {
            javafx.application.Platform.runLater(() -> {
                statusLabel.setText(msg);
                if (!progressBar.isVisible()) {
                    progressBar.setVisible(true);
                    progressBar.setProgress(-1); // Indeterminate
                }
            });
        });
        edu.chd.campusmap.util.OverpassImporter.setOnComplete(() -> {
            javafx.application.Platform.runLater(() -> {
                navigationService.reloadGraph();
                statusLabel.setText("路网数据已就绪");
                progressBar.setVisible(false);
            });
        });
        new Thread(() -> {
            edu.chd.campusmap.util.OverpassImporter.importIfEmpty();
        }, "osm-importer").start();
    }

    private VBox createTopBar() {
        Node logoNode = createBrandLogo();

        Label title = new Label("长安大学校园地图");
        title.getStyleClass().add("top-bar-title");

        HBox statusRow = new HBox(6, statusLabel, progressBar);
        statusRow.setAlignment(Pos.CENTER_LEFT);
        statusRow.getStyleClass().add("top-status-row");

        VBox brandCopy = new VBox(2, title, statusRow, statusMetaLabel);
        brandCopy.getStyleClass().add("top-bar-brand");
        brandCopy.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(brandCopy, Priority.ALWAYS);

        HBox brandBlock = new HBox(10, logoNode, brandCopy);
        brandBlock.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(brandBlock, Priority.ALWAYS);

        panelToggleBtn.getStyleClass().addAll("button", "panel-toggle-btn");
        searchPanelBtn.getStyleClass().addAll("button", "top-action-btn");
        detailPanelBtn.getStyleClass().addAll("button", "top-action-btn");
        routePanelBtn.getStyleClass().addAll("button", "top-action-btn");
        userCenterBtn.getStyleClass().addAll("button", "top-primary-btn");
        logoutBtn.getStyleClass().addAll("button", "top-logout-btn");
        panelToggleBtn.setVisible(false);
        searchPanelBtn.setVisible(false);
        detailPanelBtn.setVisible(false);
        routePanelBtn.setVisible(false);
        userCenterBtn.setVisible(false);
        logoutBtn.setVisible(false);

        FlowPane actionRow = new FlowPane();
        actionRow.getChildren().addAll(panelToggleBtn, searchPanelBtn, routePanelBtn, userCenterBtn, logoutBtn);
        actionRow.getStyleClass().add("top-action-flow");
        actionRow.setHgap(10);
        actionRow.setVgap(8);
        actionRow.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(actionRow, Priority.ALWAYS);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topBar = new HBox(10, brandBlock, spacer, actionRow);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.getStyleClass().add("top-bar");

        VBox container = new VBox(topBar);
        container.getStyleClass().add("top-bar-shell");
        container.setMinHeight(Region.USE_PREF_SIZE);
        setActivePanel("search");
        return container;
    }

    private Node createBrandLogo() {
        StackPane logoWrap = new StackPane();
        logoWrap.getStyleClass().add("top-brand-logo");

        try {
            var resource = getClass().getResource("/image/school-badge.png");
            if (resource != null) {
                ImageView logoImage = new ImageView(new Image(resource.toExternalForm(), true));
                logoImage.setFitWidth(120);
                logoImage.setFitHeight(60);
                logoImage.setPreserveRatio(true);
                logoImage.getStyleClass().add("top-brand-logo-image");
                logoWrap.getChildren().add(logoImage);
                return logoWrap;
            }
        } catch (Exception ignored) {
        }

        Label fallback = new Label("CHD");
        fallback.getStyleClass().add("top-brand-logo-fallback");
        logoWrap.getChildren().add(fallback);
        return logoWrap;
    }

    private StackPane createMapArea() {
        StackPane mapFrame = new StackPane(mapRenderer);
        mapFrame.getStyleClass().add("map-frame");
        mapFrame.setMaxWidth(Double.MAX_VALUE);
        mapFrame.setMaxHeight(Double.MAX_VALUE);
        mapFrame.setMinHeight(0);

        Button zoomInBtn = new Button("+");
        zoomInBtn.getStyleClass().addAll("button", "map-control-btn");
        zoomInBtn.setTooltip(new Tooltip("放大"));
        zoomInBtn.setOnAction(e -> mapRenderer.setZoom(mapRenderer.getZoom() + 1));

        Button zoomOutBtn = new Button("-");
        zoomOutBtn.getStyleClass().addAll("button", "map-control-btn");
        zoomOutBtn.setTooltip(new Tooltip("缩小"));
        zoomOutBtn.setOnAction(e -> mapRenderer.setZoom(mapRenderer.getZoom() - 1));

        Button resetBtn = new Button("复位");
        resetBtn.getStyleClass().addAll("button", "map-control-btn", "map-reset-btn");
        resetBtn.setTooltip(new Tooltip("回到中心点"));
        resetBtn.setOnAction(e -> mapRenderer.resetView());

        HBox controlCard = new HBox(4, zoomInBtn, zoomOutBtn, resetBtn);
        controlCard.getStyleClass().add("map-control-card");
        controlCard.setMaxHeight(Region.USE_PREF_SIZE);
        controlCard.setMaxWidth(Region.USE_PREF_SIZE);
        StackPane.setAlignment(controlCard, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(controlCard, new Insets(0, 24, 32, 0));

        Label mapHint = new Label("拖拽移动 • 滚轮缩放");
        mapHint.getStyleClass().add("map-hint-chip");
        StackPane.setAlignment(mapHint, Pos.BOTTOM_LEFT);
        StackPane.setMargin(mapHint, new Insets(0, 0, 32, 24));

        StackPane mapArea = new StackPane(mapFrame, mapHint, controlCard);
        mapArea.getStyleClass().add("map-area");
        mapArea.setMinHeight(0);
        StackPane.setAlignment(mapFrame, Pos.TOP_LEFT);
        return mapArea;
    }

    private void updatePrimaryNavButton(Button button, boolean active) {
        if (active) {
            button.getStyleClass().setAll("button", "top-action-btn", "top-nav-btn-active");
        } else {
            button.getStyleClass().setAll("button", "top-action-btn", "top-nav-btn");
        }
    }

    public BorderPane getRoot() {
      return root;
    }
}
