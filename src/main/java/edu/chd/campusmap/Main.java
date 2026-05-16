package edu.chd.campusmap;

import edu.chd.campusmap.controller.MapController;
import edu.chd.campusmap.controller.SearchController;
import edu.chd.campusmap.controller.UserController;
import edu.chd.campusmap.model.Building;
import edu.chd.campusmap.model.User;
import edu.chd.campusmap.util.DatabaseInitializer;
import edu.chd.campusmap.view.MainMapView;
import edu.chd.campusmap.view.SearchPanel;
import edu.chd.campusmap.view.DetailPanel;
import edu.chd.campusmap.view.NavigationPanel;
import edu.chd.campusmap.view.LoginView;
import edu.chd.campusmap.view.RegisterView;
import edu.chd.campusmap.view.UserCenterView;
import javafx.application.Application;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

import java.util.List;

public class Main extends Application {

    private final UserController userController = new UserController();
    private final MapController mapController = new MapController();
    private final SearchController searchController = new SearchController();

    private StackPane root;
    private LoginView loginView;
    private RegisterView registerView;

    private BorderPane mainLayout;
    private MainMapView mainMapView;
    private SearchPanel searchPanel;
    private DetailPanel detailPanel;
    private NavigationPanel navigationPanel;
    private StackPane rightPanel;
    private boolean sidePanelCollapsed;

    @Override
    public void start(Stage primaryStage) {
        // 自动初始化数据库（无数据时自动建表 + 插入示例数据）
        DatabaseInitializer.init();

        root = new StackPane();
        Scene scene = new Scene(root, 1160, 700);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

        primaryStage.setTitle("长安大学校园服务地图系统");
        
        // 替换窗口系统默认图标为长安大学校徽
        try {
            java.io.InputStream iconStream = getClass().getResourceAsStream("/image/xh2.png");
            if (iconStream != null) {
                primaryStage.getIcons().add(new javafx.scene.image.Image(iconStream));
            }
        } catch (Exception e) {
            System.err.println("图标加载失败: " + e.getMessage());
        }
        
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(980);
        primaryStage.setMinHeight(620);

        // 关闭窗口时确保 JavaFX 退出（清理 tileLoader 等非守护线程）
        primaryStage.setOnCloseRequest(e -> {
            javafx.application.Platform.exit();
        });

        showLoginView();
        primaryStage.show();
    }

    private void showLoginView() {
        if (loginView == null) {
            loginView = new LoginView(userController);
            loginView.setOnLoginSuccess(this::showMainMap);
            loginView.setOnSwitchToRegister(this::showRegisterView);
        }
        loginView.reset();
        loginView.setOpacity(0);
        root.getChildren().setAll(loginView);
        edu.chd.campusmap.util.AnimationUtil.fadeIn(loginView).play();
    }

    private void showRegisterView() {
        if (registerView == null) {
            registerView = new RegisterView(userController);
            registerView.setOnRegisterSuccess(this::showLoginView);
            registerView.setOnSwitchToLogin(this::showLoginView);
        }
        registerView.reset();
        registerView.setOpacity(0);
        root.getChildren().setAll(registerView);
        edu.chd.campusmap.util.AnimationUtil.fadeIn(registerView).play();
    }

    private void showMainMap() {
        User currentUser = userController.getCurrentUser();

        mainMapView = new MainMapView(mapController, currentUser.getId());
        mainLayout = mainMapView.getRoot();
        mainLayout.setMinHeight(0); // 确保整个主布局能够随窗口自由收缩
        searchPanel = new SearchPanel(searchController);
        detailPanel = new DetailPanel();
        navigationPanel = new NavigationPanel();
        detailPanel.setMapView(mainMapView);

        // search → map select (selectBuilding 会触发 observer，不必再显式调用 showDetailForBuilding)
        searchPanel.setSearchListener(building -> {
            mapController.selectBuilding(building);
        });

        // map marker click → detail
        mapController.addObserver(building -> {
            System.out.println("[Main] Observer fired: " + building.getName() + " id=" + building.getId());
            showDetailForBuilding(building);
        });

        // detail panel back button
        detailPanel.setOnBack(this::showSearchPanel);

        // detail panel data changed (edit/favorite/comment) → update map markers + sync list
        detailPanel.setOnDataChanged(() -> {
            Building b = detailPanel.getCurrentBuilding();
            if (b != null) {
                mainMapView.updateBuildingInList(b);
                mainMapView.updateMarkerOnMap(b);
            }
        });

        // ---- 导航回调 ----
        searchPanel.setOnPlanRoute((from, to) -> {
            List<double[]> path = mainMapView.calculateRoute(from, to);
            if (path.isEmpty()) {
                mainMapView.updateStatus("无法找到路径，请稍后重试");
                return;
            }
            mainMapView.showNavigationPath(path);
            mainMapView.setActivePanel("route");
            double dist = mainMapView.calculateRouteDistance(path);

            // 生成步骤列表
            java.util.List<String> steps = new java.util.ArrayList<>();
            steps.add("从 " + from.getName() + " 出发");
            steps.add("步行 " + (dist >= 1000 ? String.format("%.2f 公里", dist / 1000) : String.format("%.0f 米", dist)));
            steps.add("到达 " + to.getName());

            navigationPanel.showRoute(from, to, dist, steps);
            
            if (!navigationPanel.isVisible()) {
                navigationPanel.setVisible(true);
                navigationPanel.setOpacity(0);
                edu.chd.campusmap.util.AnimationUtil.slideInRight(navigationPanel).play();
            }
            
            searchPanel.setVisible(false);
            detailPanel.setVisible(false);

            mainMapView.updateStatus("已生成路径，总距离 " + (dist >= 1000 ? String.format("%.2f 公里", dist / 1000) : String.format("%.0f 米", dist)));
        });

        detailPanel.setOnNavigate(() -> {
            Building b = detailPanel.getCurrentBuilding();
            if (b != null) {
                searchPanel.switchToNavMode(b);
                if (!searchPanel.isVisible()) {
                    searchPanel.setVisible(true);
                    searchPanel.setOpacity(0);
                    edu.chd.campusmap.util.AnimationUtil.slideInRight(searchPanel).play();
                }
                detailPanel.setVisible(false);
                navigationPanel.setVisible(false);
                mainMapView.setActivePanel("route");
            }
        });

        navigationPanel.setOnExit(() -> {
            mainMapView.clearNavigation();
            navigationPanel.setVisible(false);
            if (!searchPanel.isVisible()) {
                searchPanel.setVisible(true);
                searchPanel.setOpacity(0);
                edu.chd.campusmap.util.AnimationUtil.slideInRight(searchPanel).play();
            }
            detailPanel.setVisible(false);
        });

        navigationPanel.setOnBack(() -> {
            navigationPanel.setVisible(false);
            if (!searchPanel.isVisible()) {
                searchPanel.setVisible(true);
                searchPanel.setOpacity(0);
                edu.chd.campusmap.util.AnimationUtil.slideInRight(searchPanel).play();
            }
            detailPanel.setVisible(false);
        });

        // right side: stack search + detail + nav
        rightPanel = new StackPane(searchPanel, detailPanel, navigationPanel);
        rightPanel.setMinHeight(0);
        mainLayout.setRight(rightPanel);
        sidePanelCollapsed = false;

        // user info & buttons
        mainMapView.setUserName(currentUser.getUsername());
        mainMapView.getPanelToggleBtn().setOnAction(e -> toggleSidePanel());
        mainMapView.getSearchPanelBtn().setOnAction(e -> showSearchPanelFromTopNav());
        mainMapView.getDetailPanelBtn().setOnAction(e -> showDetailPanelFromTopNav());
        mainMapView.getRoutePanelBtn().setOnAction(e -> showRoutePanelFromTopNav());
        mainMapView.getUserCenterBtn().setOnAction(e -> showUserCenter());
        mainMapView.getLogoutBtn().setOnAction(e -> logout());
        mainMapView.setUserControlsVisible(true);
        mainMapView.setSidePanelCollapsed(false);

        root.getChildren().setAll(mainLayout);
    }

    private void showDetailForBuilding(Building building) {
        ensureSidePanelVisible();
        User user = userController.isLoggedIn() ? userController.getCurrentUser() : null;
        detailPanel.showBuilding(building, user);
        mainMapView.setActivePanel("detail");
        
        if (!detailPanel.isVisible()) {
            detailPanel.setVisible(true);
            detailPanel.setOpacity(0);
            edu.chd.campusmap.util.AnimationUtil.slideInRight(detailPanel).play();
        }
        
        searchPanel.setVisible(false);
        if (navigationPanel != null) navigationPanel.setVisible(false);
    }

    private void showSearchPanel() {
        ensureSidePanelVisible();
        if (!searchPanel.isVisible()) {
            searchPanel.setVisible(true);
            searchPanel.setOpacity(0);
            edu.chd.campusmap.util.AnimationUtil.slideInRight(searchPanel).play();
        }
        detailPanel.setVisible(false);
        if (navigationPanel != null) navigationPanel.setVisible(false);
        mainMapView.setActivePanel("search");
    }

    private void showSearchPanelFromTopNav() {
        searchPanel.switchToSearchMode();
        showSearchPanel();
    }

    private void showDetailPanelFromTopNav() {
        Building building = detailPanel.getCurrentBuilding();
        if (building == null) {
            mainMapView.updateStatus("请先从地图或搜索结果中选择一个建筑");
            showSearchPanelFromTopNav();
            return;
        }
        showDetailForBuilding(building);
    }

    private void showRoutePanelFromTopNav() {
        ensureSidePanelVisible();
        Building building = detailPanel.getCurrentBuilding();
        searchPanel.switchToNavMode(building);
        if (!searchPanel.isVisible()) {
            searchPanel.setVisible(true);
            searchPanel.setOpacity(0);
            edu.chd.campusmap.util.AnimationUtil.slideInRight(searchPanel).play();
        }
        detailPanel.setVisible(false);
        if (navigationPanel != null) navigationPanel.setVisible(false);
        mainMapView.setActivePanel("route");
        mainMapView.updateStatus(building == null ? "请选择起点和终点后生成路线" : "已带入当前建筑，可继续选择起点生成路线");
    }

    private void toggleSidePanel() {
        if (sidePanelCollapsed) {
            ensureSidePanelVisible();
            return;
        }
        mainLayout.setRight(null);
        sidePanelCollapsed = true;
        mainMapView.setSidePanelCollapsed(true);
    }

    private void ensureSidePanelVisible() {
        if (!sidePanelCollapsed) {
            return;
        }
        mainLayout.setRight(rightPanel);
        sidePanelCollapsed = false;
        mainMapView.setSidePanelCollapsed(false);
    }

    private void showUserCenter() {
        User currentUser = userController.getCurrentUser();
        if (currentUser == null) return;

        UserCenterView userCenterView = new UserCenterView(currentUser);
        userCenterView.setOnLogout(this::logout);
        userCenterView.setOnClose(this::showMainMapAfterUserCenter);

        userCenterView.setOpacity(0);
        root.getChildren().setAll(userCenterView);
        edu.chd.campusmap.util.AnimationUtil.fadeIn(userCenterView).play();
    }

    private void showMainMapAfterUserCenter() {
        if (mainLayout != null) {
            mainLayout.setOpacity(0);
            root.getChildren().setAll(mainLayout);
            edu.chd.campusmap.util.AnimationUtil.fadeIn(mainLayout).play();
            detailPanel.refresh();
        }
    }

    private void logout() {
        Alert alert = new Alert(Alert.AlertType.WARNING, "\u786e\u5b9a\u8981\u9000\u51fa\u767b\u5f55\u5417\uFF1F");
        alert.setTitle("\u9000\u51fa\u7cfb\u7edf");
        alert.setHeaderText("\u9000\u51fa\u767b\u5f55");
        
        // 自定义警告图标（橙色感叹号）
        javafx.scene.shape.SVGPath warningIcon = new javafx.scene.shape.SVGPath();
        warningIcon.setContent("M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z");
        warningIcon.setFill(javafx.scene.paint.Color.web("#f0a11f"));
        javafx.scene.layout.StackPane iconPane = new javafx.scene.layout.StackPane(warningIcon);
        iconPane.setPrefSize(48, 48);
        warningIcon.setScaleX(1.8);
        warningIcon.setScaleY(1.8);
        alert.setGraphic(iconPane);

        // 应用全局样式
        if (root.getScene() != null) {
            alert.getDialogPane().getStylesheets().addAll(root.getScene().getStylesheets());
        }
        alert.getDialogPane().getStyleClass().add("custom-alert");

        ButtonType confirmBtn = new ButtonType("\u9000\u51fa", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn = new ButtonType("\u53d6\u6d88", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(confirmBtn, cancelBtn);

        Button okBtn = (Button) alert.getDialogPane().lookupButton(confirmBtn);
        if (okBtn != null) {
            okBtn.getStyleClass().addAll("button", "btn-danger");
        }
        Button cancelBtnNode = (Button) alert.getDialogPane().lookupButton(cancelBtn);
        if (cancelBtnNode != null) {
            cancelBtnNode.getStyleClass().addAll("button", "btn-outline");
        }

        if (alert.showAndWait().orElse(cancelBtn) != confirmBtn) return;

        userController.logout();
        showSearchPanel();
        showLoginView();
    }

    public static void main(String[] args) {
        // 捕获所有未处理的异常
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            System.err.println("=== 未捕获异常 in " + thread.getName() + " ===");
            throwable.printStackTrace();
      });
        launch(args);
    }
}
