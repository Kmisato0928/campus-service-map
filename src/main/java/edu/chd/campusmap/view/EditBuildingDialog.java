package edu.chd.campusmap.view;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

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

    private final edu.chd.campusmap.model.Building building;
    private final int userId;
    private final boolean isAdmin;
    private final edu.chd.campusmap.service.MapService mapService;
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

    public EditBuildingDialog(edu.chd.campusmap.model.Building building, int userId, boolean isAdmin, MainMapView mapView) {
        this.building = building;
        this.userId = userId;
        this.isAdmin = isAdmin;
        this.mapView = mapView;
        this.mapService = new edu.chd.campusmap.service.MapService();

        initModality(Modality.NONE);
        Window owner = mapView.getRoot().getScene() != null ? mapView.getRoot().getScene().getWindow() : null;
        if (owner != null) {
            initOwner(owner);
        }
        setTitle("编辑建筑信息 - " + building.getName());

        VBox root = new VBox(12);
        root.getStyleClass().addAll("root", "dialog-container");
        root.setPadding(new Insets(18));

        Label titleLabel = new Label("编辑建筑信息");
        titleLabel.getStyleClass().add("title-label");
        Label subtitleLabel = new Label("调整名称、类别、坐标和说明，保存后会立即同步到当前详情页。");
        subtitleLabel.getStyleClass().add("dialog-subtitle");

        roleHint = new Label();
        roleHint.getStyleClass().add(isAdmin ? "role-admin" : "role-user");
        if (isAdmin) {
            roleHint.setText("管理员模式：修改将全局生效");
        } else {
            roleHint.setText("个人模式：修改仅自己可见");
        }

        Label buildingChip = new Label("当前建筑：" + building.getName());
        buildingChip.getStyleClass().add("meta-chip");
        HBox chipRow = new HBox(10, buildingChip, roleHint);

        VBox headerBox = new VBox(8, titleLabel, subtitleLabel, chipRow);
        headerBox.getStyleClass().add("dialog-header");

        // 拖动提示
        Label dragHint = new Label("拖动地图高亮标记可自动回填坐标，也支持手动修改经纬度。");
        dragHint.getStyleClass().add("hint-banner");
        dragHint.setWrapText(true);

        Label nameLabel = new Label("名称");
        nameLabel.getStyleClass().add("field-label");
        nameField = new TextField(building.getName());
        nameField.setPromptText("请输入建筑名称");

        Label catLabel = new Label("类别");
        catLabel.getStyleClass().add("field-label");
        categoryBox = new ComboBox<>();
        categoryBox.getItems().addAll(CATEGORIES.values());
        String currentCat = CATEGORIES.getOrDefault(building.getCategory(), "其他");
        categoryBox.setValue(currentCat);
        categoryBox.setMaxWidth(Double.MAX_VALUE);

        Label latLabel = new Label("纬度");
        latLabel.getStyleClass().add("field-label");
        latField = new TextField(String.valueOf(building.getLatitude()));
        latField.setPromptText("例如 34.123456");
        latField.setEditable(true);

        Label lngLabel = new Label("经度");
        lngLabel.getStyleClass().add("field-label");
        lngField = new TextField(String.valueOf(building.getLongitude()));
        lngField.setPromptText("例如 108.123456");
        lngField.setEditable(true);

        Label formTitle = new Label("基础设置");
        formTitle.getStyleClass().add("section-caption");
        Label formHint = new Label("上半部分可直接完成名称、类别与坐标修改。");
        formHint.getStyleClass().add("muted-text");

        GridPane formGrid = new GridPane();
        formGrid.getStyleClass().add("dialog-form-grid");
        formGrid.add(nameLabel, 0, 0);
        formGrid.add(nameField, 0, 1);
        formGrid.add(catLabel, 1, 0);
        formGrid.add(categoryBox, 1, 1);
        formGrid.add(latLabel, 0, 2);
        formGrid.add(latField, 0, 3);
        formGrid.add(lngLabel, 1, 2);
        formGrid.add(lngField, 1, 3);
        GridPane.setHgrow(nameField, Priority.ALWAYS);
        GridPane.setHgrow(categoryBox, Priority.ALWAYS);
        GridPane.setHgrow(latField, Priority.ALWAYS);
        GridPane.setHgrow(lngField, Priority.ALWAYS);

        VBox formCard = new VBox(8, formTitle, formHint, formGrid);
        formCard.getStyleClass().addAll("section-card", "section-card-emphasis", "dialog-section");

        Label descLabel = new Label("描述");
        descLabel.getStyleClass().add("field-label");
        descArea = new TextArea(building.getDescription());
        descArea.setPromptText("补充建筑用途、开放信息、位置特征等内容");
        descArea.setPrefRowCount(3);
        descArea.setWrapText(true);

        Label descTitle = new Label("文字说明");
        descTitle.getStyleClass().add("section-caption");
        Label descHint = new Label("这段文字会直接出现在详情页。");
        descHint.getStyleClass().add("muted-text");
        VBox descCard = new VBox(8, descTitle, descHint, descLabel, descArea);
        descCard.getStyleClass().addAll("section-card", "dialog-section");

        Button saveBtn = new Button("保存");
        saveBtn.getStyleClass().addAll("button", "btn-primary", "btn-lg");
        saveBtn.setOnAction(e -> save());

        Button cancelBtn = new Button("取消");
        cancelBtn.getStyleClass().addAll("button", "btn-outline");
        cancelBtn.setOnAction(e -> {
            mapView.exitDragMode();
            close();
        });

        Button closeOnlyBtn = new Button("稍后再改");
        closeOnlyBtn.getStyleClass().addAll("button", "btn-ghost");
        closeOnlyBtn.setOnAction(e -> {
            mapView.exitDragMode();
            close();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox btnRow = new HBox(10, closeOnlyBtn, spacer, cancelBtn, saveBtn);
        btnRow.getStyleClass().add("dialog-actions");

        msgLabel = new Label();
        msgLabel.getStyleClass().addAll("panel-message", "muted-text");
        msgLabel.setText("保存前可先拖动地图标记，坐标会自动更新。");
        msgLabel.setWrapText(true);

        root.getChildren().addAll(headerBox, dragHint, formCard, descCard, btnRow, msgLabel);
        Scene scene = new Scene(root);
        String stylesheet = getClass().getResource("/css/style.css").toExternalForm();
        scene.getStylesheets().add(stylesheet);
        if (owner != null && owner.getScene() != null && owner.getScene().getRoot().getStyleClass().contains("dark-mode")) {
            root.getStyleClass().add("dark-mode");
        }
        setScene(scene);
        setMinWidth(430);
        setMinHeight(440);
        setWidth(680);
        setHeight(500);
        sizeToScene();
        setResizable(true);

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
            msgLabel.getStyleClass().setAll("panel-message", "error-text");
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
            msgLabel.getStyleClass().setAll("panel-message", "error-text");
            msgLabel.setText("纬度和经度必须是有效数字");
            return;
        }

        if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            msgLabel.getStyleClass().setAll("panel-message", "error-text");
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
            edu.chd.campusmap.model.BuildingOverride override = new edu.chd.campusmap.model.BuildingOverride(
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
            msgLabel.getStyleClass().setAll("panel-message", "success-text");
            msgLabel.setText("保存成功");
            mapView.exitDragMode();
            close();
            if (onSaved != null) {
                Platform.runLater(onSaved);
            }
        } else {
            msgLabel.getStyleClass().setAll("panel-message", "error-text");
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
