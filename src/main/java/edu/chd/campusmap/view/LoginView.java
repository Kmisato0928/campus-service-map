package edu.chd.campusmap.view;

import edu.chd.campusmap.controller.UserController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class LoginView extends VBox {
    private final UserController userController;
    private final TextField usernameField;
    private final PasswordField passwordField;
    private final Label messageLabel;
    private Runnable onLoginSuccess;
    private Runnable onSwitchToRegister;

    public LoginView(UserController userController) {
        this.userController = userController;
        this.setAlignment(Pos.CENTER);
        this.setSpacing(0);
        this.setPadding(new Insets(0));
        this.getStyleClass().add("auth-shell");

        Label formBadge = new Label("账号登录");
        formBadge.getStyleClass().add("auth-badge");

        Label brandTitle = new Label("长安大学校园服务地图");
        brandTitle.getStyleClass().add("auth-hero-title");
        Label brandSubtitle = new Label("输入账号后即可进入系统。");
        brandSubtitle.getStyleClass().add("auth-hero-subtitle");

        VBox headerBox = new VBox(8, formBadge, brandTitle, brandSubtitle);
        headerBox.getStyleClass().add("auth-card-header");

        VBox form = new VBox(10);
        form.getStyleClass().add("auth-form");

        Label userLabel = new Label("用户名");
        userLabel.getStyleClass().add("field-label");
        usernameField = new TextField();
        usernameField.setPromptText("请输入用户名");

        Label passLabel = new Label("密码");
        passLabel.getStyleClass().add("field-label");
        passwordField = new PasswordField();
        passwordField.setPromptText("请输入密码");

        Button loginBtn = new Button("进入校园地图");
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.getStyleClass().addAll("button", "btn-primary", "btn-lg");

        messageLabel = new Label();
        messageLabel.getStyleClass().addAll("error-text", "panel-message");

        Hyperlink registerLink = new Hyperlink("还没有账号？立即注册");
        registerLink.setAlignment(Pos.CENTER);
        registerLink.getStyleClass().add("auth-switch-link");

        VBox userGroup = new VBox(6, userLabel, usernameField);
        VBox passGroup = new VBox(6, passLabel, passwordField);

        HBox switchRow = new HBox(8, new Label("第一次使用？"), registerLink);
        switchRow.getStyleClass().add("auth-switch-row");
        switchRow.setAlignment(Pos.CENTER);

        form.getChildren().addAll(userGroup, passGroup, loginBtn, messageLabel, switchRow);

        VBox formCard = new VBox(12, headerBox, form);
        formCard.getStyleClass().addAll("auth-card", "auth-card-compact");

        this.getChildren().add(formCard);

        loginBtn.setOnAction(e -> doLogin());
        passwordField.setOnAction(e -> doLogin());
        usernameField.setOnAction(e -> passwordField.requestFocus());
        registerLink.setOnAction(e -> {
            if (onSwitchToRegister != null) onSwitchToRegister.run();
        });
    }

    private void doLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("请输入用户名和密码");
            return;
        }

        if (userController.login(username, password)) {
            messageLabel.getStyleClass().setAll("success-text");
            messageLabel.setText("登录成功！");
            if (onLoginSuccess != null) onLoginSuccess.run();
        } else {
            messageLabel.getStyleClass().setAll("error-text");
            messageLabel.setText("用户名或密码错误");
        }
    }

    public void setOnLoginSuccess(Runnable onLoginSuccess) {
        this.onLoginSuccess = onLoginSuccess;
    }

    public void setOnSwitchToRegister(Runnable onSwitchToRegister) {
        this.onSwitchToRegister = onSwitchToRegister;
    }

    public void reset() {
        usernameField.clear();
        passwordField.clear();
        messageLabel.setText("");
    }
}
