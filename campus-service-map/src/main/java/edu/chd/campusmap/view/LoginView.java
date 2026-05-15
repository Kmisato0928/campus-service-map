package edu.chd.campusmap.view;

import edu.chd.campusmap.controller.UserController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

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
        this.setSpacing(15);
        this.setPadding(new Insets(40));
        this.setStyle("-fx-background-color: #ecf0f1;");

        Label title = new Label("长安大学校园服务地图");
        title.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 24));
        title.setStyle("-fx-text-fill: #1a5276;");

        Label subtitle = new Label("用户登录");
        subtitle.setFont(Font.font("Microsoft YaHei", 14));
        subtitle.setStyle("-fx-text-fill: #566573;");

        VBox form = new VBox(10);
        form.setMaxWidth(320);
        form.setPadding(new Insets(20));
        form.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 4);");

        Label userLabel = new Label("用户名");
        usernameField = new TextField();
        usernameField.setPromptText("请输入用户名");

        Label passLabel = new Label("密码");
        passwordField = new PasswordField();
        passwordField.setPromptText("请输入密码");

        Button loginBtn = new Button("登  录");
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setStyle("-fx-background-color: #2e86c1; -fx-text-fill: white; -fx-font-size: 15px; -fx-padding: 8 0;");

        messageLabel = new Label();
        messageLabel.setStyle("-fx-text-fill: #e74c3c;");

        Hyperlink registerLink = new Hyperlink("还没有账号？立即注册");
        registerLink.setAlignment(Pos.CENTER);

        form.getChildren().addAll(userLabel, usernameField, passLabel, passwordField, loginBtn, messageLabel, registerLink);

        this.getChildren().addAll(title, subtitle, form);

        loginBtn.setOnAction(e -> doLogin());
        passwordField.setOnAction(e -> doLogin());
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
            messageLabel.setStyle("-fx-text-fill: #27ae60;");
            messageLabel.setText("登录成功！");
            if (onLoginSuccess != null) onLoginSuccess.run();
        } else {
            messageLabel.setStyle("-fx-text-fill: #e74c3c;");
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
