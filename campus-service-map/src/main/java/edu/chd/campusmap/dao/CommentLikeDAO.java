package edu.chd.campusmap.dao;

import edu.chd.campusmap.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CommentLikeDAO {

    // MySQL 表不存在的错误码
    private static final int TABLE_NOT_FOUND = 1146;

    private boolean isTableNotFound(SQLException e) {
        return e.getErrorCode() == TABLE_NOT_FOUND;
    }

    /**
     * 点赞
     */
    public boolean add(int userId, int commentId) {
        String sql = "INSERT IGNORE INTO comment_likes (user_id, comment_id) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, commentId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            if (isTableNotFound(e)) return false;
            throw new RuntimeException("点赞失败", e);
        }
    }

    /**
     * 取消点赞
     */
    public boolean remove(int userId, int commentId) {
        String sql = "DELETE FROM comment_likes WHERE user_id=? AND comment_id=?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, commentId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            if (isTableNotFound(e)) return false;
            throw new RuntimeException("取消点赞失败", e);
        }
    }

    /**
     * 查询用户是否已点赞
     */
    public boolean isLiked(int userId, int commentId) {
        String sql = "SELECT 1 FROM comment_likes WHERE user_id=? AND comment_id=?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, commentId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            if (isTableNotFound(e)) return false;
            throw new RuntimeException("查询点赞状态失败", e);
        }
    }

    /**
     * 获取评论点赞数
     */
    public int countByComment(int commentId) {
        String sql = "SELECT COUNT(*) FROM comment_likes WHERE comment_id=?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, commentId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            if (isTableNotFound(e)) return 0;
            throw new RuntimeException("查询点赞数失败", e);
        }
        return 0;
    }
}
