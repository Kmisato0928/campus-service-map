package edu.chd.campusmap.view;

import edu.chd.campusmap.controller.UserController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class RegisterView extends VBox {
    private final UserController userController;
    private final TextField usernameField;
    private final PasswordField passwordField;
    private final PasswordField confirmField;
    private final TextField emailField;
    private final Label messageLabel;
    private Runnable onRegisterSuccess;
    private Runnable onSwitchToLogin;

    public RegisterView(UserController userController) {
        this.userController = userController;
        this.setAlignment(Pos.CENTER);
        this.setSpacing(0);
        this.setPadding(new Insets(0));
        this.getStyleClass().add("auth-shell");

        Label formBadge = new Label("新用户注册");
        formBadge.getStyleClass().add("auth-badge");

        Label brandTitle = new Label("创建你的校园地图账号");
        brandTitle.getStyleClass().add("auth-hero-title");
        Label brandSubtitle = new Label("填写信息后即可完成注册。");
        brandSubtitle.getStyleClass().add("auth-hero-subtitle");

        VBox headerBox = new VBox(8, formBadge, brandTitle, brandSubtitle);
        headerBox.getStyleClass().add("auth-card-header");

        VBox form = new VBox(10);
        form.getStyleClass().add("auth-form");

        Label userLabel = new Label("用户名");
        userLabel.getStyleClass().add("field-label");
        usernameField = new TextField();
        usernameField.setPromptText("请输入用户名");

        Label emailLabel = new Label("邮箱");
        emailLabel.getStyleClass().add("field-label");
        emailField = new TextField();
        emailField.setPromptText("请输入邮箱（选填）");

        Label passLabel = new Label("密码");
        passLabel.getStyleClass().add("field-label");
        passwordField = new PasswordField();
        passwordField.setPromptText("请输入密码（至少6位）");

        Label confirmLabel = new Label("确认密码");
        confirmLabel.getStyleClass().add("field-label");
        confirmField = new PasswordField();
        confirmField.setPromptText("请再次输入密码");

        Button registerBtn = new Button("创建并完成注册");
        registerBtn.setMaxWidth(Double.MAX_VALUE);
        registerBtn.getStyleClass().addAll("button", "btn-primary", "btn-lg");

        messageLabel = new Label();
        messageLabel.getStyleClass().addAll("error-text", "panel-message");

        Hyperlink loginLink = new Hyperlink("已有账号？返回登录");
        loginLink.setAlignment(Pos.CENTER);
        loginLink.getStyleClass().add("auth-switch-link");

        VBox userGroup = new VBox(6, userLabel, usernameField);
        VBox emailGroup = new VBox(6, emailLabel, emailField);
        VBox passGroup = new VBox(6, passLabel, passwordField);
        VBox confirmGroup = new VBox(6, confirmLabel, confirmField);

        HBox switchRow = new HBox(8, new Label("已经注册过？"), loginLink);
        switchRow.getStyleClass().add("auth-switch-row");
        switchRow.setAlignment(Pos.CENTER);

        form.getChildren().addAll(userGroup, emailGroup, passGroup, confirmGroup, registerBtn, messageLabel, switchRow);

        VBox formCard = new VBox(12, headerBox, form);
        formCard.getStyleClass().addAll("auth-card", "auth-card-compact");

        this.getChildren().add(formCard);

        registerBtn.setOnAction(e -> doRegister());
        confirmField.setOnAction(e -> doRegister());
        loginLink.setOnAction(e -> {
            if (onSwitchToLogin != null) onSwitchToLogin.run();
        });
    }

    private void doRegister() {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirm = confirmField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("用户名和密码不能为空");
            return;
        }
        if (password.length() < 6) {
            messageLabel.setText("密码至少6位");
            return;
        }
        if (!password.equals(confirm)) {
            messageLabel.setText("两次密码输入不一致");
            return;
        }
        if (!email.isEmpty() && !email.matches("^[\\w.+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            messageLabel.setText("邮箱格式不正确");
            return;
        }

        if (userController.register(username, password, email)) {
            messageLabel.getStyleClass().setAll("success-text");
            messageLabel.setText("注册成功！请登录");
            if (onRegisterSuccess != null) onRegisterSuccess.run();
        } else {
            messageLabel.getStyleClass().setAll("error-text");
            messageLabel.setText("注册失败，用户名可能已存在");
        }
    }

    public void setOnRegisterSuccess(Runnable onRegisterSuccess) {
        this.onRegisterSuccess = onRegisterSuccess;
    }

    public void setOnSwitchToLogin(Runnable onSwitchToLogin) {
        this.onSwitchToLogin = onSwitchToLogin;
    }

    public void reset() {
        usernameField.clear();
        passwordField.clear();
        confirmField.clear();
        emailField.clear();
        messageLabel.setText("");
    }
}
