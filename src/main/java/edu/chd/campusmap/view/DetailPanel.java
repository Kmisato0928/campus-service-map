package edu.chd.campusmap.view;

import edu.chd.campusmap.model.Building;
import edu.chd.campusmap.model.Comment;
import edu.chd.campusmap.model.User;
import edu.chd.campusmap.service.CommentService;
import edu.chd.campusmap.dao.FavoriteDAO;
import javafx.collections.FXCollections;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import java.util.List;

public class DetailPanel extends VBox {
    private final CommentService commentService;
    private final FavoriteDAO favoriteDAO;
    private Building currentBuilding;
    private User currentUser;
    private final Label nameLabel;
    private final Label categoryLabel;
    private final Label descLabel;
    private final ListView<Comment> commentList;
    private final TextArea commentInput;
    private final ComboBox<Integer> ratingBox;
    private final Button favoriteBtn;
    private final Button editBtn;
    private final Button submitBtn;
    private final Label msgLabel;
    private final Button navigateBtn;
    private Runnable onDataChanged;
    private Runnable onBack;
    private Runnable onNavigate;
    private MainMapView mainMapView;
    private EditBuildingDialog currentEditDialog;

    private static final java.util.Map<String, String> CAT_NAMES = java.util.Map.of(
        "TEACHING", "教学楼", "CANTEEN", "餐厅",
        "LIBRARY", "图书馆", "DORM", "宿舍", "OTHER", "其他"
    );

    public DetailPanel() {
        this.commentService = new CommentService();
        this.favoriteDAO = new FavoriteDAO();
        this.setPadding(new Insets(15));
        this.setSpacing(10);
        this.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; -fx-border-width: 0 0 0 1;");
        this.setPrefWidth(300);

        Label title = new Label("建筑详情");
        title.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 16));

        Button backBtn = new Button("← 返回搜索");
        backBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #2e86c1; -fx-border-color: #2e86c1; -fx-border-radius: 4; -fx-cursor: hand;");
        backBtn.setOnAction(e -> { if (onBack != null) onBack.run(); });
        HBox titleRow = new HBox(10, title, backBtn);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        nameLabel = new Label();
        nameLabel.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 18));
        nameLabel.setStyle("-fx-text-fill: #1a5276;");

        categoryLabel = new Label();
        categoryLabel.setStyle("-fx-text-fill: #566573;");

        descLabel = new Label();
        descLabel.setWrapText(true);
        descLabel.setStyle("-fx-text-fill: #2c3e50; -fx-line-spacing: 4;");

        favoriteBtn = new Button("收藏");
        favoriteBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white;");
        favoriteBtn.setOnAction(e -> toggleFavorite());

        editBtn = new Button("编辑");
        editBtn.setStyle("-fx-background-color: #2e86c1; -fx-text-fill: white;");
        editBtn.setOnAction(e -> editBuilding());

        Button locateBtn = new Button("定位");
        locateBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        locateBtn.setOnAction(e -> {
            if (currentBuilding != null && mainMapView != null) {
                mainMapView.animateCenter(currentBuilding.getLatitude(), currentBuilding.getLongitude());
            }
        });

        navigateBtn = new Button("导航");
        navigateBtn.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white;");
        navigateBtn.setOnAction(e -> { if (onNavigate != null) onNavigate.run(); });

        HBox infoRow = new HBox(10, categoryLabel, locateBtn, navigateBtn, favoriteBtn, editBtn);
        infoRow.setAlignment(Pos.CENTER_LEFT);

        Separator sep1 = new Separator();
        Label commentTitle = new Label("用户评论");
        commentTitle.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 14));

        commentList = new ListView<>();
        commentList.setPrefHeight(200);
        commentList.setCellFactory(param -> new ListCell<>() {
            private final Label usernameLbl = new Label();
            private final Label starsLbl = new Label();
            private final Label timeLbl = new Label();
            private final Label contentLbl = new Label();
            private final Button likeBtn = new Button();
            private final Label likeCountLbl = new Label();
            private final Button deleteBtn = new Button("删除");
            private final VBox cellBox = new VBox(3);
            private Comment currentComment;

            {
                usernameLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 12; -fx-text-fill: #2c3e50;");
                timeLbl.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 10;");
                starsLbl.setStyle("-fx-font-size: 12;");
                contentLbl.setWrapText(true);
                contentLbl.setStyle("-fx-font-size: 12; -fx-text-fill: #34495e;");
                likeBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-font-size: 12; -fx-padding: 2 6;");
                likeBtn.setStyle("-fx-background-color: #ecf0f1; -fx-border-color: #bdc3c7; -fx-border-radius: 4; -fx-cursor: hand; -fx-font-size: 11; -fx-padding: 2 8;");
                likeCountLbl.setStyle("-fx-font-size: 11; -fx-text-fill: #566573;");
                deleteBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #e74c3c; -fx-cursor: hand; -fx-font-size: 11; -fx-padding: 2 6;");

                HBox topRow = new HBox(6);
                Region spacer1 = new Region();
                HBox.setHgrow(spacer1, Priority.ALWAYS);
                topRow.getChildren().addAll(usernameLbl, starsLbl, spacer1, timeLbl);

                HBox actionRow = new HBox(6);
                Region spacer2 = new Region();
                HBox.setHgrow(spacer2, Priority.ALWAYS);
                actionRow.getChildren().addAll(likeBtn, likeCountLbl, spacer2, deleteBtn);

                cellBox.getChildren().addAll(topRow, contentLbl, actionRow);
                cellBox.setStyle("-fx-padding: 8; -fx-border-color: #eee; -fx-border-width: 0 0 1 0;");
            }

            @Override
            protected void updateItem(Comment item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    currentComment = null;
                } else {
                    currentComment = item;
                    usernameLbl.setText(item.getUsername());
                    starsLbl.setText("★".repeat(item.getRating()) + "☆".repeat(5 - item.getRating()));
                    timeLbl.setText(item.getFormattedTime());
                    contentLbl.setText(item.getContent());
                    likeCountLbl.setText("👍 " + item.getLikeCount());
                    likeBtn.setText(item.isLikedByMe() ? "已赞" : "点赞");
                    likeBtn.setOnAction(e -> handleLike());
                    boolean canDelete = currentUser != null
                            && (item.getUserId() == currentUser.getId() || "ADMIN".equals(currentUser.getRole()));
                    deleteBtn.setVisible(canDelete);
                    deleteBtn.setManaged(canDelete);
                    deleteBtn.setOnAction(e -> handleDelete());
                    setGraphic(cellBox);
                }
            }

            private void handleLike() {
                if (currentUser == null || currentComment == null) return;
                if (currentComment.isLikedByMe()) {
                    commentService.unlikeComment(currentUser.getId(), currentComment.getId());
                } else {
                    commentService.likeComment(currentUser.getId(), currentComment.getId());
                }
                loadComments();
            }

            private void handleDelete() {
                if (currentUser == null || currentComment == null) return;
                boolean isAdmin = "ADMIN".equals(currentUser.getRole());
                if (commentService.deleteComment(currentComment.getId(), currentUser.getId(), isAdmin)) {
                    loadComments();
                    if (onDataChanged != null) onDataChanged.run();
                }
            }
        });

        commentInput = new TextArea();
        commentInput.setPromptText("写评论...");
        commentInput.setPrefRowCount(3);

        HBox commentBottom = new HBox(10);
        ratingBox = new ComboBox<>();
        ratingBox.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5));
        ratingBox.setValue(5);
        ratingBox.setPrefWidth(70);

        submitBtn = new Button("发表");
        submitBtn.setStyle("-fx-background-color: #2e86c1; -fx-text-fill: white;");
        submitBtn.setOnAction(e -> submitComment());

        msgLabel = new Label();
        msgLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 12;");

        commentBottom.getChildren().addAll(new Label("评分:"), ratingBox, submitBtn);

        getChildren().addAll(titleRow, nameLabel, infoRow, descLabel, sep1,
                commentTitle, commentList, commentInput, commentBottom, msgLabel);

        setVisible(false);
    }

    public void showBuilding(Building building, User user) {
        this.currentBuilding = building;
        this.currentUser = user;
        nameLabel.setText(building.getName());
        categoryLabel.setText("类别: " + CAT_NAMES.getOrDefault(building.getCategory(), building.getCategory()));
        descLabel.setText(building.getDescription() != null ? building.getDescription() : "暂无描述");
        // 编辑按钮：管理员始终可见；普通用户只有建筑未被管理员编辑时才可见
        if (user == null) {
            editBtn.setVisible(false);
        } else if ("ADMIN".equals(user.getRole())) {
            editBtn.setVisible(true);
        } else {
            editBtn.setVisible(!building.isEditedByAdmin());
        }
        updateFavoriteButton();
        loadComments();
        setVisible(true);
        msgLabel.setText("");
        commentInput.clear();
    }

    public void clearUser() {
        this.currentUser = null;
    }

    public void refresh() {
        if (currentBuilding != null) {
            updateFavoriteButton();
            loadComments();
        }
    }

    private void loadComments() {
        if (currentBuilding == null) return;
        Integer uid = currentUser != null ? currentUser.getId() : null;
        List<Comment> comments = commentService.getCommentsByBuilding(currentBuilding.getId(), uid);
        commentList.setItems(FXCollections.observableArrayList(comments));
    }

    private void toggleFavorite() {
        if (currentUser == null || currentBuilding == null) {
            msgLabel.setStyle("-fx-text-fill: #e74c3c;");
            msgLabel.setText("请先登录");
            return;
        }
        int uid = currentUser.getId();
        int bid = currentBuilding.getId();
        boolean isFav = favoriteDAO.isFavorited(uid, bid);
        if (isFav) {
            favoriteDAO.deleteByUserAndBuilding(uid, bid);
            msgLabel.setStyle("-fx-text-fill: #566573;");
            msgLabel.setText("已取消收藏");
        } else {
            favoriteDAO.save(new edu.chd.campusmap.model.Favorite(uid, bid));
            msgLabel.setStyle("-fx-text-fill: #27ae60;");
            msgLabel.setText("已收藏");
        }
        updateFavoriteButton();
        if (onDataChanged != null) onDataChanged.run();
    }

    private void editBuilding() {
        if (currentBuilding == null || currentUser == null || mainMapView == null) return;
        // 关闭已有编辑对话框
        if (currentEditDialog != null) {
            currentEditDialog.close();
            currentEditDialog = null;
        }
        boolean isAdmin = "ADMIN".equals(currentUser.getRole());
        currentEditDialog = new EditBuildingDialog(currentBuilding, currentUser.getId(), isAdmin, mainMapView);
        currentEditDialog.setOnSaved(() -> {
            currentEditDialog = null;
            Platform.runLater(() -> {
                showBuilding(currentBuilding, currentUser);
                if (onDataChanged != null) onDataChanged.run();
            });
        });
        currentEditDialog.show();
    }

    private void updateFavoriteButton() {
        if (currentUser == null || currentBuilding == null) {
            favoriteBtn.setText("收藏");
            return;
        }
        boolean isFav = favoriteDAO.isFavorited(currentUser.getId(), currentBuilding.getId());
        favoriteBtn.setText(isFav ? "取消收藏" : "收藏");
        favoriteBtn.setStyle(isFav
                ? "-fx-background-color: #95a5a6; -fx-text-fill: white;"
                : "-fx-background-color: #f39c12; -fx-text-fill: white;");
    }

    private void submitComment() {
        if (currentUser == null) {
            msgLabel.setStyle("-fx-text-fill: #e74c3c;");
            msgLabel.setText("请先登录");
            return;
        }
        String content = commentInput.getText().trim();
        if (content.isEmpty()) {
            msgLabel.setStyle("-fx-text-fill: #e74c3c;");
            msgLabel.setText("评论内容不能为空");
            return;
        }
        int rating = ratingBox.getValue() != null ? ratingBox.getValue() : 5;
        if (commentService.addComment(currentUser.getId(), currentBuilding.getId(), content, rating)) {
            msgLabel.setStyle("-fx-text-fill: #27ae60;");
            msgLabel.setText("评论发表成功");
            commentInput.clear();
            loadComments();
            if (onDataChanged != null) onDataChanged.run();
        } else {
            msgLabel.setStyle("-fx-text-fill: #e74c3c;");
            msgLabel.setText("评论发表失败");
        }
    }

    public Building getCurrentBuilding() {
        return currentBuilding;
    }

    public void setMapView(MainMapView mainMapView) {
        this.mainMapView = mainMapView;
    }

    public void setOnDataChanged(Runnable onDataChanged) {
        this.onDataChanged = onDataChanged;
    }

    public void setOnBack(Runnable onBack) {
        this.onBack = onBack;
    }

    public void setOnNavigate(Runnable onNavigate) {
        this.onNavigate = onNavigate;
    }
}
