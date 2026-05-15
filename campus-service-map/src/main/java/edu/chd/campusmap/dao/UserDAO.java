package edu.chd.campusmap.dao;

import edu.chd.campusmap.model.User;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO extends BaseDAO<User> {

    @Override
    protected String getInsertSql() {
        return "INSERT INTO users (username, password, email, role) VALUES (?, ?, ?, ?)";
    }

    @Override
    protected void setInsertParams(PreparedStatement stmt, User user) throws SQLException {
        stmt.setString(1, user.getUsername());
        stmt.setString(2, user.getPassword());
        stmt.setString(3, user.getEmail());
        stmt.setString(4, user.getRole() != null ? user.getRole() : "USER");
    }

    @Override
    protected String getUpdateSql() {
        return "UPDATE users SET username=?, password=?, email=?, role=? WHERE id=?";
    }

    @Override
    protected void setUpdateParams(PreparedStatement stmt, User user) throws SQLException {
        stmt.setString(1, user.getUsername());
        stmt.setString(2, user.getPassword());
        stmt.setString(3, user.getEmail());
        stmt.setString(4, user.getRole());
        stmt.setInt(5, user.getId());
    }

    @Override
    protected String getDeleteSql() {
        return "DELETE FROM users WHERE id=?";
    }

    @Override
    protected String getSelectByIdSql() {
        return "SELECT * FROM users WHERE id=?";
    }

    @Override
    protected User mapRow(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));
        user.setEmail(rs.getString("email"));
        user.setRole(rs.getString("role"));
        if (rs.getTimestamp("created_at") != null) {
            user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        }
        return user;
    }

    public User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username=?";
        try (var conn = getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("根据用户名查询失败", e);
        }
        return null;
    }
}
