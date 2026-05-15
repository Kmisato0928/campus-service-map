package edu.chd.campusmap.dao;

import edu.chd.campusmap.model.Building;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BuildingDAO extends BaseDAO<Building> {

    @Override
    protected String getInsertSql() {
        return "INSERT INTO buildings (name, category, latitude, longitude, description) VALUES (?, ?, ?, ?, ?)";
    }

    @Override
    protected void setInsertParams(PreparedStatement stmt, Building building) throws SQLException {
        stmt.setString(1, building.getName());
        stmt.setString(2, building.getCategory());
        stmt.setDouble(3, building.getLatitude());
        stmt.setDouble(4, building.getLongitude());
        stmt.setString(5, building.getDescription());
    }

    @Override
    protected String getUpdateSql() {
        return "UPDATE buildings SET name=?, category=?, latitude=?, longitude=?, description=? WHERE id=?";
    }

    @Override
    protected void setUpdateParams(PreparedStatement stmt, Building building) throws SQLException {
        stmt.setString(1, building.getName());
        stmt.setString(2, building.getCategory());
        stmt.setDouble(3, building.getLatitude());
        stmt.setDouble(4, building.getLongitude());
        stmt.setString(5, building.getDescription());
        stmt.setInt(6, building.getId());
    }

    @Override
    protected String getDeleteSql() {
        return "DELETE FROM buildings WHERE id=?";
    }

    @Override
    protected String getSelectByIdSql() {
        return "SELECT * FROM buildings WHERE id=?";
    }

    @Override
    protected Building mapRow(ResultSet rs) throws SQLException {
        Building building = new Building();
        building.setId(rs.getInt("id"));
        building.setName(rs.getString("name"));
        building.setCategory(rs.getString("category"));
        building.setLatitude(rs.getDouble("latitude"));
        building.setLongitude(rs.getDouble("longitude"));
        building.setDescription(rs.getString("description"));
        building.setImageUrl(rs.getString("image_url"));
        try {
            building.setEditedByAdmin(rs.getBoolean("edited_by_admin"));
        } catch (SQLException ignored) {
        }
        return building;
    }

    public List<Building> findAll() {
        List<Building> list = new ArrayList<>();
        String sql = "SELECT * FROM buildings ORDER BY id";
        try (var conn = getConnection();
             var stmt = conn.prepareStatement(sql);
             var rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询所有建筑失败", e);
        }
        return list;
    }

    public List<Building> searchByName(String keyword) {
        List<Building> list = new ArrayList<>();
        String sql = "SELECT * FROM buildings WHERE name LIKE ? ORDER BY id";
        try (var conn = getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, "%" + keyword + "%");
            try (var rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("搜索建筑失败", e);
        }
        return list;
    }

    public List<Building> findByCategory(String category) {
        List<Building> list = new ArrayList<>();
        String sql = "SELECT * FROM buildings WHERE category=? ORDER BY id";
        try (var conn = getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, category);
            try (var rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("按分类查询失败", e);
        }
        return list;
    }
}
