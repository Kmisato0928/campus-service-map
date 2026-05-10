package edu.chd.campusmap.dao;

import edu.chd.campusmap.model.Favorite;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FavoriteDAO extends BaseDAO<Favorite> {

    @Override
    protected String getInsertSql() {
        return "INSERT INTO favorites (user_id, building_id) VALUES (?, ?)";
    }

    @Override
    protected void setInsertParams(PreparedStatement stmt, Favorite favorite) throws SQLException {
        stmt.setInt(1, favorite.getUserId());
        stmt.setInt(2, favorite.getBuildingId());
    }

    @Override
    protected String getUpdateSql() {
        return null;
    }

    @Override
    protected void setUpdateParams(PreparedStatement stmt, Favorite entity) throws SQLException {
    }

    @Override
    protected String getDeleteSql() {
        return "DELETE FROM favorites WHERE id=?";
    }

    @Override
    protected String getSelectByIdSql() {
        return "SELECT * FROM favorites WHERE id=?";
    }

    @Override
    protected Favorite mapRow(ResultSet rs) throws SQLException {
        Favorite favorite = new Favorite();
        favorite.setId(rs.getInt("id"));
        favorite.setUserId(rs.getInt("user_id"));
        favorite.setBuildingId(rs.getInt("building_id"));
        if (rs.getTimestamp("created_at") != null) {
            favorite.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        }
        return favorite;
    }

    public List<Favorite> findByUserId(int userId) {
        List<Favorite> list = new ArrayList<>();
        String sql = "SELECT f.*, b.name AS building_name, b.category AS building_category " +
                     "FROM favorites f JOIN buildings b ON f.building_id=b.id " +
                     "WHERE f.user_id=? ORDER BY f.created_at DESC";
        try (var conn = getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (var rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Favorite fav = mapRow(rs);
                    fav.setBuildingName(rs.getString("building_name"));
                    fav.setBuildingCategory(rs.getString("building_category"));
                    list.add(fav);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询收藏失败", e);
        }
        return list;
    }

    public boolean isFavorited(int userId, int buildingId) {
        String sql = "SELECT COUNT(*) FROM favorites WHERE user_id=? AND building_id=?";
        try (var conn = getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, buildingId);
            try (var rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询收藏状态失败", e);
        }
    }

    public boolean deleteByUserAndBuilding(int userId, int buildingId) {
        String sql = "DELETE FROM favorites WHERE user_id=? AND building_id=?";
        try (var conn = getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, buildingId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("取消收藏失败", e);
        }
    }
}
