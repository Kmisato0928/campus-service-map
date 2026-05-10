package edu.chd.campusmap.service;

import edu.chd.campusmap.dao.UserDAO;
import edu.chd.campusmap.model.User;
import edu.chd.campusmap.util.PasswordUtil;
import edu.chd.campusmap.util.UserSession;

public class UserService {
    private final UserDAO userDAO;

    public UserService() {
        this.userDAO = new UserDAO();
    }

    public boolean register(String username, String password, String email) {
        if (username == null || username.trim().isEmpty()) return false;
        if (password == null || password.length() < 6) return false;
        if (userDAO.findByUsername(username) != null) return false;

        User user = new User(username, PasswordUtil.hash(password), email);
        return userDAO.save(user);
    }

    public boolean login(String username, String password) {
        User user = userDAO.findByUsername(username);
        if (user == null) return false;
        if (!PasswordUtil.verify(password, user.getPassword())) return false;
        UserSession.getInstance().login(user);
        return true;
    }

    public User getUserById(int id) {
        return userDAO.findById(id);
    }
}
