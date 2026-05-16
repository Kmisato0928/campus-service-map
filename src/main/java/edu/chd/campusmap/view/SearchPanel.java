package edu.chd.campusmap.view;

import edu.chd.campusmap.controller.SearchController;
import edu.chd.campusmap.model.Building;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import javafx.util.StringConverter;

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

    private static final Map<String, String> CAT_NAMES_SHORT = Map.of(
        "TEACHING", "教学", "CANTEEN", "饮食",
        "LIBRARY", "图书", "DORM", "生活", "OTHER", "其他"
    );

    public SearchPanel(SearchController searchController) {
        this.searchController = searchController;
        this.setMinHeight(0);
        this.setPadding(new Insets(16));
        this.setSpacing(12);
        this.getStyleClass().add("side-panel");
        this.setPrefWidth(300);

        // --- Header ---
        Label title = new Label("搜索与导航");
        title.getStyleClass().add("side-header-title");
        Label subtitle = new Label("搜索地点或直接规划校内路线。");
        subtitle.getStyleClass().add("side-header-subtitle");
        VBox headerCopy = new VBox(4, title, subtitle);
        headerCopy.getStyleClass().add("side-header-copy");
        VBox headerBox = new VBox(headerCopy);
        headerBox.getStyleClass().add("side-header-card");

        // ---- Tab toggle ----
        searchTab = new ToggleButton("查找建筑");
        navTab = new ToggleButton("规划路径");
        searchTab.setMaxWidth(Double.MAX_VALUE);
        navTab.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(searchTab, Priority.ALWAYS);
        HBox.setHgrow(navTab, Priority.ALWAYS);

        searchTab.setSelected(true);
        searchTab.getStyleClass().add("tab-active");
        navTab.getStyleClass().add("tab-inactive");
        
        // Add listener to switch tab styles
        searchTab.selectedProperty().addListener((obs, old, isSelected) -> {
            if (isSelected) {
                searchTab.getStyleClass().setAll("toggle-button", "tab-active");
                navTab.getStyleClass().setAll("toggle-button", "tab-inactive");
            }
        });
        navTab.selectedProperty().addListener((obs, old, isSelected) -> {
            if (isSelected) {
                navTab.getStyleClass().setAll("toggle-button", "tab-active");
                searchTab.getStyleClass().setAll("toggle-button", "tab-inactive");
            }
        });

        ToggleGroup tabGroup = new ToggleGroup();
        searchTab.setToggleGroup(tabGroup);
        navTab.setToggleGroup(tabGroup);

        HBox tabSwitcher = new HBox(searchTab, navTab);
        tabSwitcher.getStyleClass().add("section-card");
        tabSwitcher.setPadding(new Insets(4));
        tabSwitcher.setSpacing(0);

        // ---- Search Content ----
        keywordField = new TextField();
        keywordField.setPromptText("输入建筑名称");
        
        categoryBox = new ComboBox<>();
        categoryBox.setItems(FXCollections.observableArrayList(CATEGORIES.values()));
        categoryBox.setValue("全部");
        categoryBox.setMaxWidth(Double.MAX_VALUE);

        Button searchBtn = new Button("搜索地点");
        searchBtn.setMaxWidth(Double.MAX_VALUE);
        searchBtn.getStyleClass().addAll("button", "btn-primary", "btn-lg");

        VBox searchForm = new VBox(10, 
            new Label("建筑名称") {{ getStyleClass().add("field-label"); }},
            keywordField, 
            new Label("分类筛选") {{ getStyleClass().add("field-label"); }},
            categoryBox, 
            searchBtn
        );
        searchForm.getStyleClass().add("section-card");

        resultList = new ListView<>();
        resultList.getStyleClass().add("compact-list");
        resultList.setMinHeight(0); // 确保结果列表可以在窗口缩小时自适应，不会挤占其他空间
        resultList.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Building item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    VBox cell = new VBox(2);
                    Label name = new Label(item.getName());
                    name.getStyleClass().add("section-caption");
                    Label cat = new Label(CAT_NAMES_SHORT.getOrDefault(item.getCategory(), item.getCategory()));
                    cat.getStyleClass().add("meta-chip");
                    cell.getChildren().addAll(name, cat);
                    setGraphic(cell);
                }
            }
        });

        searchContent = new VBox(12, searchForm, resultList);
        searchContent.setMinHeight(0);
        VBox.setVgrow(resultList, Priority.ALWAYS);
        VBox.setVgrow(searchContent, Priority.ALWAYS);

        // ---- Nav mode ----
        fromBox = new ComboBox<>();
        fromBox.setPromptText("选择出发点");
        fromBox.setMaxWidth(Double.MAX_VALUE);

        toBox = new ComboBox<>();
        toBox.setPromptText("选择目的地");
        toBox.setMaxWidth(Double.MAX_VALUE);

        Button planBtn = new Button("生成路线");
        planBtn.setMaxWidth(Double.MAX_VALUE);
        planBtn.getStyleClass().addAll("button", "btn-primary", "btn-lg");
        planBtn.setOnAction(e -> {
            Building from = fromBox.getValue();
            Building to = toBox.getValue();
            if (from != null && to != null && from != to && onPlanRoute != null) {
                onPlanRoute.accept(from, to);
            }
        });

        VBox navForm = new VBox(10, 
            new Label("起点") {{ getStyleClass().add("field-label"); }},
            fromBox, 
            new Label("终点") {{ getStyleClass().add("field-label"); }},
            toBox, 
            planBtn
        );
        navForm.getStyleClass().addAll("section-card", "section-card-emphasis");

        navContent = new VBox(12, navForm);
        navContent.setMinHeight(0);
        navContent.setVisible(false);
        navContent.setManaged(false);
        VBox.setVgrow(navContent, Priority.ALWAYS);

        // --- Logic ---
        keywordField.setOnAction(e -> performSearch());
        searchBtn.setOnAction(e -> performSearch());
        categoryBox.setOnAction(e -> performSearch());

        resultList.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null && listener != null) {
                listener.onBuildingSelected(selected);
            }
        });

        tabGroup.selectedToggleProperty().addListener((obs, old, sel) -> {
            boolean isSearch = sel == searchTab;
            searchContent.setVisible(isSearch);
            searchContent.setManaged(isSearch);
            navContent.setVisible(!isSearch);
            navContent.setManaged(!isSearch);
            if (!isSearch) loadBuildingDropdowns();
        });

        // Cell factories for dropdowns
        Callback<ListView<Building>, ListCell<Building>> cellFactory = lv -> new ListCell<>() {
            @Override
            protected void updateItem(Building item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        };
        StringConverter<Building> buildingNameConverter = new StringConverter<>() {
            @Override
            public String toString(Building building) {
                return building == null ? "" : building.getName();
            }

            @Override
            public Building fromString(String string) {
                return null;
            }
        };
        fromBox.setCellFactory(cellFactory);
        fromBox.setButtonCell(cellFactory.call(null));
        fromBox.setConverter(buildingNameConverter);
        toBox.setCellFactory(cellFactory);
        toBox.setButtonCell(cellFactory.call(null));
        toBox.setConverter(buildingNameConverter);

        getChildren().addAll(headerBox, tabSwitcher, searchContent, navContent);
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

    public void switchToSearchMode() {
        searchTab.setSelected(true);
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
