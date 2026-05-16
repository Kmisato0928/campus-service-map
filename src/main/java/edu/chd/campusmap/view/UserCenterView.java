package edu.chd.campusmap.view;

import edu.chd.campusmap.dao.CommentDAO;
import edu.chd.campusmap.dao.FavoriteDAO;
import edu.chd.campusmap.model.Comment;
import edu.chd.campusmap.model.Favorite;
import edu.chd.campusmap.model.User;
import edu.chd.campusmap.service.MapService;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

public class UserCenterView extends BorderPane {
    private final User currentUser;
    private final FavoriteDAO favoriteDAO;
    private final CommentDAO commentDAO;
    private final MapService mapService;
    private final ListView<Favorite> favoriteList;
    private final ListView<Comment> commentList;
    private final Label favoriteCountLabel;
    private final Label commentCountLabel;
    private final Label roleCountLabel;
    private final Label activityCountLabel;
    private Runnable onLogout;
    private Runnable onClose;

    public UserCenterView(User user) {
        this.currentUser = user;
        this.favoriteDAO = new FavoriteDAO();
        this.commentDAO = new CommentDAO();
        this.mapService = new MapService();
        this.setPadding(new Insets(0));
        this.getStyleClass().add("dashboard-shell");

        Label title = new Label("个人中心");
        title.getStyleClass().add("title-label");

        Label accountName = new Label(user.getUsername());
        accountName.getStyleClass().add("dashboard-hero-title");
        Label accountRole = new Label("ADMIN".equals(user.getRole()) ? "管理员" : "普通用户");
        accountRole.getStyleClass().add("success-chip");
        Label accountEmail = new Label(user.getEmail() == null || user.getEmail().isBlank() ? "未填写邮箱" : user.getEmail());
        accountEmail.getStyleClass().add("meta-chip");

        HBox identityRow = new HBox(12, accountName, accountRole, accountEmail);
        identityRow.setAlignment(Pos.CENTER_LEFT);

        Button logoutBtn = new Button("退出登录");
        logoutBtn.getStyleClass().addAll("button", "btn-danger");
        logoutBtn.setOnAction(e -> {
            if (onLogout != null) onLogout.run();
        });

        Button closeBtn = new Button("返回地图");
        closeBtn.getStyleClass().addAll("button", "btn-primary");
        closeBtn.setOnAction(e -> {
            if (onClose != null) onClose.run();
        });

        Button themeBtn = new Button("切换深色模式");
        themeBtn.getStyleClass().addAll("button", "btn-secondary");
        themeBtn.setOnAction(e -> {
            if (getScene() != null && getScene().getRoot() != null) {
                var root = getScene().getRoot();
                if (root.getStyleClass().contains("dark-mode")) {
                    root.getStyleClass().remove("dark-mode");
                } else {
                    root.getStyleClass().add("dark-mode");
                }
            }
        });

        FlowPane metaRow = new FlowPane(10, 10, identityRow);
        metaRow.getStyleClass().add("dashboard-meta-row");

        favoriteCountLabel = createStatValueLabel("0");
        commentCountLabel = createStatValueLabel("0");
        roleCountLabel = createStatValueLabel("普通用户");
        activityCountLabel = createStatValueLabel("0");

        VBox favoriteStat = createStatCard("收藏地点", favoriteCountLabel);
        VBox commentStat = createStatCard("评论记录", commentCountLabel);
        VBox roleStat = createStatCard("账号身份", roleCountLabel);
        VBox activityStat = createStatCard("总活跃度", activityCountLabel);

        HBox statRow = new HBox(14, favoriteStat, commentStat, roleStat, activityStat);
        statRow.getStyleClass().add("dashboard-stat-row");
        HBox.setHgrow(favoriteStat, Priority.ALWAYS);
        HBox.setHgrow(commentStat, Priority.ALWAYS);
        HBox.setHgrow(roleStat, Priority.ALWAYS);
        HBox.setHgrow(activityStat, Priority.ALWAYS);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox actionRow = new HBox(12, themeBtn, closeBtn, logoutBtn);
        actionRow.getStyleClass().add("dashboard-header-actions");
        actionRow.setAlignment(Pos.CENTER_RIGHT);

        HBox titleAndActions = new HBox(title, spacer, actionRow);
        titleAndActions.setAlignment(Pos.CENTER_LEFT);

        VBox heroText = new VBox(12, titleAndActions, identityRow);
        heroText.getStyleClass().add("dashboard-hero-copy");

        VBox topArea = new VBox(20, heroText, statRow);
        topArea.getStyleClass().add("dashboard-header");

        // left: favorites
        VBox leftPanel = new VBox(8);
        leftPanel.getStyleClass().addAll("card-panel", "dashboard-panel");
        Label favTitle = new Label("我的收藏");
        favTitle.getStyleClass().add("dashboard-panel-title");
        Label favSubtitle = new Label("常用建筑会保存在这里，双击后可快速回到地图。");
        favSubtitle.getStyleClass().add("dashboard-panel-subtitle");

        favoriteList = new ListView<>();
        favoriteList.getStyleClass().add("compact-list");
        favoriteList.setPrefHeight(300);
        favoriteList.setPlaceholder(new Label("暂无收藏记录，快去地图探索并点亮常用地点吧") {{ getStyleClass().add("muted-text"); }});
        favoriteList.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Favorite item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String bName = item.getBuildingName() != null ? item.getBuildingName() : "未知建筑";
                    Label nameLbl = new Label(bName);
                    nameLbl.getStyleClass().add("section-caption");
                    Label catLbl = new Label(item.getBuildingCategory());
                    catLbl.getStyleClass().add("meta-chip");
                    
                    HBox box = new HBox(12, nameLbl, catLbl);
                    box.setAlignment(Pos.CENTER_LEFT);
                    box.setPadding(new Insets(4, 0, 4, 0));
                    setGraphic(box);
                    setText(null);
                }
            }
        });

        favoriteList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                Favorite selected = favoriteList.getSelectionModel().getSelectedItem();
                if (selected != null && onClose != null) onClose.run();
            }
        });

        Label favHint = new Label("双击收藏可返回地图并在图上定位");
        favHint.getStyleClass().add("muted-text");

        leftPanel.getChildren().addAll(favTitle, favoriteList, favHint);
        HBox.setHgrow(leftPanel, Priority.ALWAYS);
        VBox.setVgrow(favoriteList, Priority.ALWAYS);

        // right: comments
        VBox rightPanel = new VBox(8);
        rightPanel.getStyleClass().addAll("card-panel", "dashboard-panel");
        Label comTitle = new Label("我的评论");
        comTitle.getStyleClass().add("dashboard-panel-title");

        commentList = new ListView<>();
        commentList.getStyleClass().add("compact-list");
        commentList.setPrefHeight(300);
        commentList.setPlaceholder(new Label("暂无评论记录，快去地图发表你的看法吧") {{ getStyleClass().add("muted-text"); }});
        commentList.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Comment item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String stars = "★".repeat(item.getRating()) + "☆".repeat(5 - item.getRating());
                    var building = mapService.getBuildingById(item.getBuildingId());
                    String buildingName = building != null ? building.getName() : "建筑#" + item.getBuildingId();
                    
                    VBox cellBox = new VBox(4);
                    Label titleLbl = new Label(buildingName + "  " + stars);
                    titleLbl.getStyleClass().add("section-label");
                    Label contentLbl = new Label(item.getContent());
                    contentLbl.getStyleClass().add("info-label");
                    
                    cellBox.getChildren().addAll(titleLbl, contentLbl);
                    cellBox.getStyleClass().add("comment-cell");
                    setGraphic(cellBox);
                    setText(null);
                }
            }
        });

        rightPanel.getChildren().addAll(comTitle, commentList);
        HBox.setHgrow(rightPanel, Priority.ALWAYS);
        VBox.setVgrow(commentList, Priority.ALWAYS);

        HBox centerContent = new HBox(20, leftPanel, rightPanel);
        centerContent.getStyleClass().add("dashboard-grid");
        BorderPane.setMargin(centerContent, new Insets(20, 0, 0, 0));

        setTop(topArea);
        setCenter(centerContent);

        loadData();
    }

    private Label createStatValueLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("dashboard-stat-value");
        return label;
    }

    private VBox createStatCard(String title, Label valueLabel) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("dashboard-stat-title");

        VBox card = new VBox(8, titleLabel, valueLabel);
        card.getStyleClass().add("dashboard-stat-card");
        return card;
    }

    // Removed buildAccountSummary as it is no longer used

    private void loadData() {
        // load favorites
        List<Favorite> favs = favoriteDAO.findByUserId(currentUser.getId());
        favoriteList.setItems(FXCollections.observableArrayList(favs));

        // load comments
        List<Comment> comments = commentDAO.findByUserId(currentUser.getId());
        commentList.setItems(FXCollections.observableArrayList(comments));
        favoriteCountLabel.setText(String.valueOf(favs.size()));
        commentCountLabel.setText(String.valueOf(comments.size()));
        roleCountLabel.setText("ADMIN".equals(currentUser.getRole()) ? "管理员" : "普通用户");
        activityCountLabel.setText(String.valueOf(favs.size() + comments.size()));
    }

    public void setOnLogout(Runnable onLogout) {
        this.onLogout = onLogout;
    }

    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }
}
