package edu.chd.campusmap.dao;

import edu.chd.campusmap.model.Comment;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CommentDAO extends BaseDAO<Comment> {

    @Override
    protected String getInsertSql() {
        return "INSERT INTO comments (user_id, building_id, content, rating) VALUES (?, ?, ?, ?)";
    }

    @Override
    protected void setInsertParams(PreparedStatement stmt, Comment comment) throws SQLException {
        stmt.setInt(1, comment.getUserId());
        stmt.setInt(2, comment.getBuildingId());
        stmt.setString(3, comment.getContent());
        stmt.setInt(4, comment.getRating());
    }

    @Override
    protected String getUpdateSql() {
        return "UPDATE comments SET content=?, rating=? WHERE id=?";
    }

    @Override
    protected void setUpdateParams(PreparedStatement stmt, Comment comment) throws SQLException {
        stmt.setString(1, comment.getContent());
        stmt.setInt(2, comment.getRating());
        stmt.setInt(3, comment.getId());
    }

    @Override
    protected String getDeleteSql() {
        return "DELETE FROM comments WHERE id=?";
    }

    @Override
    protected String getSelectByIdSql() {
        return "SELECT c.*, u.username FROM comments c JOIN users u ON c.user_id=u.id WHERE c.id=?";
    }

    @Override
    protected Comment mapRow(ResultSet rs) throws SQLException {
        Comment comment = new Comment();
        comment.setId(rs.getInt("id"));
        comment.setUserId(rs.getInt("user_id"));
        comment.setBuildingId(rs.getInt("building_id"));
        comment.setContent(rs.getString("content"));
        comment.setRating(rs.getInt("rating"));
        if (rs.getTimestamp("created_at") != null) {
            comment.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        }
        try {
            comment.setUsername(rs.getString("username"));
        } catch (SQLException ignored) {
        }
        return comment;
    }

    public List<Comment> findByBuildingId(int buildingId) {
        List<Comment> list = new ArrayList<>();
        String sql = "SELECT c.*, u.username FROM comments c JOIN users u ON c.user_id=u.id WHERE c.building_id=? ORDER BY c.created_at DESC";
        try (var conn = getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, buildingId);
            try (var rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询评论失败", e);
        }
        return list;
    }

    public List<Comment> findByUserId(int userId) {
        List<Comment> list = new ArrayList<>();
        String sql = "SELECT c.*, u.username FROM comments c JOIN users u ON c.user_id=u.id WHERE c.user_id=? ORDER BY c.created_at DESC";
        try (var conn = getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (var rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询用户评论失败", e);
        }
        return list;
    }
}
