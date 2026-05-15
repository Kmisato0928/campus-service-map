package edu.chd.campusmap.view;

import edu.chd.campusmap.model.Building;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;

public class NavigationPanel extends VBox {

    private final Label titleLabel;
    private final Label fromLabel;
    private final Label toLabel;
    private final Label distanceLabel;
    private final ListView<String> stepList;
    private final Button exitNavBtn;

    private Runnable onExit;
    private Runnable onBack;

    public NavigationPanel() {
        this.setPadding(new Insets(15));
        this.setSpacing(10);
        this.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; -fx-border-width: 0 0 0 1;");
        this.setPrefWidth(300);

        Label title = new Label("路线导航");
        title.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 16));

        Button backBtn = new Button("← 返回");
        backBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #2e86c1; -fx-border-color: #2e86c1; -fx-border-radius: 4; -fx-cursor: hand;");
        backBtn.setOnAction(e -> { if (onBack != null) onBack.run(); });

        HBox titleRow = new HBox(10, title, backBtn);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        titleLabel = new Label("路线规划");
        titleLabel.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 18));
        titleLabel.setStyle("-fx-text-fill: #1a5276;");

        fromLabel = new Label();
        fromLabel.setStyle("-fx-text-fill: #2c3e50;");

        toLabel = new Label();
        toLabel.setStyle("-fx-text-fill: #2c3e50;");

        distanceLabel = new Label();
        distanceLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 14; -fx-font-weight: bold;");

        stepList = new ListView<>();
        stepList.setPrefHeight(300);

        exitNavBtn = new Button("退出导航");
        exitNavBtn.setMaxWidth(Double.MAX_VALUE);
        exitNavBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 8 0;");
        exitNavBtn.setOnAction(e -> { if (onExit != null) onExit.run(); });

        getChildren().addAll(titleRow, titleLabel, fromLabel, toLabel, distanceLabel, stepList, exitNavBtn);
        setVisible(false);
    }

    public void showRoute(Building from, Building to, double totalMeters, List<String> steps) {
        titleLabel.setText("导航: " + from.getName() + " → " + to.getName());
        fromLabel.setText("起点: " + from.getName());
        toLabel.setText("终点: " + to.getName());

        String distText;
        if (totalMeters >= 1000) {
            distText = String.format("总距离: %.2f 公里", totalMeters / 1000);
        } else {
            distText = String.format("总距离: %.0f 米", totalMeters);
        }
        distanceLabel.setText(distText);

        stepList.getItems().setAll(steps);
        setVisible(true);
    }

    public void setOnExit(Runnable onExit) {
        this.onExit = onExit;
    }

    public void setOnBack(Runnable onBack) {
        this.onBack = onBack;
    }
}
