package edu.chd.campusmap.dao;

import edu.chd.campusmap.model.BuildingOverride;
import edu.chd.campusmap.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class BuildingOverrideDAO {

    // H2/MySQL 表不存在的 SQLState (42S02)
    private static final String TABLE_NOT_FOUND = "42S02";

    private boolean isTableNotFound(SQLException e) {
        return TABLE_NOT_FOUND.equals(e.getSQLState());
    }

    /**
     * 查询用户对某建筑的覆盖记录
     */
    public BuildingOverride findByUserAndBuilding(int userId, int buildingId) {
        String sql = "SELECT * FROM user_building_overrides WHERE user_id=? AND building_id=?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, buildingId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            if (isTableNotFound(e)) return null;
            throw new RuntimeException("查询建筑覆盖失败", e);
        }
        return null;
    }

    /**
     * 保存或更新覆盖记录
     */
    public boolean save(BuildingOverride override) {
        String sql = "REPLACE INTO user_building_overrides (user_id, building_id, name, category, latitude, longitude, description) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, override.getUserId());
            stmt.setInt(2, override.getBuildingId());
            stmt.setString(3, override.getName());
            stmt.setString(4, override.getCategory());
            stmt.setDouble(5, override.getLatitude());
            stmt.setDouble(6, override.getLongitude());
            stmt.setString(7, override.getDescription());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            if (isTableNotFound(e)) return false;
            throw new RuntimeException("保存建筑覆盖失败", e);
        }
    }

    /**
     * 管理员编辑后，清除该建筑的所有用户覆盖
     */
    public void deleteByBuilding(int buildingId) {
        String sql = "DELETE FROM user_building_overrides WHERE building_id=?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, buildingId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            if (isTableNotFound(e)) return;
            throw new RuntimeException("清除建筑覆盖失败", e);
        }
    }

    /**
     * 删除用户的覆盖记录
     */
    public boolean deleteByUserAndBuilding(int userId, int buildingId) {
        String sql = "DELETE FROM user_building_overrides WHERE user_id=? AND building_id=?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, buildingId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            if (isTableNotFound(e)) return false;
            throw new RuntimeException("删除建筑覆盖失败", e);
        }
    }

    private BuildingOverride mapRow(ResultSet rs) throws SQLException {
        BuildingOverride o = new BuildingOverride();
        o.setId(rs.getInt("id"));
        o.setUserId(rs.getInt("user_id"));
        o.setBuildingId(rs.getInt("building_id"));
        o.setName(rs.getString("name"));
        o.setCategory(rs.getString("category"));
        o.setLatitude(rs.getDouble("latitude"));
        o.setLongitude(rs.getDouble("longitude"));
        o.setDescription(rs.getString("description"));
        return o;
    }
}
