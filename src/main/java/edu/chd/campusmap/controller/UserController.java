package edu.chd.campusmap.controller;

import edu.chd.campusmap.model.User;
import edu.chd.campusmap.service.UserService;

public class UserController {
    private final UserService userService;

    public UserController() {
        this.userService = new UserService();
    }

    public boolean register(String username, String password, String email) {
        return userService.register(username, password, email);
    }

    public boolean login(String username, String password) {
        return userService.login(username, password);
    }

    public User getCurrentUser() {
        return edu.chd.campusmap.util.UserSession.getInstance().getCurrentUser();
    }

    public boolean isLoggedIn() {
        return edu.chd.campusmap.util.UserSession.getInstance().isLoggedIn();
    }

    public void logout() {
        edu.chd.campusmap.util.UserSession.getInstance().logout();
    }
}
