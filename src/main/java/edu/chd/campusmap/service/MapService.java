package edu.chd.campusmap.service;

import edu.chd.campusmap.dao.BuildingDAO;
import edu.chd.campusmap.dao.BuildingOverrideDAO;
import edu.chd.campusmap.model.Building;
import edu.chd.campusmap.model.BuildingOverride;

import java.util.List;

public class MapService {
    private final BuildingDAO buildingDAO;
    private final BuildingOverrideDAO overrideDAO;

    public MapService() {
        this.buildingDAO = new BuildingDAO();
        this.overrideDAO = new BuildingOverrideDAO();
    }

    public List<Building> getAllBuildings() {
        return buildingDAO.findAll();
    }

    /**
     * 获取建筑列表，并应用当前用户的个人覆盖（普通用户编辑仅自己可见）
     */
    public List<Building> getAllBuildingsForUser(int userId) {
        List<Building> buildings = buildingDAO.findAll();
        for (Building b : buildings) {
            BuildingOverride override = overrideDAO.findByUserAndBuilding(userId, b.getId());
            if (override != null) {
                applyOverride(b, override);
            }
        }
        return buildings;
    }

    public Building getBuildingById(int id) {
        return buildingDAO.findById(id);
    }

    public Building getBuildingById(int id, int userId) {
        Building b = buildingDAO.findById(id);
        if (b != null && userId > 0) {
            BuildingOverride override = overrideDAO.findByUserAndBuilding(userId, id);
            if (override != null) applyOverride(b, override);
        }
        return b;
    }

    /**
     * 管理员更新建筑：更新 buildings 表 + 清除用户覆盖 + 标记管理员已编辑
     */
    public boolean adminSaveBuilding(Building building) {
        boolean ok = buildingDAO.update(building);
        if (ok) {
            // 管理员编辑后，清除所有用户的个人覆盖，确保所有用户看到统一的管理员版本
            overrideDAO.deleteByBuilding(building.getId());
            markAdminEdited(building.getId());
        }
        return ok;
    }

    private void markAdminEdited(int buildingId) {
        String sql = "UPDATE buildings SET edited_by_admin=TRUE WHERE id=?";
        try (var conn = edu.chd.campusmap.util.DatabaseConnection.getInstance().getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, buildingId);
            stmt.executeUpdate();
        } catch (java.sql.SQLException e) {
            throw new RuntimeException("标记管理员编辑失败", e);
        }
    }

    /**
     * 保存用户的个人建筑覆盖
     */
    public boolean saveUserOverride(BuildingOverride override) {
        return overrideDAO.save(override);
    }

    /**
     * 将覆盖数据合并到建筑对象
     */
    private void applyOverride(Building building, BuildingOverride override) {
        if (override.getName() != null) building.setName(override.getName());
        if (override.getCategory() != null) building.setCategory(override.getCategory());
        building.setLatitude(override.getLatitude());
        building.setLongitude(override.getLongitude());
        if (override.getDescription() != null) building.setDescription(override.getDescription());
    }
}
