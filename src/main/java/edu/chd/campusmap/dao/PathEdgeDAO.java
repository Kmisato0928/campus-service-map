package edu.chd.campusmap.dao;

import edu.chd.campusmap.model.PathEdge;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class PathEdgeDAO extends BaseDAO<PathEdge> {

    @Override
    protected String getInsertSql() {
        return "INSERT INTO path_edges (start_node_id, end_node_id, distance_meters, road_name) VALUES (?, ?, ?, ?)";
    }

    @Override
    protected void setInsertParams(PreparedStatement stmt, PathEdge edge) throws SQLException {
        stmt.setInt(1, edge.getStartNodeId());
        stmt.setInt(2, edge.getEndNodeId());
        stmt.setDouble(3, edge.getDistanceMeters());
        if (edge.getRoadName() != null) {
            stmt.setString(4, edge.getRoadName());
        } else {
            stmt.setNull(4, java.sql.Types.VARCHAR);
        }
    }

    @Override
    protected String getUpdateSql() {
        return "UPDATE path_edges SET start_node_id=?, end_node_id=?, distance_meters=?, road_name=? WHERE id=?";
    }

    @Override
    protected void setUpdateParams(PreparedStatement stmt, PathEdge edge) throws SQLException {
        stmt.setInt(1, edge.getStartNodeId());
        stmt.setInt(2, edge.getEndNodeId());
        stmt.setDouble(3, edge.getDistanceMeters());
        if (edge.getRoadName() != null) {
            stmt.setString(4, edge.getRoadName());
        } else {
            stmt.setNull(4, java.sql.Types.VARCHAR);
        }
        stmt.setInt(5, edge.getId());
    }

    @Override
    protected String getDeleteSql() {
        return "DELETE FROM path_edges WHERE id=?";
    }

    @Override
    protected String getSelectByIdSql() {
        return "SELECT * FROM path_edges WHERE id=?";
    }

    @Override
    protected PathEdge mapRow(ResultSet rs) throws SQLException {
        PathEdge edge = new PathEdge();
        edge.setId(rs.getInt("id"));
        edge.setStartNodeId(rs.getInt("start_node_id"));
        edge.setEndNodeId(rs.getInt("end_node_id"));
        edge.setDistanceMeters(rs.getDouble("distance_meters"));
        edge.setRoadName(rs.getString("road_name"));
        return edge;
    }

    public List<PathEdge> findAll() {
        List<PathEdge> list = new ArrayList<>();
        String sql = "SELECT * FROM path_edges ORDER BY id";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询所有路径边失败", e);
        }
        return list;
    }

    public void deleteAll() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM path_edges");
        } catch (SQLException e) {
            throw new RuntimeException("清空路径边数据失败", e);
        }
    }

    public int count() {
        String sql = "SELECT COUNT(*) FROM path_edges";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            return 0;
        }
        return 0;
    }

    public void batchInsert(List<PathEdge> edges) {
        String sql = "INSERT INTO path_edges (start_node_id, end_node_id, distance_meters, road_name) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (PathEdge edge : edges) {
                stmt.setInt(1, edge.getStartNodeId());
                stmt.setInt(2, edge.getEndNodeId());
                stmt.setDouble(3, edge.getDistanceMeters());
                if (edge.getRoadName() != null) {
                    stmt.setString(4, edge.getRoadName());
                } else {
                    stmt.setNull(4, java.sql.Types.VARCHAR);
                }
                stmt.addBatch();
            }
            stmt.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException("批量插入路径边失败", e);
        }
    }
}
