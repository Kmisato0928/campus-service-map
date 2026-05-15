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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;

public class UserCenterView extends BorderPane {
    private final User currentUser;
    private final FavoriteDAO favoriteDAO;
    private final CommentDAO commentDAO;
    private final MapService mapService;
    private final ListView<String> favoriteList;
    private final ListView<Comment> commentList;
    private final Label infoLabel;
    private Runnable onLogout;
    private Runnable onClose;

    public UserCenterView(User user) {
        this.currentUser = user;
        this.favoriteDAO = new FavoriteDAO();
        this.commentDAO = new CommentDAO();
        this.mapService = new MapService();
        this.setPadding(new Insets(20));
        this.setStyle("-fx-background-color: #f0f3f5;");

        Label title = new Label("个人中心");
        title.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 20));
        title.setStyle("-fx-text-fill: #1a5276;");

        infoLabel = new Label("欢迎, " + user.getUsername()
                + "  |  角色: " + ("ADMIN".equals(user.getRole()) ? "管理员" : "普通用户"));
        infoLabel.setStyle("-fx-text-fill: #566573;");

        Button logoutBtn = new Button("退出登录");
        logoutBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        logoutBtn.setOnAction(e -> {
            if (onLogout != null) onLogout.run();
        });

        Button closeBtn = new Button("返回地图");
        closeBtn.setStyle("-fx-background-color: #2e86c1; -fx-text-fill: white;");
        closeBtn.setOnAction(e -> {
            if (onClose != null) onClose.run();
        });

        HBox topBar = new HBox(15, infoLabel, closeBtn, logoutBtn);
        topBar.setAlignment(Pos.CENTER_RIGHT);

        VBox topArea = new VBox(10, title, topBar);

        // left: favorites
        VBox leftPanel = new VBox(8);
        leftPanel.setPadding(new Insets(10));
        leftPanel.setStyle("-fx-background-color: white; -fx-background-radius: 6;");
        Label favTitle = new Label("我的收藏");
        favTitle.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 14));

        favoriteList = new ListView<>();
        favoriteList.setPrefHeight(300);
        favoriteList.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
            }
        });

        favoriteList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String selected = favoriteList.getSelectionModel().getSelectedItem();
                if (selected != null && onClose != null) onClose.run();
            }
        });

        leftPanel.getChildren().addAll(favTitle, favoriteList);

        // right: comments
        VBox rightPanel = new VBox(8);
        rightPanel.setPadding(new Insets(10));
        rightPanel.setStyle("-fx-background-color: white; -fx-background-radius: 6;");
        Label comTitle = new Label("我的评论");
        comTitle.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 14));

        commentList = new ListView<>();
        commentList.setPrefHeight(300);
        commentList.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Comment item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String stars = "★".repeat(item.getRating()) + "☆".repeat(5 - item.getRating());
                    var building = mapService.getBuildingById(item.getBuildingId());
                    String buildingName = building != null ? building.getName() : "建筑#" + item.getBuildingId();
                    setText(buildingName + "  " + stars + "\n" + item.getContent());
                    setStyle("-fx-padding: 6; -fx-border-color: #eee; -fx-border-width: 0 0 1 0;");
                }
            }
        });

        rightPanel.getChildren().addAll(comTitle, commentList);

        HBox centerContent = new HBox(20, leftPanel, rightPanel);
        centerContent.setPadding(new Insets(20, 0, 0, 0));

        setTop(topArea);
        setCenter(centerContent);

        loadData();
    }

    private void loadData() {
        // load favorites
        List<Favorite> favs = favoriteDAO.findByUserId(currentUser.getId());
        var favItems = FXCollections.<String>observableArrayList();
        for (Favorite f : favs) {
            String bName = f.getBuildingName() != null ? f.getBuildingName() : "建筑#" + f.getBuildingId();
            favItems.add(bName + "  (" + f.getBuildingCategory() + ")");
        }
        favoriteList.setItems(favItems);

        // load comments
        List<Comment> comments = commentDAO.findByUserId(currentUser.getId());
        commentList.setItems(FXCollections.observableArrayList(comments));
    }

    public void setOnLogout(Runnable onLogout) {
        this.onLogout = onLogout;
    }

    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }
}
