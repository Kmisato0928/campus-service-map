package edu.chd.campusmap.dao;

import edu.chd.campusmap.model.PathNode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class PathNodeDAO extends BaseDAO<PathNode> {

    @Override
    protected String getInsertSql() {
        return "INSERT INTO path_nodes (lat, lon, name, building_id) VALUES (?, ?, ?, ?)";
    }

    @Override
    protected void setInsertParams(PreparedStatement stmt, PathNode node) throws SQLException {
        stmt.setDouble(1, node.getLat());
        stmt.setDouble(2, node.getLon());
        if (node.getName() != null) {
            stmt.setString(3, node.getName());
        } else {
            stmt.setNull(3, java.sql.Types.VARCHAR);
        }
        if (node.getBuildingId() != null) {
            stmt.setInt(4, node.getBuildingId());
        } else {
            stmt.setNull(4, java.sql.Types.INTEGER);
        }
    }

    @Override
    protected String getUpdateSql() {
        return "UPDATE path_nodes SET lat=?, lon=?, name=?, building_id=? WHERE id=?";
    }

    @Override
    protected void setUpdateParams(PreparedStatement stmt, PathNode node) throws SQLException {
        stmt.setDouble(1, node.getLat());
        stmt.setDouble(2, node.getLon());
        if (node.getName() != null) {
            stmt.setString(3, node.getName());
        } else {
            stmt.setNull(3, java.sql.Types.VARCHAR);
        }
        if (node.getBuildingId() != null) {
            stmt.setInt(4, node.getBuildingId());
        } else {
            stmt.setNull(4, java.sql.Types.INTEGER);
        }
        stmt.setInt(5, node.getId());
    }

    @Override
    protected String getDeleteSql() {
        return "DELETE FROM path_nodes WHERE id=?";
    }

    @Override
    protected String getSelectByIdSql() {
        return "SELECT * FROM path_nodes WHERE id=?";
    }

    @Override
    protected PathNode mapRow(ResultSet rs) throws SQLException {
        PathNode node = new PathNode();
        node.setId(rs.getInt("id"));
        node.setLat(rs.getDouble("lat"));
        node.setLon(rs.getDouble("lon"));
        node.setName(rs.getString("name"));
        int bId = rs.getInt("building_id");
        node.setBuildingId(rs.wasNull() ? null : bId);
        return node;
    }

    public List<PathNode> findAll() {
        List<PathNode> list = new ArrayList<>();
        String sql = "SELECT * FROM path_nodes ORDER BY id";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询所有路径节点失败", e);
        }
        return list;
    }

    public PathNode findNearest(double lat, double lon) {
        String sql = "SELECT *, (6371000 * 2 * ASIN(SQRT(" +
                "POWER(SIN(RADIANS(lat - ?) / 2), 2) + " +
                "COS(RADIANS(?)) * COS(RADIANS(lat)) * POWER(SIN(RADIANS(lon - ?) / 2), 2)" +
                "))) AS dist FROM path_nodes ORDER BY dist LIMIT 1";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, lat);
            stmt.setDouble(2, lat);
            stmt.setDouble(3, lon);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("查找最近路径节点失败", e);
        }
        return null;
    }

    public int count() {
        String sql = "SELECT COUNT(*) FROM path_nodes";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            return 0;
        }
        return 0;
    }

    public void deleteAll() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM path_edges");
            stmt.execute("DELETE FROM path_nodes");
        } catch (SQLException e) {
            throw new RuntimeException("清空路径数据失败", e);
        }
    }

    public void batchInsert(List<PathNode> nodes) {
        String sql = "INSERT INTO path_nodes (lat, lon, name, building_id) VALUES (?, ?, ?, ?)";
        Connection conn = getConnection();
        boolean originalAutoCommit = true;
        try {
            originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                for (PathNode node : nodes) {
                    stmt.setDouble(1, node.getLat());
                    stmt.setDouble(2, node.getLon());
                    if (node.getName() != null) {
                        stmt.setString(3, node.getName());
                    } else {
                        stmt.setNull(3, java.sql.Types.VARCHAR);
                    }
                    if (node.getBuildingId() != null) {
                        stmt.setInt(4, node.getBuildingId());
                    } else {
                        stmt.setNull(4, java.sql.Types.INTEGER);
                    }
                    stmt.executeUpdate();
                    try (ResultSet keys = stmt.getGeneratedKeys()) {
                        if (keys.next()) {
                            node.setId(keys.getInt(1));
                        }
                    }
                }
            }
            conn.commit();
        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException ex) { /* ignore */ }
            throw new RuntimeException("批量插入路径节点失败", e);
        } finally {
            try { conn.setAutoCommit(originalAutoCommit); } catch (SQLException e) { /* ignore */ }
        }
    }
}
