package edu.chd.campusmap.view;

import edu.chd.campusmap.model.Building;
import edu.chd.campusmap.model.BuildingOverride;
import edu.chd.campusmap.service.MapService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.LinkedHashMap;
import java.util.Map;

public class EditBuildingDialog extends Stage {

    private static final Map<String, String> CATEGORIES = new LinkedHashMap<>();
    static {
        CATEGORIES.put("TEACHING", "教学楼");
        CATEGORIES.put("CANTEEN", "餐厅");
        CATEGORIES.put("LIBRARY", "图书馆");
        CATEGORIES.put("DORM", "宿舍");
        CATEGORIES.put("OTHER", "其他");
    }

    private final Building building;
    private final int userId;
    private final boolean isAdmin;
    private final MapService mapService;
    private final MainMapView mapView;
    private final TextField nameField;
    private final ComboBox<String> categoryBox;
    private final TextField latField;
    private final TextField lngField;
    private final TextArea descArea;
    private final Label msgLabel;
    private final Label roleHint;
    private Runnable onSaved;
    private boolean saved = false;

    public EditBuildingDialog(Building building, int userId, boolean isAdmin, MainMapView mapView) {
        this.building = building;
        this.userId = userId;
        this.isAdmin = isAdmin;
        this.mapView = mapView;
        this.mapService = new MapService();

        initModality(Modality.NONE);
        setTitle("编辑建筑信息 - " + building.getName());
        setMinWidth(420);
        setMinHeight(420);

        VBox root = new VBox(12);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #f8f9fa;");

        Label titleLabel = new Label("编辑建筑信息");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1a5276;");

        roleHint = new Label();
        roleHint.setStyle("-fx-font-size: 12; -fx-text-fill: " + (isAdmin ? "#27ae60" : "#e67e22") + ";");
        if (isAdmin) {
            roleHint.setText("管理员模式：修改将全局生效");
        } else {
            roleHint.setText("个人模式：修改仅自己可见");
        }

        // 拖动提示
        Label dragHint = new Label("提示：地图上的标记已变为红色可拖动状态，拖动后坐标自动更新");
        dragHint.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12; -fx-font-weight: bold; -fx-background-color: #fdedec; -fx-padding: 6 10; -fx-border-radius: 4; -fx-background-radius: 4;");

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(8);
        form.setPadding(new Insets(10, 0, 10, 0));

        form.add(new Label("名称:"), 0, 0);
        nameField = new TextField(building.getName());
        nameField.setPrefWidth(260);
        form.add(nameField, 1, 0);

        form.add(new Label("类别:"), 0, 1);
        categoryBox = new ComboBox<>();
        categoryBox.getItems().addAll(CATEGORIES.values());
        String currentCat = CATEGORIES.getOrDefault(building.getCategory(), "其他");
        categoryBox.setValue(currentCat);
        categoryBox.setPrefWidth(260);
        form.add(categoryBox, 1, 1);

        form.add(new Label("纬度:"), 0, 2);
        latField = new TextField(String.valueOf(building.getLatitude()));
        latField.setEditable(true);
        form.add(latField, 1, 2);

        form.add(new Label("经度:"), 0, 3);
        lngField = new TextField(String.valueOf(building.getLongitude()));
        lngField.setEditable(true);
        form.add(lngField, 1, 3);

        form.add(new Label("描述:"), 0, 4);
        descArea = new TextArea(building.getDescription());
        descArea.setPrefRowCount(3);
        descArea.setPrefWidth(260);
        form.add(descArea, 1, 4);

        Button saveBtn = new Button("保存");
        saveBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 14px;");
        saveBtn.setOnAction(e -> save());

        Button cancelBtn = new Button("取消");
        cancelBtn.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-size: 14px;");
        cancelBtn.setOnAction(e -> {
            mapView.exitDragMode();
            close();
        });

        HBox btnRow = new HBox(10, saveBtn, cancelBtn);
        btnRow.setAlignment(Pos.CENTER);

        msgLabel = new Label();
        msgLabel.setStyle("-fx-text-fill: #27ae60;");

        root.getChildren().addAll(titleLabel, roleHint, dragHint, form, btnRow, msgLabel);
        Scene scene = new Scene(root);
        setScene(scene);

        // 关闭时退出拖动模式
        setOnCloseRequest(e -> mapView.exitDragMode());

        // 进入拖动模式，接收坐标更新
        mapView.enterDragMode(building.getId(), (lat, lng) -> {
            Platform.runLater(() -> {
                latField.setText(String.format("%.6f", lat));
                lngField.setText(String.format("%.6f", lng));
            });
        });
    }

    private void save() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            msgLabel.setStyle("-fx-text-fill: #e74c3c;");
            msgLabel.setText("名称不能为空");
            return;
        }

        String chineseCat = categoryBox.getValue();
        String category = CATEGORIES.entrySet().stream()
                .filter(e -> e.getValue().equals(chineseCat))
                .map(Map.Entry::getKey)
                .findFirst().orElse("OTHER");

        double lat, lng;
        try {
            lat = Double.parseDouble(latField.getText().trim());
            lng = Double.parseDouble(lngField.getText().trim());
        } catch (NumberFormatException e) {
            msgLabel.setStyle("-fx-text-fill: #e74c3c;");
            msgLabel.setText("纬度和经度必须是有效数字");
            return;
        }

        if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            msgLabel.setStyle("-fx-text-fill: #e74c3c;");
            msgLabel.setText("经纬度范围无效（纬度: -90~90, 经度: -180~180）");
            return;
        }

        String description = descArea.getText().trim();
        if (description.isEmpty()) {
            description = "暂无描述";
        }

        boolean ok;
        if (isAdmin) {
            // 管理员：更新 buildings + 清除用户覆盖 + 标记管理员已编辑
            building.setName(name);
            building.setCategory(category);
            building.setLatitude(lat);
            building.setLongitude(lng);
            building.setDescription(description);
            ok = mapService.adminSaveBuilding(building);
            if (ok) {
                building.setEditedByAdmin(true);
            }
        } else {
            // 普通用户：保存到个人覆盖表（仅自己可见）
            BuildingOverride override = new BuildingOverride(
                    userId, building.getId(), name, category, lat, lng, description);
            ok = mapService.saveUserOverride(override);
            // 更新当前 building 对象以便 UI 即时刷新
            if (ok) {
                building.setName(name);
                building.setCategory(category);
                building.setLatitude(lat);
                building.setLongitude(lng);
                building.setDescription(description);
            }
        }

        if (ok) {
            saved = true;
            msgLabel.setStyle("-fx-text-fill: #27ae60;");
            msgLabel.setText("保存成功");
            mapView.exitDragMode();
            close();
            if (onSaved != null) {
                Platform.runLater(onSaved);
            }
        } else {
            msgLabel.setStyle("-fx-text-fill: #e74c3c;");
            msgLabel.setText("保存失败，请重试");
        }
    }

    public void setOnSaved(Runnable onSaved) {
        this.onSaved = onSaved;
    }

    public boolean isSaved() {
        return saved;
    }
}
