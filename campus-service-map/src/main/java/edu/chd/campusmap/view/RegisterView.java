package edu.chd.campusmap.view;

import edu.chd.campusmap.controller.UserController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

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
        this.setSpacing(15);
        this.setPadding(new Insets(40));
        this.setStyle("-fx-background-color: #ecf0f1;");

        Label title = new Label("长安大学校园服务地图");
        title.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 24));
        title.setStyle("-fx-text-fill: #1a5276;");

        Label subtitle = new Label("用户注册");
        subtitle.setFont(Font.font("Microsoft YaHei", 14));
        subtitle.setStyle("-fx-text-fill: #566573;");

        VBox form = new VBox(10);
        form.setMaxWidth(320);
        form.setPadding(new Insets(20));
        form.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 4);");

        Label userLabel = new Label("用户名");
        usernameField = new TextField();
        usernameField.setPromptText("请输入用户名");

        Label emailLabel = new Label("邮箱");
        emailField = new TextField();
        emailField.setPromptText("请输入邮箱（选填）");

        Label passLabel = new Label("密码");
        passwordField = new PasswordField();
        passwordField.setPromptText("请输入密码（至少6位）");

        Label confirmLabel = new Label("确认密码");
        confirmField = new PasswordField();
        confirmField.setPromptText("请再次输入密码");

        Button registerBtn = new Button("注  册");
        registerBtn.setMaxWidth(Double.MAX_VALUE);
        registerBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 15px; -fx-padding: 8 0;");

        messageLabel = new Label();
        messageLabel.setStyle("-fx-text-fill: #e74c3c;");

        Hyperlink loginLink = new Hyperlink("已有账号？返回登录");
        loginLink.setAlignment(Pos.CENTER);

        form.getChildren().addAll(userLabel, usernameField, emailLabel, emailField,
                passLabel, passwordField, confirmLabel, confirmField,
                registerBtn, messageLabel, loginLink);

        this.getChildren().addAll(title, subtitle, form);

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

        if (userController.register(username, password, email)) {
            messageLabel.setStyle("-fx-text-fill: #27ae60;");
            messageLabel.setText("注册成功！请登录");
            if (onRegisterSuccess != null) onRegisterSuccess.run();
        } else {
            messageLabel.setStyle("-fx-text-fill: #e74c3c;");
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
