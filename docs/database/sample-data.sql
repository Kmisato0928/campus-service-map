USE campus_map;

-- #region agent log
SELECT JSON_OBJECT(
    'sessionId', 'ce5951',
    'runId', 'pre-fix',
    'hypothesisId', 'H2',
    'location', 'docs/database/sample-data.sql:3',
    'message', 'Current database before cleanup',
    'data', JSON_OBJECT('database', DATABASE()),
    'timestamp', UNIX_TIMESTAMP(NOW(3)) * 1000
) AS agent_debug_log;
-- #endregion

-- #region agent log
SELECT JSON_OBJECT(
    'sessionId', 'ce5951',
    'runId', 'pre-fix',
    'hypothesisId', 'H1',
    'location', 'docs/database/sample-data.sql:15',
    'message', 'Buildings table existence before drop',
    'data', JSON_OBJECT(
        'exists',
        EXISTS (
            SELECT 1
            FROM information_schema.tables
            WHERE table_schema = DATABASE() AND table_name = 'buildings'
        )
    ),
    'timestamp', UNIX_TIMESTAMP(NOW(3)) * 1000
) AS agent_debug_log;
-- #endregion

DELETE FROM realtime_info;
DELETE FROM favorites;
DELETE FROM comments;
DELETE FROM buildings;
DELETE FROM users;

-- #region agent log
SELECT JSON_OBJECT(
    'sessionId', 'ce5951',
    'runId', 'pre-fix',
    'hypothesisId', 'H1',
    'location', 'docs/database/sample-data.sql:39',
    'message', 'Buildings table existence after cleanup before insert',
    'data', JSON_OBJECT(
        'exists',
        EXISTS (
            SELECT 1
            FROM information_schema.tables
            WHERE table_schema = DATABASE() AND table_name = 'buildings'
        )
    ),
    'timestamp', UNIX_TIMESTAMP(NOW(3)) * 1000
) AS agent_debug_log;
-- #endregion

-- ========== 建筑数据（长安大学渭水校区 20+ 建筑） ==========
INSERT INTO buildings (name, category, latitude, longitude, description) VALUES
('鸿远教学楼', 'TEACHING', 34.3750, 108.9100, '渭水校区主教学楼，配备多媒体教室和智慧教室'),
('明远教学楼', 'TEACHING', 34.3730, 108.9080, '承担基础课程教学任务，设有大型阶梯教室'),
('修远教学楼', 'TEACHING', 34.3740, 108.9120, '文科类教学楼，环境安静'),
('图书馆', 'LIBRARY', 34.3710, 108.9110, '校图书馆总馆，藏书丰富，设有自习区'),
('树惠园餐厅', 'CANTEEN', 34.3755, 108.9095, '东区学生餐厅，提供多样化的餐饮选择'),
('滋兰苑餐厅', 'CANTEEN', 34.3720, 108.9125, '西区学生餐厅，以面食和特色窗口著称'),
('小时空餐厅', 'CANTEEN', 34.3745, 108.9130, '教师餐厅，环境优雅，提供自助餐'),
('天问餐厅', 'CANTEEN', 34.3760, 108.9105, '西区新餐厅，品种丰富'),
('朝晖大学生活动中心', 'OTHER', 34.3725, 108.9090, '学生活动举办地，礼堂可容纳千人'),
('长安文化艺术中心', 'OTHER', 34.3715, 108.9130, '艺术展览、演出场地'),
('体育场', 'OTHER', 34.3765, 108.9080, '标准田径场，含足球场和看台'),
('体育馆', 'OTHER', 34.3770, 108.9090, '室内体育馆，设有篮球场、羽毛球场、乒乓球馆'),
('游泳馆', 'OTHER', 34.3760, 108.9070, '室内恒温游泳池'),
('1号学生公寓', 'DORM', 34.3757, 108.9108, '东区学生宿舍'),
('2号学生公寓', 'DORM', 34.3753, 108.9105, '东区学生宿舍'),
('3号学生公寓', 'DORM', 34.3748, 108.9102, '东区学生宿舍'),
('4号学生公寓', 'DORM', 34.3723, 108.9118, '西区学生宿舍'),
('5号学生公寓', 'DORM', 34.3718, 108.9115, '西区学生宿舍'),
('6号学生公寓', 'DORM', 34.3713, 108.9112, '西区学生宿舍'),
('校医院', 'OTHER', 34.3700, 108.9090, '提供基础医疗服务和急诊'),
('行政楼', 'OTHER', 34.3690, 108.9100, '学校行政办公所在地'),
('交通馆', 'TEACHING', 34.3745, 108.9060, '交通运输类专业实验楼'),
('信息工程学院实验楼', 'TEACHING', 34.3735, 108.9070, '计算机与信息类实验教学中心'),
('汽车试验场', 'OTHER', 34.3780, 108.9050, '车辆工程专业试验场地');

-- ========== 用户数据 ==========
INSERT INTO users (username, password, email, role) VALUES
('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'admin@chd.edu.cn', 'ADMIN'),
('zhangsan', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'zhangsan@chd.edu.cn', 'USER'),
('lisi', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'lisi@chd.edu.cn', 'USER');

-- ========== 评论数据 ==========
INSERT INTO comments (user_id, building_id, content, rating) VALUES
(2, 1, '教室设备很新，智慧屏非常好用', 5),
(2, 5, '树惠园的麻辣烫很好吃，就是高峰期人太多', 4),
(3, 3, '修远楼自习室很安静，适合学习', 5),
(3, 4, '图书馆座位充足，学习氛围好', 5),
(2, 10, '偶尔有演出活动，丰富了课余生活', 4);

-- ========== 收藏数据 ==========
INSERT INTO favorites (user_id, building_id) VALUES
(2, 1),
(2, 4),
(2, 5),
(3, 3),
(3, 4);

-- ========== 实时信息 ==========
INSERT INTO realtime_info (building_id, info_type, status, time_slot, day_of_week) VALUES
(1, 'OCCUPANCY', 'MEDIUM', '10:00-12:00', 1),
(1, 'OCCUPANCY', 'HIGH', '14:00-16:00', 1),
(4, 'CROWD', 'LOW', '08:00-10:00', 1),
(4, 'CROWD', 'HIGH', '18:00-21:00', 1),
(5, 'CROWD', 'HIGH', '12:00-13:00', 1),
(5, 'CROWD', 'MEDIUM', '17:30-18:30', 1),
(6, 'CROWD', 'MEDIUM', '12:00-13:00', 1),
(6, 'CROWD', 'HIGH', '18:00-19:00', 1);