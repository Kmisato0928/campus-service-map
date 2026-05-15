-- ============================================================
-- 长安大学校园服务地图系统 — 数据库初始化脚本
-- 用法: mysql -u root -p < init-database.sql
-- 特点: 使用 REPLACE INTO，已存在的记录会被覆盖，
--       自定义的记录（不同 ID）会保留
-- ============================================================

CREATE DATABASE IF NOT EXISTS campus_map
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE campus_map;

-- ======================== 建表 ========================

CREATE TABLE IF NOT EXISTS users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    role VARCHAR(10) DEFAULT 'USER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS buildings (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(20) NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    description TEXT,
    image_url VARCHAR(255),
    edited_by_admin BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS comments (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    building_id INT NOT NULL,
    content TEXT NOT NULL,
    rating INT DEFAULT 5,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (building_id) REFERENCES buildings(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS favorites (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    building_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_favorite (user_id, building_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (building_id) REFERENCES buildings(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS realtime_info (
    id INT PRIMARY KEY AUTO_INCREMENT,
    building_id INT NOT NULL,
    info_type VARCHAR(20) NOT NULL,
    status VARCHAR(10) NOT NULL,
    time_slot VARCHAR(20),
    day_of_week INT,
    FOREIGN KEY (building_id) REFERENCES buildings(id) ON DELETE CASCADE
);

-- ======================== 兼容已有数据库 ========================
-- 为旧表添加 edited_by_admin 列（如果不存在）
ALTER TABLE buildings ADD COLUMN IF NOT EXISTS edited_by_admin BOOLEAN DEFAULT FALSE;

-- 用户个人建筑覆盖（普通用户编辑仅自己可见）
CREATE TABLE IF NOT EXISTS user_building_overrides (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    building_id INT NOT NULL,
    name VARCHAR(100),
    category VARCHAR(20),
    latitude DOUBLE,
    longitude DOUBLE,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_user_building (user_id, building_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (building_id) REFERENCES buildings(id) ON DELETE CASCADE
);

-- 评论点赞
CREATE TABLE IF NOT EXISTS comment_likes (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    comment_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_like (user_id, comment_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (comment_id) REFERENCES comments(id) ON DELETE CASCADE
);

-- ======================== 数据 ========================
-- 使用 REPLACE INTO：相同 ID 覆盖，不同 ID 新增

-- 建筑数据（长安大学渭水校区 24 个建筑）
REPLACE INTO buildings (id, name, category, latitude, longitude, description) VALUES
(1, '鸿远教学楼', 'TEACHING', 34.3750, 108.9100, '渭水校区主教学楼，配备多媒体教室和智慧教室'),
(2, '明远教学楼', 'TEACHING', 34.3730, 108.9080, '承担基础课程教学任务，设有大型阶梯教室'),
(3, '修远教学楼', 'TEACHING', 34.3740, 108.9120, '文科类教学楼，环境安静'),
(4, '图书馆', 'LIBRARY', 34.3710, 108.9110, '校图书馆总馆，藏书丰富，设有自习区'),
(5, '树惠园餐厅', 'CANTEEN', 34.3755, 108.9095, '东区学生餐厅，提供多样化的餐饮选择'),
(6, '滋兰苑餐厅', 'CANTEEN', 34.3720, 108.9125, '西区学生餐厅，以面食和特色窗口著称'),
(7, '小时空餐厅', 'CANTEEN', 34.3745, 108.9130, '教师餐厅，环境优雅，提供自助餐'),
(8, '天问餐厅', 'CANTEEN', 34.3760, 108.9105, '西区新餐厅，品种丰富'),
(9, '朝晖大学生活动中心', 'OTHER', 34.3725, 108.9090, '学生活动举办地，礼堂可容纳千人'),
(10, '长安文化艺术中心', 'OTHER', 34.3715, 108.9130, '艺术展览、演出场地'),
(11, '体育场', 'OTHER', 34.3765, 108.9080, '标准田径场，含足球场和看台'),
(12, '体育馆', 'OTHER', 34.3770, 108.9090, '室内体育馆，设有篮球场、羽毛球场、乒乓球馆'),
(13, '游泳馆', 'OTHER', 34.3760, 108.9070, '室内恒温游泳池'),
(14, '1号学生公寓', 'DORM', 34.3757, 108.9108, '东区学生宿舍'),
(15, '2号学生公寓', 'DORM', 34.3753, 108.9105, '东区学生宿舍'),
(16, '3号学生公寓', 'DORM', 34.3748, 108.9102, '东区学生宿舍'),
(17, '4号学生公寓', 'DORM', 34.3723, 108.9118, '西区学生宿舍'),
(18, '5号学生公寓', 'DORM', 34.3718, 108.9115, '西区学生宿舍'),
(19, '6号学生公寓', 'DORM', 34.3713, 108.9112, '西区学生宿舍'),
(20, '校医院', 'OTHER', 34.3700, 108.9090, '提供基础医疗服务和急诊'),
(21, '行政楼', 'OTHER', 34.3690, 108.9100, '学校行政办公所在地'),
(22, '交通馆', 'TEACHING', 34.3745, 108.9060, '交通运输类专业实验楼'),
(23, '信息工程学院实验楼', 'TEACHING', 34.3735, 108.9070, '计算机与信息类实验教学中心'),
(24, '汽车试验场', 'OTHER', 34.3780, 108.9050, '车辆工程专业试验场地');

-- 用户数据（密码均为 '111111' 的 BCrypt 哈希）
REPLACE INTO users (id, username, password, email, role) VALUES
(1, 'admin', '$2a$10$sS2db1ruPyUhiu8wckRZGuc4wBzJKNSS5PTvm9h/CECIbYdVMZu1C', 'admin@chd.edu.cn', 'ADMIN'),
(2, 'zhangsan', '$2a$10$sS2db1ruPyUhiu8wckRZGuc4wBzJKNSS5PTvm9h/CECIbYdVMZu1C', 'zhangsan@chd.edu.cn', 'USER'),
(3, 'lisi', '$2a$10$sS2db1ruPyUhiu8wckRZGuc4wBzJKNSS5PTvm9h/CECIbYdVMZu1C', 'lisi@chd.edu.cn', 'USER');

-- 评论
REPLACE INTO comments (id, user_id, building_id, content, rating) VALUES
(1, 2, 1, '教室设备很新，智慧屏非常好用', 5),
(2, 2, 5, '树惠园的麻辣烫很好吃，就是高峰期人太多', 4),
(3, 3, 3, '修远楼自习室很安静，适合学习', 5),
(4, 3, 4, '图书馆座位充足，学习氛围好', 5),
(5, 2, 10, '偶尔有演出活动，丰富了课余生活', 4);

-- 收藏
REPLACE INTO favorites (id, user_id, building_id) VALUES
(1, 2, 1),
(2, 2, 4),
(3, 2, 5),
(4, 3, 3),
(5, 3, 4);

-- 实时信息
REPLACE INTO realtime_info (id, building_id, info_type, status, time_slot, day_of_week) VALUES
(1, 1, 'OCCUPANCY', 'MEDIUM', '10:00-12:00', 1),
(2, 1, 'OCCUPANCY', 'HIGH', '14:00-16:00', 1),
(3, 4, 'CROWD', 'LOW', '08:00-10:00', 1),
(4, 4, 'CROWD', 'HIGH', '18:00-21:00', 1),
(5, 5, 'CROWD', 'HIGH', '12:00-13:00', 1),
(6, 5, 'CROWD', 'MEDIUM', '17:30-18:30', 1),
(7, 6, 'CROWD', 'MEDIUM', '12:00-13:00', 1),
(8, 6, 'CROWD', 'HIGH', '18:00-19:00', 1);

SELECT '数据库初始化完成!' AS status;
