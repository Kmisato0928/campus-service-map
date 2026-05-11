package edu.chd.campusmap.view;

import edu.chd.campusmap.controller.MapController;
import edu.chd.campusmap.model.Building;
import edu.chd.campusmap.pattern.factory.BuildingMarkerFactory;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.BiConsumer;

public class MainMapView {
    private final BorderPane root;
    private final MapController mapController;
    private final WebView webView;
    private final List<Building> allBuildings;
    private Label statusLabel;
    private boolean mapLoaded = false;
    private final Button userCenterBtn = new Button("个人中心");
    private final Button logoutBtn = new Button("退出");
    private final Label userNameLabel = new Label();
    private final JavaBridge javaBridge = new JavaBridge();
    private BiConsumer<Double, Double> dragPositionCallback;

    public MainMapView(MapController mapController, int userId) {
        this.mapController = mapController;
        this.webView = new WebView();
        this.root = new BorderPane();
        this.allBuildings = userId > 0
                ? mapController.getMapService().getAllBuildingsForUser(userId)
                : mapController.getMapService().getAllBuildings();
        initUI();
        registerObserver();
    }

    private void initUI() {
        var engine = webView.getEngine();
        webView.setContextMenuEnabled(false);

        engine.setOnAlert(event ->
            System.out.println("[WebView] " + event.getData()));

        String html = buildMapHtml();
        engine.loadContent(html);

        engine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            if (state == Worker.State.SUCCEEDED && !mapLoaded) {
                mapLoaded = true;
                Platform.runLater(() -> {
                    try {
                        JSObject window = (JSObject) engine.executeScript("window");
                        window.setMember("javaBridge", javaBridge);
                        addAllMarkers(window);
                    } catch (Exception e) {
                        System.err.println("[WebView] 地图初始化失败: " + e.getMessage());
                    }
                });
            }
        });

        statusLabel = new Label("就绪 | 长安大学渭水校区");
        statusLabel.setStyle("-fx-background-color: #f0f0f0; -fx-padding: 4 10;");

        VBox topBar = createTopBar();
        root.setTop(topBar);
        root.setCenter(webView);
        root.setBottom(statusLabel);
    }

    private String buildMapHtml() {
        String leafletCss = loadResource("/map/leaflet.css");
        String leafletJs = loadResource("/map/leaflet.js");

        return "<!DOCTYPE html>\n" +
               "<html>\n<head>\n" +
               "<meta charset='UTF-8'>\n" +
               "<meta name='viewport' content='width=device-width, initial-scale=1.0'>\n" +
               "<title>Campus Map</title>\n" +
               "<style>\n" +
               leafletCss + "\n" +
               "body { margin: 0; padding: 0; }\n" +
               "#map { width: 100%; height: 100vh; }\n" +
               ".building-popup .popup-name { font-weight: bold; font-size: 14px; }\n" +
               ".building-popup .popup-category { font-size: 12px; color: #666; }\n" +
               "</style>\n</head>\n<body>\n" +
               "<div id='map'></div>\n" +
               "<div id='drag-hint' style='display:none;position:absolute;top:10px;left:50%;transform:translateX(-50%);background:rgba(231,76,60,0.9);color:white;padding:8px 18px;border-radius:6px;z-index:1000;font-size:14px;box-shadow:0 2px 10px rgba(0,0,0,0.3);pointer-events:none;white-space:nowrap;'>拖动红色标记调整位置，坐标将自动更新到编辑框</div>\n" +
               "<script>\n" +
               leafletJs + "\n" +
               "var map = L.map('map', {\n" +
               "  center:[34.3740,108.9100], zoom:15,\n" +
               "  zoomControl:true\n" +
               "});\n" +
               "L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {\n" +
               "  maxZoom:19, attribution:'&copy; OpenStreetMap'\n" +
               "}).addTo(map);\n" +
               "var markers = {};\n" +
               "var selectedMarker = null;\n" +
               "var dragMarkerId = null;\n" +
               "var catNames = {TEACHING:'教学楼',CANTEEN:'餐厅',LIBRARY:'图书馆',DORM:'宿舍',OTHER:'其他'};\n" +
               "function addBuilding(id, name, lat, lng, c, color) {\n" +
               "  var icon = L.divIcon({\n" +
               "    className: 'custom-marker',\n" +
               "    html: '<div style=\"background:'+color+';width:16px;height:16px;border-radius:50%;border:3px solid white;box-shadow:0 1px 4px rgba(0,0,0,0.4);\"></div>',\n" +
               "    iconSize: [22,22],\n" +
               "    iconAnchor: [11,11],\n" +
               "    popupAnchor: [0,-11]\n" +
               "  });\n" +
               "  var marker = L.marker([lat, lng], {icon: icon}).addTo(map);\n" +
               "  marker.bindPopup('<div class=\"building-popup\"><div class=\"popup-name\">'+name+'</div><div class=\"popup-category\">'+catNames[c]+'</div></div>');\n" +
               "  (function(bid){ marker.on('click',function(){ if(window.javaBridge) window.javaBridge.onBuildingSelected(bid); }); })(id);\n" +
               "  markers[id] = marker;\n" +
               "}\n" +
               "function updateBuildingMarker(id, lat, lng, name, c, color) {\n" +
               "  if(markers[id]) { map.removeLayer(markers[id]); }\n" +
               "  addBuilding(id, name, lat, lng, c, color);\n" +
               "}\n" +
               "function selectBuilding(id) {\n" +
               "  if(selectedMarker) selectedMarker.closePopup();\n" +
               "  if(markers[id]){ selectedMarker=markers[id]; selectedMarker.openPopup(); map.setView(selectedMarker.getLatLng(), map.getZoom(), {animate:true}); }\n" +
               "}\n" +
               "function enableDrag(buildingId) {\n" +
               "  if(!markers[buildingId]) return;\n" +
               "  disableDrag();\n" +
               "  dragMarkerId = buildingId;\n" +
               "  var m = markers[buildingId];\n" +
               "  m._origIcon = m.options.icon;\n" +
               "  m.dragging.enable();\n" +
               "  m.setIcon(L.divIcon({className:'custom-marker',html:'<div style=\"background:#e74c3c;width:20px;height:20px;border:3px solid #fff;border-radius:50%;box-shadow:0 0 12px rgba(231,76,60,0.8);\"></div>',iconSize:[20,20],iconAnchor:[10,10]}));\n" +
               "  m.setZIndexOffset(10000);\n" +
               "  m.on('dragend',function(){\n" +
               "    var p=m.getLatLng();\n" +
               "    if(window.javaBridge) window.javaBridge.onMarkerDrag(buildingId,p.lat,p.lng);\n" +
               "  });\n" +
               "  document.getElementById('drag-hint').style.display='block';\n" +
               "}\n" +
               "function disableDrag() {\n" +
               "  if(dragMarkerId && markers[dragMarkerId]){\n" +
               "    var m=markers[dragMarkerId];\n" +
               "    m.dragging.disable();\n" +
               "    m.off('dragend');\n" +
               "    if(m._origIcon) m.setIcon(m._origIcon);\n" +
               "    m.setZIndexOffset(0);\n" +
               "  }\n" +
               "  dragMarkerId=null;\n" +
               "  document.getElementById('drag-hint').style.display='none';\n" +
               "}\n" +
               "</script>\n</body>\n</html>";
    }

    private String loadResource(String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) return "";
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    private void addAllMarkers(JSObject window) {
        if (allBuildings.isEmpty()) {
            statusLabel.setText("暂无建筑数据，请检查数据库连接");
            return;
        }
        for (Building b : allBuildings) {
            String color = BuildingMarkerFactory.getColorHex(b.getCategory());
            window.call("addBuilding", b.getId(), b.getName(),
                b.getLatitude(), b.getLongitude(), b.getCategory(), color);
        }
    }

    private void registerObserver() {
        mapController.addObserver(building -> {
            statusLabel.setText("已选择: " + building.getName());
            if (mapLoaded) {
                Platform.runLater(() ->
                    webView.getEngine().executeScript("selectBuilding(" + building.getId() + ")"));
            }
        });
    }

    private Building findBuildingById(int id) {
        for (Building b : allBuildings) {
            if (b.getId() == id) return b;
        }
        return null;
    }

    /**
     * 编辑后同步更新 allBuildings 列表中的建筑对象，使 findBuildingById 返回最新数据
     */
    public void updateBuildingInList(Building building) {
        for (int i = 0; i < allBuildings.size(); i++) {
            if (allBuildings.get(i).getId() == building.getId()) {
                allBuildings.set(i, building);
                break;
            }
        }
    }

    public void updateMarkerOnMap(Building building) {
        if (mapLoaded) {
            String color = BuildingMarkerFactory.getColorHex(building.getCategory());
            Platform.runLater(() -> {
                try {
                    webView.getEngine().executeScript(
                        "updateBuildingMarker(" + building.getId() + ","
                        + building.getLatitude() + ","
                        + building.getLongitude() + ",'"
                        + building.getName().replace("'", "\\'") + "','"
                        + building.getCategory() + "','" + color + "')");
                } catch (Exception e) {
                    System.err.println("[WebView] 更新标记失败: " + e.getMessage());
                }
            });
        }
    }

    public void enterDragMode(int buildingId, BiConsumer<Double, Double> onDrag) {
        this.dragPositionCallback = onDrag;
        if (!mapLoaded) return;
        Platform.runLater(() -> {
            try {
                webView.getEngine().executeScript("enableDrag(" + buildingId + ")");
            } catch (Exception e) {
                System.err.println("[WebView] 启动拖动模式失败: " + e.getMessage());
            }
        });
    }

    public void exitDragMode() {
        this.dragPositionCallback = null;
        if (!mapLoaded) return;
        Platform.runLater(() -> {
            try {
                webView.getEngine().executeScript("disableDrag()");
            } catch (Exception e) {
                System.err.println("[WebView] 退出拖动模式失败: " + e.getMessage());
            }
        });
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

    public class JavaBridge {
        public void onBuildingSelected(int buildingId) {
            System.out.println("[Bridge] Marker clicked, buildingId=" + buildingId);
            Building building = findBuildingById(buildingId);
            if (building != null) {
                System.out.println("[Bridge] Found: " + building.getName());
                mapController.selectBuilding(building);
            } else {
                System.out.println("[Bridge] Building NOT FOUND for id=" + buildingId);
            }
        }

        public void onMarkerDrag(int buildingId, double lat, double lng) {
            System.out.println("[Bridge] Marker dragged: id=" + buildingId + " lat=" + lat + " lng=" + lng);
            if (dragPositionCallback != null) {
                Platform.runLater(() -> dragPositionCallback.accept(lat, lng));
            }
        }
    }
}
