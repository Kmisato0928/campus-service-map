package edu.chd.campusmap.view;

import edu.chd.campusmap.controller.SearchController;
import edu.chd.campusmap.model.Building;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class SearchPanel extends VBox {

    private final SearchController searchController;

    // Search mode
    private final TextField keywordField;
    private final ComboBox<String> categoryBox;
    private final ListView<Building> resultList;
    private final VBox searchContent;

    // Nav mode
    private final ComboBox<Building> fromBox;
    private final ComboBox<Building> toBox;
    private final VBox navContent;

    // Toggle
    private final ToggleButton searchTab;
    private final ToggleButton navTab;

    public interface SearchListener {
        void onBuildingSelected(Building building);
    }

    private SearchListener listener;
    private BiConsumer<Building, Building> onPlanRoute;

    private static final Map<String, String> CATEGORIES = new LinkedHashMap<>();
    static {
        CATEGORIES.put("ALL", "全部");
        CATEGORIES.put("TEACHING", "教学楼");
        CATEGORIES.put("CANTEEN", "餐厅");
        CATEGORIES.put("LIBRARY", "图书馆");
        CATEGORIES.put("DORM", "宿舍");
        CATEGORIES.put("OTHER", "其他");
    }

    public SearchPanel(SearchController searchController) {
        this.searchController = searchController;
        this.setPadding(new Insets(10));
        this.setSpacing(8);
        this.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; -fx-border-width: 0 0 0 1;");
        this.setPrefWidth(280);

        // ---- Tab toggle ----
        searchTab = new ToggleButton("搜索");
        navTab = new ToggleButton("导航");
        searchTab.setSelected(true);
        searchTab.setStyle("-fx-background-color: #2e86c1; -fx-text-fill: white; -fx-cursor: hand;");
        navTab.setStyle("-fx-background-color: #bdc3c7; -fx-text-fill: white; -fx-cursor: hand;");
        ToggleGroup tabGroup = new ToggleGroup();
        searchTab.setToggleGroup(tabGroup);
        navTab.setToggleGroup(tabGroup);

        ToolBar tabBar = new ToolBar(searchTab, navTab);
        tabBar.setStyle("-fx-background-color: transparent; -fx-padding: 0;");

        // ---- Search mode ----
        Label title = new Label("搜索建筑");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        keywordField = new TextField();
        keywordField.setPromptText("输入建筑名称...");

        categoryBox = new ComboBox<>();
        categoryBox.setItems(FXCollections.observableArrayList(CATEGORIES.values()));
        categoryBox.setValue("全部");

        Button searchBtn = new Button("搜索");
        searchBtn.setMaxWidth(Double.MAX_VALUE);
        searchBtn.setStyle("-fx-background-color: #2e86c1; -fx-text-fill: white; -fx-font-size: 14px;");

        resultList = new ListView<>();
        resultList.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Building item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName() + " (" + item.getCategory() + ")");
            }
        });

        keywordField.setOnAction(e -> performSearch());
        searchBtn.setOnAction(e -> performSearch());
        categoryBox.setOnAction(e -> performSearch());

        resultList.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null && listener != null) {
                listener.onBuildingSelected(selected);
            }
        });

        searchContent = new VBox(8, title, keywordField, categoryBox, searchBtn, resultList);

        // ---- Nav mode ----
        Label navTitle = new Label("路线规划");
        navTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        fromBox = new ComboBox<>();
        fromBox.setPromptText("选择起点...");
        fromBox.setMaxWidth(Double.MAX_VALUE);
        fromBox.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Building item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });
        fromBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Building item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });

        toBox = new ComboBox<>();
        toBox.setPromptText("选择终点...");
        toBox.setMaxWidth(Double.MAX_VALUE);
        toBox.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Building item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });
        toBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Building item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });

        Button planBtn = new Button("规划路线");
        planBtn.setMaxWidth(Double.MAX_VALUE);
        planBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 14px;");
        planBtn.setOnAction(e -> {
            Building from = fromBox.getValue();
            Building to = toBox.getValue();
            if (from != null && to != null && from != to && onPlanRoute != null) {
                onPlanRoute.accept(from, to);
            }
        });

        navContent = new VBox(8, navTitle,
                new Label("起点:"), fromBox,
                new Label("终点:"), toBox,
                planBtn);
        navContent.setVisible(false);
        navContent.setManaged(false);

        tabGroup.selectedToggleProperty().addListener((obs, old, sel) -> {
            boolean isSearch = sel == searchTab;
            searchContent.setVisible(isSearch);
            searchContent.setManaged(isSearch);
            navContent.setVisible(!isSearch);
            navContent.setManaged(!isSearch);
            if (!isSearch) loadBuildingDropdowns();
        });

        getChildren().addAll(tabBar, searchContent, navContent);
    }

    private void performSearch() {
        String keyword = keywordField.getText();
        String chineseCat = categoryBox.getValue();
        String category = CATEGORIES.entrySet().stream()
                .filter(e -> e.getValue().equals(chineseCat))
                .map(Map.Entry::getKey)
                .findFirst().orElse("ALL");
        List<Building> results = searchController.search(keyword, category);
        resultList.setItems(FXCollections.observableArrayList(results));
    }

    private void loadBuildingDropdowns() {
        List<Building> all = searchController.searchByName("");
        fromBox.setItems(FXCollections.observableArrayList(all));
        toBox.setItems(FXCollections.observableArrayList(all));
    }

    public void switchToNavMode(Building destination) {
        navTab.setSelected(true);
        loadBuildingDropdowns();
        toBox.setValue(destination);
    }

    public void setSearchListener(SearchListener listener) {
        this.listener = listener;
    }

    public void setOnPlanRoute(BiConsumer<Building, Building> onPlanRoute) {
        this.onPlanRoute = onPlanRoute;
    }
}
