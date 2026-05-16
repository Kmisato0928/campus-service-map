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
        this.setMinHeight(0);
        this.setPadding(new Insets(16));
        this.setSpacing(12);
        this.getStyleClass().add("side-panel");
        this.setPrefWidth(300);

        // --- Header ---
        Button backBtn = new Button("←");
        backBtn.getStyleClass().addAll("button", "btn-ghost", "btn-round", "btn-icon-only");
        backBtn.setTooltip(new Tooltip("返回搜索"));
        backBtn.setOnAction(e -> { if (onBack != null) onBack.run(); });

        Label badge = new Label("详情页");
        badge.getStyleClass().add("side-header-badge");
        Label title = new Label("建筑详情");
        title.getStyleClass().add("side-header-title");
        Label subtitle = new Label("查看简介、定位并快速发起导航。");
        subtitle.getStyleClass().add("side-header-subtitle");

        VBox headerCopy = new VBox(4, badge, title, subtitle);
        headerCopy.getStyleClass().add("side-header-copy");
        HBox headerRow = new HBox(12, backBtn, headerCopy);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.getStyleClass().add("side-header-card");

        // --- Summary Card ---
        nameLabel = new Label();
        nameLabel.getStyleClass().add("heading-label");
        nameLabel.setWrapText(true);

        categoryLabel = new Label();
        categoryLabel.getStyleClass().add("meta-chip");

        descLabel = new Label();
        descLabel.getStyleClass().add("info-label");
        descLabel.setWrapText(true);
        descLabel.setMinHeight(40);

        // Primary Actions
        Button locateBtn = new Button("定位");
        locateBtn.getStyleClass().addAll("button", "btn-outline", "btn-lg");
        HBox.setHgrow(locateBtn, Priority.ALWAYS);
        locateBtn.setMaxWidth(Double.MAX_VALUE);
        locateBtn.setOnAction(e -> {
            if (currentBuilding != null && mainMapView != null) {
                mainMapView.animateCenter(currentBuilding.getLatitude(), currentBuilding.getLongitude());
            }
        });

        navigateBtn = new Button("导航");
        navigateBtn.getStyleClass().addAll("button", "btn-primary", "btn-lg");
        HBox.setHgrow(navigateBtn, Priority.ALWAYS);
        navigateBtn.setMaxWidth(Double.MAX_VALUE);
        navigateBtn.setOnAction(e -> { if (onNavigate != null) onNavigate.run(); });

        HBox mainActions = new HBox(10, locateBtn, navigateBtn);
        mainActions.setAlignment(Pos.CENTER);

        // Secondary Actions
        favoriteBtn = new Button("收藏");
        favoriteBtn.getStyleClass().addAll("button", "btn-outline");
        HBox.setHgrow(favoriteBtn, Priority.ALWAYS);
        favoriteBtn.setMaxWidth(Double.MAX_VALUE);
        favoriteBtn.setOnAction(e -> toggleFavorite());

        editBtn = new Button("编辑资料");
        editBtn.getStyleClass().addAll("button", "btn-secondary", "btn-sm");
        HBox.setHgrow(editBtn, Priority.ALWAYS);
        editBtn.setMaxWidth(Double.MAX_VALUE);
        editBtn.setOnAction(e -> editBuilding());

        HBox secondaryActions = new HBox(10, favoriteBtn, editBtn);
        secondaryActions.setAlignment(Pos.CENTER);
        secondaryActions.setPadding(new Insets(4, 0, 0, 0));

        VBox summaryCard = new VBox(12, 
            nameLabel, 
            categoryLabel, 
            new Separator(),
            new Label("建筑简介") {{ getStyleClass().add("section-caption"); }},
            descLabel, 
            mainActions,
            secondaryActions
        );
        summaryCard.getStyleClass().addAll("section-card", "section-card-emphasis");

        // --- Comments Section ---
        Label commentTitle = new Label("用户互动");
        commentTitle.getStyleClass().add("section-caption");
        Label commentHint = new Label("查看评价并参与评论。");
        commentHint.getStyleClass().add("muted-text");
        
        commentList = new ListView<>();
        commentList.getStyleClass().add("compact-list");
        commentList.setPrefHeight(220);
        commentList.setMinHeight(0); // 确保在窗口缩小后可以自适应压缩，避免挤占底部控件
        VBox.setVgrow(commentList, Priority.ALWAYS);
        // ... (Cell factory stays the same as it's already functional)
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
                usernameLbl.getStyleClass().add("comment-username");
                timeLbl.getStyleClass().add("comment-time");
                starsLbl.getStyleClass().add("comment-stars");
                contentLbl.getStyleClass().add("comment-content");
                likeBtn.getStyleClass().add("btn-ghost");
                likeCountLbl.getStyleClass().add("comment-like-count");
                deleteBtn.getStyleClass().addAll("btn-ghost", "danger-text-button");

                HBox topRow = new HBox(6);
                Region spacer1 = new Region();
                HBox.setHgrow(spacer1, Priority.ALWAYS);
                topRow.getChildren().addAll(usernameLbl, starsLbl, spacer1, timeLbl);

                HBox actionRow = new HBox(6);
                Region spacer2 = new Region();
                HBox.setHgrow(spacer2, Priority.ALWAYS);
                actionRow.getChildren().addAll(likeBtn, likeCountLbl, spacer2, deleteBtn);

                cellBox.getStyleClass().add("comment-cell");
                cellBox.getChildren().addAll(topRow, contentLbl, actionRow);
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
                    boolean loggedIn = currentUser != null;
                    likeBtn.setText(item.isLikedByMe() ? "已赞" : "点赞");
                    likeBtn.setVisible(loggedIn);
                    likeBtn.setManaged(loggedIn);
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
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "确定要删除这条评论吗？");
                alert.setTitle("删除评论");
                alert.setHeaderText(null);
                if (alert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
                boolean isAdmin = "ADMIN".equals(currentUser.getRole());
                if (commentService.deleteComment(currentComment.getId(), currentUser.getId(), isAdmin)) {
                    loadComments();
                    if (onDataChanged != null) onDataChanged.run();
                }
            }
        });

        commentInput = new TextArea();
        commentInput.setPromptText("写下你的看法");
        commentInput.setPrefRowCount(3);
        commentInput.textProperty().addListener((obs, old, text) -> {
            if (text.length() > 200) {
                commentInput.setText(text.substring(0, 200));
            }
        });

        HBox commentBottom = new HBox(10);
        ratingBox = new ComboBox<>();
        ratingBox.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5));
        ratingBox.setValue(5);
        ratingBox.setPrefWidth(70);

        submitBtn = new Button("发表");
        submitBtn.getStyleClass().addAll("button", "btn-primary");
        submitBtn.setOnAction(e -> submitComment());

        msgLabel = new Label();
        msgLabel.getStyleClass().addAll("success-text", "panel-message");

        Label ratingLabel = new Label("评分");
        ratingLabel.getStyleClass().add("field-label");
        commentBottom.getChildren().addAll(ratingLabel, ratingBox, submitBtn);
        commentBottom.setAlignment(Pos.CENTER_LEFT);

        VBox commentSection = new VBox(8, commentTitle, commentHint, commentList);
        commentSection.getStyleClass().add("section-card");
        VBox.setVgrow(commentList, Priority.ALWAYS);

        Label inputTitle = new Label("写下你的评论");
        inputTitle.getStyleClass().add("section-caption");
        VBox composerCard = new VBox(8, inputTitle, commentInput, commentBottom, msgLabel);
        composerCard.getStyleClass().add("section-card");

        getChildren().addAll(headerRow, summaryCard, commentSection, composerCard);
        VBox.setVgrow(commentSection, Priority.ALWAYS);

        setVisible(false);
    }

    public void showBuilding(Building building, User user) {
        this.currentBuilding = building;
        this.currentUser = user;
        nameLabel.setText(building.getName());
        categoryLabel.setText(CAT_NAMES.getOrDefault(building.getCategory(), building.getCategory()));
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
            msgLabel.getStyleClass().setAll("error-text");
            msgLabel.setText("请先登录");
            return;
        }
        int uid = currentUser.getId();
        int bid = currentBuilding.getId();
        boolean isFav = favoriteDAO.isFavorited(uid, bid);
        if (isFav) {
            favoriteDAO.deleteByUserAndBuilding(uid, bid);
            msgLabel.getStyleClass().setAll("info-label");
            msgLabel.setText("已取消收藏");
        } else {
            favoriteDAO.save(new edu.chd.campusmap.model.Favorite(uid, bid));
            msgLabel.getStyleClass().setAll("success-text");
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
        if (isFav) {
            favoriteBtn.setText("★ 取消收藏");
            favoriteBtn.getStyleClass().setAll("button", "btn-secondary");
        } else {
            favoriteBtn.setText("☆ 收藏");
            favoriteBtn.getStyleClass().setAll("button", "btn-warning");
        }
    }

    private void submitComment() {
        if (currentUser == null) {
            msgLabel.getStyleClass().setAll("error-text");
            msgLabel.setText("请先登录");
            return;
        }
        String content = commentInput.getText().trim();
        if (content.isEmpty()) {
            msgLabel.getStyleClass().setAll("error-text");
            msgLabel.setText("评论内容不能为空");
            return;
        }
        int rating = ratingBox.getValue() != null ? ratingBox.getValue() : 5;
        if (commentService.addComment(currentUser.getId(), currentBuilding.getId(), content, rating)) {
            msgLabel.getStyleClass().setAll("success-text");
            msgLabel.setText("评论成功");
            commentInput.clear();
            ratingBox.setValue(5);
            loadComments();
            if (onDataChanged != null) onDataChanged.run();
        } else {
            msgLabel.getStyleClass().setAll("error-text");
            msgLabel.setText("评论失败，请重试");
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
