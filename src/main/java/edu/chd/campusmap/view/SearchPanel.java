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

public class SearchPanel extends VBox {
    private final SearchController searchController;
    private final TextField keywordField;
    private final ComboBox<String> categoryBox;
    private final ListView<Building> resultList;

    public interface SearchListener {
        void onBuildingSelected(Building building);
    }

    private SearchListener listener;

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
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName() + " (" + item.getCategory() + ")");
                }
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

        getChildren().addAll(title, keywordField, categoryBox, searchBtn, resultList);
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

    public void setSearchListener(SearchListener listener) {
        this.listener = listener;
    }
}
