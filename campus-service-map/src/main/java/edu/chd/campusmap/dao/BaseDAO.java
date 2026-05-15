package edu.chd.campusmap.dao;

import edu.chd.campusmap.util.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class BaseDAO<T> {

    protected Connection getConnection() {
        return DatabaseConnection.getInstance().getConnection();
    }

    public final boolean save(T entity) {
        String sql = getInsertSql();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            setInsertParams(stmt, entity);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("保存失败", e);
        }
    }

    public final boolean update(T entity) {
        String sql = getUpdateSql();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            setUpdateParams(stmt, entity);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("更新失败", e);
        }
    }

    public final boolean delete(int id) {
        String sql = getDeleteSql();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("删除失败", e);
        }
    }

    public final T findById(int id) {
        String sql = getSelectByIdSql();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询失败", e);
        }
        return null;
    }

    protected abstract String getInsertSql();
    protected abstract void setInsertParams(PreparedStatement stmt, T entity) throws SQLException;

    protected abstract String getUpdateSql();
    protected abstract void setUpdateParams(PreparedStatement stmt, T entity) throws SQLException;

    protected abstract String getDeleteSql();
    protected abstract String getSelectByIdSql();

    protected abstract T mapRow(ResultSet rs) throws SQLException;
}
