package edu.chd.campusmap;

import edu.chd.campusmap.controller.MapController;
import edu.chd.campusmap.controller.SearchController;
import edu.chd.campusmap.controller.UserController;
import edu.chd.campusmap.model.Building;
import edu.chd.campusmap.model.User;
import edu.chd.campusmap.util.DatabaseInitializer;
import edu.chd.campusmap.view.*;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

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

    @Override
    public void start(Stage primaryStage) {
        // 自动初始化数据库（无数据时自动建表 + 插入示例数据）
        DatabaseInitializer.init();

        root = new StackPane();
        Scene scene = new Scene(root, 1200, 800);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

        primaryStage.setTitle("长安大学校园服务地图系统");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);

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
        root.getChildren().setAll(loginView);
    }

    private void showRegisterView() {
        if (registerView == null) {
            registerView = new RegisterView(userController);
            registerView.setOnRegisterSuccess(this::showLoginView);
            registerView.setOnSwitchToLogin(this::showLoginView);
        }
        registerView.reset();
        root.getChildren().setAll(registerView);
    }

    private void showMainMap() {
        User currentUser = userController.getCurrentUser();

        mainLayout = new BorderPane();

        mainMapView = new MainMapView(mapController, currentUser.getId());
        searchPanel = new SearchPanel(searchController);
        detailPanel = new DetailPanel();
        navigationPanel = new NavigationPanel();
        detailPanel.setMapView(mainMapView);

        // search → map select
        searchPanel.setSearchListener(building -> {
            mapController.selectBuilding(building);
            showDetailForBuilding(building);
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
            double dist = mainMapView.calculateRouteDistance(path);

            // 生成步骤列表
            java.util.List<String> steps = new java.util.ArrayList<>();
            steps.add("从 " + from.getName() + " 出发");
            steps.add("步行 " + (dist >= 1000 ? String.format("%.2f 公里", dist / 1000) : String.format("%.0f 米", dist)));
            steps.add("到达 " + to.getName());

            navigationPanel.showRoute(from, to, dist, steps);
            navigationPanel.setVisible(true);
            searchPanel.setVisible(false);
            detailPanel.setVisible(false);

            mainMapView.updateStatus("已生成路径，总距离 " + (dist >= 1000 ? String.format("%.2f 公里", dist / 1000) : String.format("%.0f 米", dist)));
        });

        detailPanel.setOnNavigate(() -> {
            Building b = detailPanel.getCurrentBuilding();
            if (b != null) {
                searchPanel.switchToNavMode(b);
                searchPanel.setVisible(true);
                detailPanel.setVisible(false);
                navigationPanel.setVisible(false);
            }
        });

        navigationPanel.setOnExit(() -> {
            mainMapView.clearNavigation();
            navigationPanel.setVisible(false);
            searchPanel.setVisible(true);
            detailPanel.setVisible(false);
        });

        navigationPanel.setOnBack(() -> {
            navigationPanel.setVisible(false);
            searchPanel.setVisible(true);
            detailPanel.setVisible(false);
        });

        mainLayout.setCenter(mainMapView.getRoot().getCenter());
        mainLayout.setBottom(mainMapView.getRoot().getBottom());
        mainLayout.setTop(mainMapView.getRoot().getTop());

        // right side: stack search + detail + nav
        rightPanel = new StackPane(searchPanel, detailPanel, navigationPanel);
        mainLayout.setRight(rightPanel);

        // user info & buttons
        mainMapView.setUserName(currentUser.getUsername());
        mainMapView.getUserCenterBtn().setOnAction(e -> showUserCenter());
        mainMapView.getLogoutBtn().setOnAction(e -> logout());
        mainMapView.setUserControlsVisible(true);

        root.getChildren().setAll(mainLayout);
    }

    private void showDetailForBuilding(Building building) {
        User user = userController.isLoggedIn() ? userController.getCurrentUser() : null;
        detailPanel.showBuilding(building, user);
        detailPanel.setVisible(true);
        searchPanel.setVisible(false);
        if (navigationPanel != null) navigationPanel.setVisible(false);
    }

    private void showSearchPanel() {
        searchPanel.setVisible(true);
        detailPanel.setVisible(false);
        if (navigationPanel != null) navigationPanel.setVisible(false);
    }

    private void showUserCenter() {
        User currentUser = userController.getCurrentUser();
        if (currentUser == null) return;

        UserCenterView userCenterView = new UserCenterView(currentUser);
        userCenterView.setOnLogout(this::logout);
        userCenterView.setOnClose(this::showMainMapAfterUserCenter);

        root.getChildren().setAll(userCenterView);
    }

    private void showMainMapAfterUserCenter() {
        if (mainLayout != null) {
            root.getChildren().setAll(mainLayout);
            detailPanel.refresh();
        }
    }

    private void logout() {
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
