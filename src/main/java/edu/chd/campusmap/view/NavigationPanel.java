package edu.chd.campusmap.view;

import edu.chd.campusmap.model.Building;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.layout.Priority;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

public class NavigationPanel extends VBox {

    private final Label fromLabel;
    private final Label toLabel;
    private final Label distanceLabel;
    private final ListView<String> stepList;
    private final Button exitNavBtn;

    private Runnable onExit;
    private Runnable onBack;

    public NavigationPanel() {
        this.setMinHeight(0); // 释放面板最小高度限制，防止被 StackPane 居中裁切
        this.setPadding(new Insets(16, 16, 24, 16)); // 恢复合理底边距
        this.setSpacing(12);
        this.getStyleClass().add("side-panel");
        this.setPrefWidth(300);

        // --- Header ---
        Button backBtn = new Button("←");
        backBtn.getStyleClass().addAll("button", "btn-ghost", "btn-round", "btn-icon-only");
        backBtn.setOnAction(e -> { if (onBack != null) onBack.run(); });

        Label badge = new Label("路线页");
        badge.getStyleClass().add("side-header-badge");
        Label title = new Label("路线导航");
        title.getStyleClass().add("side-header-title");
        Label subtitle = new Label("查看起终点、总距离与行进步骤。");
        subtitle.getStyleClass().add("side-header-subtitle");

        VBox headerCopy = new VBox(4, badge, title, subtitle);
        headerCopy.getStyleClass().add("side-header-copy");
        HBox titleRow = new HBox(12, backBtn, headerCopy);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        titleRow.getStyleClass().add("side-header-card");

        // --- Summary Card ---
        Label summaryTitle = new Label("行程摘要");
        summaryTitle.getStyleClass().add("section-caption");

        fromLabel = new Label();
        fromLabel.getStyleClass().add("info-label");
        
        toLabel = new Label();
        toLabel.getStyleClass().add("info-label");

        distanceLabel = new Label();
        distanceLabel.getStyleClass().add("success-chip");

        VBox summaryBox = new VBox(8, 
            summaryTitle,
            new HBox(10, new Label("🛫") {{ getStyleClass().add("muted-text"); }}, fromLabel) {{ setAlignment(Pos.CENTER_LEFT); }},
            new HBox(10, new Label("🏁") {{ getStyleClass().add("muted-text"); }}, toLabel) {{ setAlignment(Pos.CENTER_LEFT); }},
            new Separator(),
            new HBox(distanceLabel) {{ setAlignment(Pos.CENTER_RIGHT); }}
        );
        summaryBox.getStyleClass().addAll("section-card", "section-card-emphasis");

        // --- Steps Card ---
        Label stepsTitle = new Label("行进指引");
        stepsTitle.getStyleClass().add("section-caption");

        stepList = new ListView<>();
        stepList.getStyleClass().add("compact-list");
        stepList.setPrefHeight(360);
        stepList.setMinHeight(0); // 彻底释放最小高度限制，随窗口自适应
        stepList.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox cell = new HBox(10);
                    cell.setAlignment(Pos.CENTER_LEFT);
                    Label icon = new Label(item.contains("出发") ? "📍" : item.contains("到达") ? "🎯" : "🚶");
                    Label text = new Label(item);
                    text.setWrapText(true);
                    text.setMaxWidth(200);
                    cell.getChildren().addAll(icon, text);
                    setGraphic(cell);
                }
            }
        });
        VBox.setVgrow(stepList, Priority.ALWAYS);

        VBox stepsCard = new VBox(10, stepsTitle, stepList);
        stepsCard.getStyleClass().add("section-card");
        stepsCard.setMinHeight(0);

        // --- Actions ---
        exitNavBtn = new Button("结束导航并清除路线");
        exitNavBtn.setMaxWidth(Double.MAX_VALUE);
        exitNavBtn.setMinHeight(42);
        exitNavBtn.getStyleClass().addAll("button", "btn-danger");
        exitNavBtn.setOnAction(e -> { if (onExit != null) onExit.run(); });

        VBox.setMargin(exitNavBtn, new Insets(12, 0, 0, 0));

        getChildren().addAll(titleRow, summaryBox, stepsCard, exitNavBtn);
        VBox.setVgrow(stepsCard, Priority.ALWAYS); // 重新启用 VGrow 让 ListView 动态收缩，避免撑爆容器
        setVisible(false);
    }

    public void showRoute(Building from, Building to, double totalMeters, List<String> steps) {
        fromLabel.setText(from.getName());
        toLabel.setText(to.getName());

        String distText;
        if (totalMeters >= 1000) {
            distText = String.format("%.2f km", totalMeters / 1000);
        } else {
            distText = String.format("%.0f m", totalMeters);
        }
        distanceLabel.setText("总计 " + distText);

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
