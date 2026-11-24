-- =============================================
-- 火车票智能客服管理系统 初始化数据
-- =============================================

-- 初始化管理员账号 (密码: admin123)
INSERT IGNORE INTO `user` (`id`, `username`, `password`, `nickname`, `role`, `status`) VALUES
(1, 'admin', MD5('admin123'), '系统管理员', 1, 1);

-- 初始化测试用户 (密码: user123)
INSERT IGNORE INTO `user` (`id`, `username`, `password`, `nickname`, `phone`, `id_card`, `role`, `status`) VALUES
(2, 'user', MD5('user123'), '测试用户', '13800138000', '110101199001011234', 0, 1);

-- 初始化站点数据
INSERT IGNORE INTO `station` (`id`, `name`, `code`, `pinyin`, `city`, `province`) VALUES
(1, '北京南', 'VNP', 'beijingnan', '北京', '北京'),
(2, '北京西', 'BXP', 'beijingxi', '北京', '北京'),
(3, '北京', 'BJP', 'beijing', '北京', '北京'),
(4, '上海虹桥', 'AOH', 'shanghaihongqiao', '上海', '上海'),
(5, '上海', 'SHH', 'shanghai', '上海', '上海'),
(6, '广州南', 'IZQ', 'guangzhounan', '广州', '广东'),
(7, '广州', 'GZQ', 'guangzhou', '广州', '广东'),
(8, '深圳北', 'IOQ', 'shenzhenbei', '深圳', '广东'),
(9, '深圳', 'SZQ', 'shenzhen', '深圳', '广东'),
(10, '杭州东', 'HGH', 'hangzhoudong', '杭州', '浙江'),
(11, '杭州', 'HZH', 'hangzhou', '杭州', '浙江'),
(12, '南京南', 'NKH', 'nanjingnan', '南京', '江苏'),
(13, '南京', 'NJG', 'nanjing', '南京', '江苏'),
(14, '武汉', 'WHN', 'wuhan', '武汉', '湖北'),
(15, '成都东', 'ICW', 'chengdudong', '成都', '四川'),
(16, '成都', 'CDW', 'chengdu', '成都', '四川'),
(17, '西安北', 'EAY', 'xianbei', '西安', '陕西'),
(18, '西安', 'XAG', 'xian', '西安', '陕西'),
(19, '郑州东', 'ZAF', 'zhengzhoudong', '郑州', '河南'),
(20, '郑州', 'ZZF', 'zhengzhou', '郑州', '河南'),
(21, '济南西', 'JGK', 'jinanxi', '济南', '山东'),
(22, '济南', 'JNK', 'jinan', '济南', '山东'),
(23, '合肥南', 'ENH', 'hefeinan', '合肥', '安徽'),
(24, '福州南', 'FYS', 'fuzhounan', '福州', '福建'),
(25, '厦门北', 'XBS', 'xiamenbei', '厦门', '福建'),
(26, '长沙南', 'CSQ', 'changshanana', '长沙', '湖南'),
(27, '南昌西', 'NXG', 'nanchangxi', '南昌', '江西'),
(28, '昆明南', 'KMM', 'kunmingnan', '昆明', '云南'),
(29, '贵阳北', 'GYW', 'guiyangbei', '贵阳', '贵州'),
(30, '重庆西', 'CXW', 'chongqingxi', '重庆', '重庆');

-- 初始化车次数据 (固定ID，方便订单关联)
INSERT IGNORE INTO `train` (`id`, `train_number`, `train_type`, `from_station_id`, `to_station_id`, `depart_time`, `arrive_time`, `duration`, `business_seats`, `business_price`, `first_seats`, `first_price`, `second_seats`, `second_price`, `running_days`) VALUES
(1, 'G1', 0, 1, 4, '07:00:00', '11:28:00', 268, 20, 1748.00, 50, 933.00, 800, 553.00, '1234567'),
(2, 'G3', 0, 1, 4, '08:00:00', '12:38:00', 278, 20, 1748.00, 50, 933.00, 800, 553.00, '1234567'),
(3, 'G5', 0, 1, 4, '09:00:00', '13:28:00', 268, 20, 1748.00, 50, 933.00, 800, 553.00, '1234567'),
(4, 'G7', 0, 1, 4, '10:00:00', '14:28:00', 268, 20, 1748.00, 50, 933.00, 800, 553.00, '1234567'),
(5, 'G13', 0, 1, 4, '11:00:00', '15:38:00', 278, 20, 1748.00, 50, 933.00, 800, 553.00, '1234567'),
(6, 'G17', 0, 1, 4, '12:00:00', '16:28:00', 268, 20, 1748.00, 50, 933.00, 800, 553.00, '1234567'),
(7, 'G21', 0, 1, 4, '13:00:00', '17:28:00', 268, 20, 1748.00, 50, 933.00, 800, 553.00, '1234567'),
(8, 'G25', 0, 1, 4, '14:00:00', '18:28:00', 268, 20, 1748.00, 50, 933.00, 800, 553.00, '1234567'),
(9, 'D1', 1, 1, 10, '08:30:00', '12:45:00', 255, 10, 614.00, 30, 314.00, 600, 184.00, '1234567'),
(10, 'D3', 1, 1, 10, '10:30:00', '14:45:00', 255, 10, 614.00, 30, 314.00, 600, 184.00, '1234567'),
(11, 'G279', 0, 2, 7, '07:30:00', '16:45:00', 555, 15, 2928.00, 40, 1528.00, 500, 918.00, '1234567'),
(12, 'G79', 0, 2, 7, '08:00:00', '17:00:00', 540, 15, 2928.00, 40, 1528.00, 500, 918.00, '1234567'),
(13, 'G6021', 0, 5, 6, '06:30:00', '14:00:00', 450, 10, 1748.00, 30, 933.00, 500, 553.00, '1234567'),
(14, 'G85', 0, 5, 6, '07:00:00', '14:30:00', 450, 10, 1748.00, 30, 933.00, 500, 553.00, '1234567'),
(15, 'G1601', 0, 10, 4, '08:00:00', '12:30:00', 270, 15, 1748.00, 40, 933.00, 600, 553.00, '1234567'),
(16, 'G7391', 0, 10, 13, '09:00:00', '11:15:00', 135, 10, 848.00, 20, 448.00, 400, 268.00, '1234567'),
(17, 'Z281', 3, 3, 11, '19:33:00', '07:15:00', 702, 0, 0.00, 0, 0.00, 0, 0.00, '1234567'),
(18, 'Z35', 3, 3, 15, '11:00:00', '06:30:00', 1170, 0, 0.00, 0, 0.00, 0, 0.00, '1234567'),
(19, 'K105', 2, 3, 11, '11:45:00', '21:00:00', 1755, 0, 0.00, 0, 0.00, 0, 0.00, '1234567'),
(20, 'G57', 0, 14, 1, '07:00:00', '14:30:00', 450, 15, 2398.00, 40, 1288.00, 600, 788.00, '1234567'),
(21, 'G307', 0, 1, 16, '08:00:00', '13:30:00', 330, 15, 2148.00, 40, 1138.00, 600, 698.00, '1234567'),
(22, 'G197', 0, 16, 21, '09:00:00', '11:00:00', 120, 10, 748.00, 20, 398.00, 400, 238.00, '1234567'),
(23, 'G6101', 0, 7, 9, '06:45:00', '07:45:00', 60, 10, 220.00, 30, 110.00, 400, 74.50, '1234567'),
(24, 'G6103', 0, 7, 8, '07:30:00', '08:20:00', 50, 10, 220.00, 30, 110.00, 400, 74.50, '1234567'),
(25, 'G6105', 0, 7, 9, '08:15:00', '09:15:00', 60, 10, 220.00, 30, 110.00, 400, 74.50, '1234567'),
(26, 'G6107', 0, 7, 8, '09:00:00', '09:55:00', 55, 10, 220.00, 30, 110.00, 400, 74.50, '1234567'),
(27, 'G6109', 0, 7, 9, '10:30:00', '11:30:00', 60, 10, 220.00, 30, 110.00, 400, 74.50, '1234567'),
(28, 'G7301', 0, 4, 10, '06:30:00', '07:30:00', 60, 10, 220.00, 30, 117.00, 400, 73.00, '1234567'),
(29, 'G7303', 0, 4, 11, '07:20:00', '08:25:00', 65, 10, 220.00, 30, 117.00, 400, 73.00, '1234567'),
(30, 'G7305', 0, 4, 10, '08:10:00', '09:10:00', 60, 10, 220.00, 30, 117.00, 400, 73.00, '1234567'),
(31, 'G7307', 0, 4, 11, '09:15:00', '10:20:00', 65, 10, 220.00, 30, 117.00, 400, 73.00, '1234567'),
(32, 'G7309', 0, 4, 10, '10:45:00', '11:45:00', 60, 10, 220.00, 30, 117.00, 400, 73.00, '1234567'),
(33, 'G8601', 0, 15, 30, '07:00:00', '08:40:00', 100, 10, 460.00, 30, 246.00, 400, 154.00, '1234567'),
(34, 'G8603', 0, 16, 30, '08:20:00', '10:05:00', 105, 10, 460.00, 30, 246.00, 400, 154.00, '1234567'),
(35, 'G8605', 0, 15, 30, '09:30:00', '11:10:00', 100, 10, 460.00, 30, 246.00, 400, 154.00, '1234567'),
(36, 'G8607', 0, 16, 30, '11:00:00', '12:45:00', 105, 10, 460.00, 30, 246.00, 400, 154.00, '1234567'),
(37, 'G8609', 0, 15, 30, '13:30:00', '15:10:00', 100, 10, 460.00, 30, 246.00, 400, 154.00, '1234567'),
(38, 'G2001', 0, 17, 19, '06:50:00', '08:50:00', 120, 10, 680.00, 30, 359.00, 400, 229.00, '1234567'),
(39, 'G2003', 0, 18, 19, '07:40:00', '09:45:00', 125, 10, 680.00, 30, 359.00, 400, 229.00, '1234567'),
(40, 'G2005', 0, 17, 20, '08:30:00', '10:40:00', 130, 10, 680.00, 30, 359.00, 400, 229.00, '1234567'),
(41, 'G2007', 0, 18, 20, '10:15:00', '12:20:00', 125, 10, 680.00, 30, 359.00, 400, 229.00, '1234567'),
(42, 'G2009', 0, 17, 19, '12:00:00', '14:00:00', 120, 10, 680.00, 30, 359.00, 400, 229.00, '1234567'),
(43, 'G1101', 0, 14, 26, '07:15:00', '08:45:00', 90, 10, 330.00, 30, 175.00, 400, 110.00, '1234567'),
(44, 'G1103', 0, 14, 26, '08:30:00', '10:00:00', 90, 10, 330.00, 30, 175.00, 400, 110.00, '1234567'),
(45, 'G1105', 0, 14, 26, '10:00:00', '11:35:00', 95, 10, 330.00, 30, 175.00, 400, 110.00, '1234567'),
(46, 'G1107', 0, 14, 26, '12:20:00', '13:55:00', 95, 10, 330.00, 30, 175.00, 400, 110.00, '1234567'),
(47, 'G7601', 0, 13, 11, '07:10:00', '09:00:00', 110, 10, 380.00, 30, 200.00, 400, 120.00, '1234567'),
(48, 'G7603', 0, 12, 10, '08:25:00', '10:10:00', 105, 10, 380.00, 30, 200.00, 400, 120.00, '1234567'),
(49, 'G7605', 0, 13, 10, '10:30:00', '12:20:00', 110, 10, 380.00, 30, 200.00, 400, 120.00, '1234567'),
(50, 'G67', 0, 1, 7, '08:00:00', '16:00:00', 480, 15, 2580.00, 40, 1380.00, 500, 862.00, '1234567'),
(51, 'G69', 0, 1, 7, '10:00:00', '18:00:00', 480, 15, 2580.00, 40, 1380.00, 500, 862.00, '1234567'),
(52, 'G19', 0, 1, 10, '09:00:00', '14:00:00', 300, 15, 980.00, 40, 530.00, 600, 320.00, '1234567'),
(53, 'G35', 0, 1, 11, '11:00:00', '16:10:00', 310, 15, 980.00, 40, 530.00, 600, 320.00, '1234567'),
(54, 'G7101', 0, 4, 12, '06:40:00', '08:20:00', 100, 10, 340.00, 30, 180.00, 400, 110.00, '1234567'),
(55, 'G7103', 0, 4, 13, '07:30:00', '09:15:00', 105, 10, 340.00, 30, 180.00, 400, 110.00, '1234567'),
(56, 'G7105', 0, 4, 12, '08:45:00', '10:25:00', 100, 10, 340.00, 30, 180.00, 400, 110.00, '1234567'),
(57, 'G7107', 0, 4, 13, '10:00:00', '11:40:00', 100, 10, 340.00, 30, 180.00, 400, 110.00, '1234567');

-- 测试用户乘客信息
INSERT IGNORE INTO `passenger` (`id`, `user_id`, `name`, `id_card`, `phone`, `type`) VALUES
(1, 2, '张三', '110101199001011234', '13800138000', 0);

-- 初始化测试订单（user_id=2 测试用户）
INSERT IGNORE INTO `order` (`order_number`, `user_id`, `train_id`, `train_number`, `from_station`, `to_station`, `depart_time`, `arrive_time`, `seat_type`, `seat_number`, `passenger_id`, `passenger_name`, `passenger_id_card`, `total_amount`, `status`, `pay_time`) VALUES
('TK20240101001', 2, 1, 'G1', '北京南', '上海虹桥', '2024-07-15 07:00:00', '2024-07-15 11:28:00', 2, '05A', 1, '张三', '110101199001011234', 553.00, 2, '2024-07-10 10:30:00'),
('TK20240101002', 2, 4, 'G7', '北京南', '上海虹桥', '2024-07-20 10:00:00', '2024-07-20 14:28:00', 1, '03C', 1, '张三', '110101199001011234', 933.00, 1, '2024-07-15 14:20:00'),
('TK20240101003', 2, 11, 'G279', '北京西', '广州南', '2024-07-25 07:30:00', '2024-07-25 16:45:00', 2, '08D', 1, '张三', '110101199001011234', 918.00, 0, NULL);

-- 管理员订单记录
INSERT IGNORE INTO `order` (`order_number`, `user_id`, `train_id`, `train_number`, `from_station`, `to_station`, `depart_time`, `arrive_time`, `seat_type`, `seat_number`, `passenger_id`, `passenger_name`, `passenger_id_card`, `total_amount`, `status`, `pay_time`, `create_time`) VALUES
('TK20240615001', 1, 1, 'G1', '北京南', '上海虹桥', '2026-08-20 07:00:00', '2026-08-20 11:28:00', 2, '05A', 1, '系统管理员', '110101199001011234', 553.00, 1, '2026-08-01 10:30:00', '2026-08-01 10:25:00'),
('TK20240615002', 1, 3, 'G5', '北京南', '上海虹桥', '2026-08-22 09:00:00', '2026-08-22 13:28:00', 1, '03C', 1, '系统管理员', '110101199001011234', 933.00, 2, '2026-08-05 14:20:00', '2026-08-05 14:15:00'),
('TK20240615003', 1, 23, 'G6101', '广州', '深圳', '2026-07-28 06:45:00', '2026-07-28 07:45:00', 2, '08D', 1, '系统管理员', '110101199001011234', 74.50, 2, '2026-07-20 09:00:00', '2026-07-20 08:55:00'),
('TK20240615004', 1, 28, 'G7301', '上海虹桥', '杭州东', '2026-07-15 06:30:00', '2026-07-15 07:30:00', 2, '02A', 1, '系统管理员', '110101199001011234', 73.00, 2, '2026-07-10 11:00:00', '2026-07-10 10:55:00'),
('TK20240615005', 1, 33, 'G8601', '成都东', '重庆西', '2026-06-30 07:00:00', '2026-06-30 08:40:00', 1, '01B', 1, '系统管理员', '110101199001011234', 246.00, 3, '2026-06-20 15:00:00', '2026-06-20 14:55:00'),
('TK20240615006', 1, 38, 'G2001', '西安北', '郑州东', '2026-08-25 06:50:00', '2026-08-25 08:50:00', 2, '06C', 1, '系统管理员', '110101199001011234', 229.00, 0, NULL, '2026-08-01 16:00:00'),
('TK20240615007', 1, 43, 'G1101', '武汉', '长沙南', '2026-05-12 07:15:00', '2026-05-12 08:45:00', 2, '04D', 1, '系统管理员', '110101199001011234', 110.00, 2, '2026-05-01 10:00:00', '2026-05-01 09:55:00'),
('TK20240615008', 1, 50, 'G67', '北京南', '广州', '2026-04-20 08:00:00', '2026-04-20 16:00:00', 0, '01A', 1, '系统管理员', '110101199001011234', 2580.00, 2, '2026-04-10 13:00:00', '2026-04-10 12:55:00');

-- Additional demo routes (2026-11-19)
-- 演示车次、时刻和价格仅用于本地测试，不代表真实铁路售票数据。
-- 按站名匹配现有站点；车次号唯一，重复初始化不会覆盖已有车次或订单。
INSERT INTO `train` (`train_number`, `train_type`, `from_station_id`, `to_station_id`, `depart_time`, `arrive_time`, `duration`, `business_seats`, `business_price`, `first_seats`, `first_price`, `second_seats`, `second_price`, `running_days`, `status`)
SELECT 'G9001', 0, s.id, d.id, '08:00:00', '12:40:00', 280, 12, 1659.00, 40, 884.80, 300, 553.00, '1234567', 1
FROM station s JOIN station d ON d.name = '上海虹桥' AND d.deleted = 0 AND d.status = 1
WHERE s.name = '北京南' AND s.deleted = 0 AND s.status = 1
AND NOT EXISTS (SELECT 1 FROM train WHERE train_number = 'G9001');
INSERT INTO `train` (`train_number`, `train_type`, `from_station_id`, `to_station_id`, `depart_time`, `arrive_time`, `duration`, `business_seats`, `business_price`, `first_seats`, `first_price`, `second_seats`, `second_price`, `running_days`, `status`)
SELECT 'G9002', 0, s.id, d.id, '14:00:00', '18:40:00', 280, 12, 1659.00, 40, 884.80, 300, 553.00, '1234567', 1
FROM station s JOIN station d ON d.name = '北京南' AND d.deleted = 0 AND d.status = 1
WHERE s.name = '上海虹桥' AND s.deleted = 0 AND s.status = 1
AND NOT EXISTS (SELECT 1 FROM train WHERE train_number = 'G9002');
INSERT INTO `train` (`train_number`, `train_type`, `from_station_id`, `to_station_id`, `depart_time`, `arrive_time`, `duration`, `business_seats`, `business_price`, `first_seats`, `first_price`, `second_seats`, `second_price`, `running_days`, `status`)
SELECT 'G9003', 0, s.id, d.id, '08:00:00', '16:30:00', 510, 12, 2586.00, 40, 1379.20, 300, 862.00, '1234567', 1
FROM station s JOIN station d ON d.name = '广州南' AND d.deleted = 0 AND d.status = 1
WHERE s.name = '北京西' AND s.deleted = 0 AND s.status = 1
AND NOT EXISTS (SELECT 1 FROM train WHERE train_number = 'G9003');
INSERT INTO `train` (`train_number`, `train_type`, `from_station_id`, `to_station_id`, `depart_time`, `arrive_time`, `duration`, `business_seats`, `business_price`, `first_seats`, `first_price`, `second_seats`, `second_price`, `running_days`, `status`)
SELECT 'G9004', 0, s.id, d.id, '14:00:00', '22:30:00', 510, 12, 2586.00, 40, 1379.20, 300, 862.00, '1234567', 1
FROM station s JOIN station d ON d.name = '北京西' AND d.deleted = 0 AND d.status = 1
WHERE s.name = '广州南' AND s.deleted = 0 AND s.status = 1
AND NOT EXISTS (SELECT 1 FROM train WHERE train_number = 'G9004');
INSERT INTO `train` (`train_number`, `train_type`, `from_station_id`, `to_station_id`, `depart_time`, `arrive_time`, `duration`, `business_seats`, `business_price`, `first_seats`, `first_price`, `second_seats`, `second_price`, `running_days`, `status`)
SELECT 'G9005', 0, s.id, d.id, '08:00:00', '08:36:00', 36, 12, 223.50, 40, 119.20, 300, 74.50, '1234567', 1
FROM station s JOIN station d ON d.name = '深圳北' AND d.deleted = 0 AND d.status = 1
WHERE s.name = '广州南' AND s.deleted = 0 AND s.status = 1
AND NOT EXISTS (SELECT 1 FROM train WHERE train_number = 'G9005');
INSERT INTO `train` (`train_number`, `train_type`, `from_station_id`, `to_station_id`, `depart_time`, `arrive_time`, `duration`, `business_seats`, `business_price`, `first_seats`, `first_price`, `second_seats`, `second_price`, `running_days`, `status`)
SELECT 'G9006', 0, s.id, d.id, '14:00:00', '14:36:00', 36, 12, 223.50, 40, 119.20, 300, 74.50, '1234567', 1
FROM station s JOIN station d ON d.name = '广州南' AND d.deleted = 0 AND d.status = 1
WHERE s.name = '深圳北' AND s.deleted = 0 AND s.status = 1
AND NOT EXISTS (SELECT 1 FROM train WHERE train_number = 'G9006');
INSERT INTO `train` (`train_number`, `train_type`, `from_station_id`, `to_station_id`, `depart_time`, `arrive_time`, `duration`, `business_seats`, `business_price`, `first_seats`, `first_price`, `second_seats`, `second_price`, `running_days`, `status`)
SELECT 'G9007', 0, s.id, d.id, '08:00:00', '09:00:00', 60, 12, 219.00, 40, 116.80, 300, 73.00, '1234567', 1
FROM station s JOIN station d ON d.name = '杭州东' AND d.deleted = 0 AND d.status = 1
WHERE s.name = '上海虹桥' AND s.deleted = 0 AND s.status = 1
AND NOT EXISTS (SELECT 1 FROM train WHERE train_number = 'G9007');
INSERT INTO `train` (`train_number`, `train_type`, `from_station_id`, `to_station_id`, `depart_time`, `arrive_time`, `duration`, `business_seats`, `business_price`, `first_seats`, `first_price`, `second_seats`, `second_price`, `running_days`, `status`)
SELECT 'G9008', 0, s.id, d.id, '14:00:00', '15:00:00', 60, 12, 219.00, 40, 116.80, 300, 73.00, '1234567', 1
FROM station s JOIN station d ON d.name = '上海虹桥' AND d.deleted = 0 AND d.status = 1
WHERE s.name = '杭州东' AND s.deleted = 0 AND s.status = 1
AND NOT EXISTS (SELECT 1 FROM train WHERE train_number = 'G9008');
INSERT INTO `train` (`train_number`, `train_type`, `from_station_id`, `to_station_id`, `depart_time`, `arrive_time`, `duration`, `business_seats`, `business_price`, `first_seats`, `first_price`, `second_seats`, `second_price`, `running_days`, `status`)
SELECT 'G9009', 0, s.id, d.id, '08:00:00', '09:30:00', 90, 12, 403.50, 40, 215.20, 300, 134.50, '1234567', 1
FROM station s JOIN station d ON d.name = '南京南' AND d.deleted = 0 AND d.status = 1
WHERE s.name = '上海虹桥' AND s.deleted = 0 AND s.status = 1
AND NOT EXISTS (SELECT 1 FROM train WHERE train_number = 'G9009');
INSERT INTO `train` (`train_number`, `train_type`, `from_station_id`, `to_station_id`, `depart_time`, `arrive_time`, `duration`, `business_seats`, `business_price`, `first_seats`, `first_price`, `second_seats`, `second_price`, `running_days`, `status`)
SELECT 'G9010', 0, s.id, d.id, '14:00:00', '15:30:00', 90, 12, 403.50, 40, 215.20, 300, 134.50, '1234567', 1
FROM station s JOIN station d ON d.name = '上海虹桥' AND d.deleted = 0 AND d.status = 1
WHERE s.name = '南京南' AND s.deleted = 0 AND s.status = 1
AND NOT EXISTS (SELECT 1 FROM train WHERE train_number = 'G9010');
INSERT INTO `train` (`train_number`, `train_type`, `from_station_id`, `to_station_id`, `depart_time`, `arrive_time`, `duration`, `business_seats`, `business_price`, `first_seats`, `first_price`, `second_seats`, `second_price`, `running_days`, `status`)
SELECT 'G9011', 0, s.id, d.id, '08:00:00', '09:30:00', 90, 12, 352.50, 40, 188.00, 300, 117.50, '1234567', 1
FROM station s JOIN station d ON d.name = '杭州东' AND d.deleted = 0 AND d.status = 1
WHERE s.name = '南京南' AND s.deleted = 0 AND s.status = 1
AND NOT EXISTS (SELECT 1 FROM train WHERE train_number = 'G9011');
INSERT INTO `train` (`train_number`, `train_type`, `from_station_id`, `to_station_id`, `depart_time`, `arrive_time`, `duration`, `business_seats`, `business_price`, `first_seats`, `first_price`, `second_seats`, `second_price`, `running_days`, `status`)
SELECT 'G9012', 0, s.id, d.id, '14:00:00', '15:30:00', 90, 12, 352.50, 40, 188.00, 300, 117.50, '1234567', 1
FROM station s JOIN station d ON d.name = '南京南' AND d.deleted = 0 AND d.status = 1
WHERE s.name = '杭州东' AND s.deleted = 0 AND s.status = 1
AND NOT EXISTS (SELECT 1 FROM train WHERE train_number = 'G9012');
INSERT INTO `train` (`train_number`, `train_type`, `from_station_id`, `to_station_id`, `depart_time`, `arrive_time`, `duration`, `business_seats`, `business_price`, `first_seats`, `first_price`, `second_seats`, `second_price`, `running_days`, `status`)
SELECT 'G9013', 0, s.id, d.id, '08:00:00', '09:50:00', 110, 12, 583.50, 40, 311.20, 300, 194.50, '1234567', 1
FROM station s JOIN station d ON d.name = '济南西' AND d.deleted = 0 AND d.status = 1
WHERE s.name = '北京南' AND s.deleted = 0 AND s.status = 1
AND NOT EXISTS (SELECT 1 FROM train WHERE train_number = 'G9013');
INSERT INTO `train` (`train_number`, `train_type`, `from_station_id`, `to_station_id`, `depart_time`, `arrive_time`, `duration`, `business_seats`, `business_price`, `first_seats`, `first_price`, `second_seats`, `second_price`, `running_days`, `status`)
SELECT 'G9014', 0, s.id, d.id, '14:00:00', '15:50:00', 110, 12, 583.50, 40, 311.20, 300, 194.50, '1234567', 1
FROM station s JOIN station d ON d.name = '北京南' AND d.deleted = 0 AND d.status = 1
WHERE s.name = '济南西' AND s.deleted = 0 AND s.status = 1
AND NOT EXISTS (SELECT 1 FROM train WHERE train_number = 'G9014');
INSERT INTO `train` (`train_number`, `train_type`, `from_station_id`, `to_station_id`, `depart_time`, `arrive_time`, `duration`, `business_seats`, `business_price`, `first_seats`, `first_price`, `second_seats`, `second_price`, `running_days`, `status`)
SELECT 'G9015', 0, s.id, d.id, '08:00:00', '11:30:00', 210, 12, 1195.50, 40, 637.60, 300, 398.50, '1234567', 1
FROM station s JOIN station d ON d.name = '上海虹桥' AND d.deleted = 0 AND d.status = 1
WHERE s.name = '济南西' AND s.deleted = 0 AND s.status = 1
AND NOT EXISTS (SELECT 1 FROM train WHERE train_number = 'G9015');
INSERT INTO `train` (`train_number`, `train_type`, `from_station_id`, `to_station_id`, `depart_time`, `arrive_time`, `duration`, `business_seats`, `business_price`, `first_seats`, `first_price`, `second_seats`, `second_price`, `running_days`, `status`)
SELECT 'G9016', 0, s.id, d.id, '14:00:00', '17:30:00', 210, 12, 1195.50, 40, 637.60, 300, 398.50, '1234567', 1
FROM station s JOIN station d ON d.name = '济南西' AND d.deleted = 0 AND d.status = 1
WHERE s.name = '上海虹桥' AND s.deleted = 0 AND s.status = 1
AND NOT EXISTS (SELECT 1 FROM train WHERE train_number = 'G9016');
INSERT INTO `train` (`train_number`, `train_type`, `from_station_id`, `to_station_id`, `depart_time`, `arrive_time`, `duration`, `business_seats`, `business_price`, `first_seats`, `first_price`, `second_seats`, `second_price`, `running_days`, `status`)
SELECT 'G9017', 0, s.id, d.id, '08:00:00', '11:00:00', 180, 12, 927.00, 40, 494.40, 300, 309.00, '1234567', 1
FROM station s JOIN station d ON d.name = '郑州东' AND d.deleted = 0 AND d.status = 1
WHERE s.name = '北京西' AND s.deleted = 0 AND s.status = 1
AND NOT EXISTS (SELECT 1 FROM train WHERE train_number = 'G9017');
INSERT INTO `train` (`train_number`, `train_type`, `from_station_id`, `to_station_id`, `depart_time`, `arrive_time`, `duration`, `business_seats`, `business_price`, `first_seats`, `first_price`, `second_seats`, `second_price`, `running_days`, `status`)
SELECT 'G9018', 0, s.id, d.id, '14:00:00', '17:00:00', 180, 12, 927.00, 40, 494.40, 300, 309.00, '1234567', 1
FROM station s JOIN station d ON d.name = '北京西' AND d.deleted = 0 AND d.status = 1
WHERE s.name = '郑州东' AND s.deleted = 0 AND s.status = 1
AND NOT EXISTS (SELECT 1 FROM train WHERE train_number = 'G9018');
INSERT INTO `train` (`train_number`, `train_type`, `from_station_id`, `to_station_id`, `depart_time`, `arrive_time`, `duration`, `business_seats`, `business_price`, `first_seats`, `first_price`, `second_seats`, `second_price`, `running_days`, `status`)
SELECT 'G9019', 0, s.id, d.id, '08:00:00', '10:00:00', 120, 12, 717.00, 40, 382.40, 300, 239.00, '1234567', 1
FROM station s JOIN station d ON d.name = '西安北' AND d.deleted = 0 AND d.status = 1
WHERE s.name = '郑州东' AND s.deleted = 0 AND s.status = 1
AND NOT EXISTS (SELECT 1 FROM train WHERE train_number = 'G9019');
INSERT INTO `train` (`train_number`, `train_type`, `from_station_id`, `to_station_id`, `depart_time`, `arrive_time`, `duration`, `business_seats`, `business_price`, `first_seats`, `first_price`, `second_seats`, `second_price`, `running_days`, `status`)
SELECT 'G9020', 0, s.id, d.id, '14:00:00', '16:00:00', 120, 12, 717.00, 40, 382.40, 300, 239.00, '1234567', 1
FROM station s JOIN station d ON d.name = '郑州东' AND d.deleted = 0 AND d.status = 1
WHERE s.name = '西安北' AND s.deleted = 0 AND s.status = 1
AND NOT EXISTS (SELECT 1 FROM train WHERE train_number = 'G9020');
INSERT INTO `train` (`train_number`, `train_type`, `from_station_id`, `to_station_id`, `depart_time`, `arrive_time`, `duration`, `business_seats`, `business_price`, `first_seats`, `first_price`, `second_seats`, `second_price`, `running_days`, `status`)
SELECT 'G9021', 0, s.id, d.id, '08:00:00', '10:00:00', 120, 12, 732.00, 40, 390.40, 300, 244.00, '1234567', 1
FROM station s JOIN station d ON d.name = '武汉' AND d.deleted = 0 AND d.status = 1
WHERE s.name = '郑州东' AND s.deleted = 0 AND s.status = 1
AND NOT EXISTS (SELECT 1 FROM train WHERE train_number = 'G9021');
INSERT INTO `train` (`train_number`, `train_type`, `from_station_id`, `to_station_id`, `depart_time`, `arrive_time`, `duration`, `business_seats`, `business_price`, `first_seats`, `first_price`, `second_seats`, `second_price`, `running_days`, `status`)
SELECT 'G9022', 0, s.id, d.id, '14:00:00', '16:00:00', 120, 12, 732.00, 40, 390.40, 300, 244.00, '1234567', 1
FROM station s JOIN station d ON d.name = '郑州东' AND d.deleted = 0 AND d.status = 1
WHERE s.name = '武汉' AND s.deleted = 0 AND s.status = 1
AND NOT EXISTS (SELECT 1 FROM train WHERE train_number = 'G9022');
INSERT INTO `train` (`train_number`, `train_type`, `from_station_id`, `to_station_id`, `depart_time`, `arrive_time`, `duration`, `business_seats`, `business_price`, `first_seats`, `first_price`, `second_seats`, `second_price`, `running_days`, `status`)
SELECT 'G9023', 0, s.id, d.id, '08:00:00', '09:30:00', 90, 12, 493.50, 40, 263.20, 300, 164.50, '1234567', 1
FROM station s JOIN station d ON d.name = '长沙南' AND d.deleted = 0 AND d.status = 1
WHERE s.name = '武汉' AND s.deleted = 0 AND s.status = 1
AND NOT EXISTS (SELECT 1 FROM train WHERE train_number = 'G9023');
INSERT INTO `train` (`train_number`, `train_type`, `from_station_id`, `to_station_id`, `depart_time`, `arrive_time`, `duration`, `business_seats`, `business_price`, `first_seats`, `first_price`, `second_seats`, `second_price`, `running_days`, `status`)
SELECT 'G9024', 0, s.id, d.id, '14:00:00', '15:30:00', 90, 12, 493.50, 40, 263.20, 300, 164.50, '1234567', 1
FROM station s JOIN station d ON d.name = '武汉' AND d.deleted = 0 AND d.status = 1
WHERE s.name = '长沙南' AND s.deleted = 0 AND s.status = 1
AND NOT EXISTS (SELECT 1 FROM train WHERE train_number = 'G9024');
INSERT INTO `train` (`train_number`, `train_type`, `from_station_id`, `to_station_id`, `depart_time`, `arrive_time`, `duration`, `business_seats`, `business_price`, `first_seats`, `first_price`, `second_seats`, `second_price`, `running_days`, `status`)
SELECT 'G9025', 0, s.id, d.id, '08:00:00', '10:40:00', 160, 12, 942.00, 40, 502.40, 300, 314.00, '1234567', 1
FROM station s JOIN station d ON d.name = '广州南' AND d.deleted = 0 AND d.status = 1
WHERE s.name = '长沙南' AND s.deleted = 0 AND s.status = 1
AND NOT EXISTS (SELECT 1 FROM train WHERE train_number = 'G9025');
INSERT INTO `train` (`train_number`, `train_type`, `from_station_id`, `to_station_id`, `depart_time`, `arrive_time`, `duration`, `business_seats`, `business_price`, `first_seats`, `first_price`, `second_seats`, `second_price`, `running_days`, `status`)
SELECT 'G9026', 0, s.id, d.id, '14:00:00', '16:40:00', 160, 12, 942.00, 40, 502.40, 300, 314.00, '1234567', 1
FROM station s JOIN station d ON d.name = '长沙南' AND d.deleted = 0 AND d.status = 1
WHERE s.name = '广州南' AND s.deleted = 0 AND s.status = 1
AND NOT EXISTS (SELECT 1 FROM train WHERE train_number = 'G9026');
INSERT INTO `train` (`train_number`, `train_type`, `from_station_id`, `to_station_id`, `depart_time`, `arrive_time`, `duration`, `business_seats`, `business_price`, `first_seats`, `first_price`, `second_seats`, `second_price`, `running_days`, `status`)
SELECT 'G9027', 0, s.id, d.id, '08:00:00', '12:30:00', 270, 12, 945.00, 40, 504.00, 300, 315.00, '1234567', 1
FROM station s JOIN station d ON d.name = '上海虹桥' AND d.deleted = 0 AND d.status = 1
WHERE s.name = '武汉' AND s.deleted = 0 AND s.status = 1
AND NOT EXISTS (SELECT 1 FROM train WHERE train_number = 'G9027');
INSERT INTO `train` (`train_number`, `train_type`, `from_station_id`, `to_station_id`, `depart_time`, `arrive_time`, `duration`, `business_seats`, `business_price`, `first_seats`, `first_price`, `second_seats`, `second_price`, `running_days`, `status`)
SELECT 'G9028', 0, s.id, d.id, '14:00:00', '18:30:00', 270, 12, 945.00, 40, 504.00, 300, 315.00, '1234567', 1
FROM station s JOIN station d ON d.name = '武汉' AND d.deleted = 0 AND d.status = 1
WHERE s.name = '上海虹桥' AND s.deleted = 0 AND s.status = 1
AND NOT EXISTS (SELECT 1 FROM train WHERE train_number = 'G9028');
INSERT INTO `train` (`train_number`, `train_type`, `from_station_id`, `to_station_id`, `depart_time`, `arrive_time`, `duration`, `business_seats`, `business_price`, `first_seats`, `first_price`, `second_seats`, `second_price`, `running_days`, `status`)
SELECT 'G9029', 0, s.id, d.id, '08:00:00', '09:40:00', 100, 12, 462.00, 40, 246.40, 300, 154.00, '1234567', 1
FROM station s JOIN station d ON d.name = '重庆西' AND d.deleted = 0 AND d.status = 1
WHERE s.name = '成都东' AND s.deleted = 0 AND s.status = 1
AND NOT EXISTS (SELECT 1 FROM train WHERE train_number = 'G9029');
INSERT INTO `train` (`train_number`, `train_type`, `from_station_id`, `to_station_id`, `depart_time`, `arrive_time`, `duration`, `business_seats`, `business_price`, `first_seats`, `first_price`, `second_seats`, `second_price`, `running_days`, `status`)
SELECT 'G9030', 0, s.id, d.id, '14:00:00', '15:40:00', 100, 12, 462.00, 40, 246.40, 300, 154.00, '1234567', 1
FROM station s JOIN station d ON d.name = '成都东' AND d.deleted = 0 AND d.status = 1
WHERE s.name = '重庆西' AND s.deleted = 0 AND s.status = 1
AND NOT EXISTS (SELECT 1 FROM train WHERE train_number = 'G9030');
INSERT INTO `train` (`train_number`, `train_type`, `from_station_id`, `to_station_id`, `depart_time`, `arrive_time`, `duration`, `business_seats`, `business_price`, `first_seats`, `first_price`, `second_seats`, `second_price`, `running_days`, `status`)
SELECT 'G9031', 0, s.id, d.id, '08:00:00', '11:50:00', 230, 12, 789.00, 40, 420.80, 300, 263.00, '1234567', 1
FROM station s JOIN station d ON d.name = '西安北' AND d.deleted = 0 AND d.status = 1
WHERE s.name = '成都东' AND s.deleted = 0 AND s.status = 1
AND NOT EXISTS (SELECT 1 FROM train WHERE train_number = 'G9031');
INSERT INTO `train` (`train_number`, `train_type`, `from_station_id`, `to_station_id`, `depart_time`, `arrive_time`, `duration`, `business_seats`, `business_price`, `first_seats`, `first_price`, `second_seats`, `second_price`, `running_days`, `status`)
SELECT 'G9032', 0, s.id, d.id, '14:00:00', '17:50:00', 230, 12, 789.00, 40, 420.80, 300, 263.00, '1234567', 1
FROM station s JOIN station d ON d.name = '成都东' AND d.deleted = 0 AND d.status = 1
WHERE s.name = '西安北' AND s.deleted = 0 AND s.status = 1
AND NOT EXISTS (SELECT 1 FROM train WHERE train_number = 'G9032');
INSERT INTO `train` (`train_number`, `train_type`, `from_station_id`, `to_station_id`, `depart_time`, `arrive_time`, `duration`, `business_seats`, `business_price`, `first_seats`, `first_price`, `second_seats`, `second_price`, `running_days`, `status`)
SELECT 'G9033', 0, s.id, d.id, '08:00:00', '10:10:00', 130, 12, 417.00, 40, 222.40, 300, 139.00, '1234567', 1
FROM station s JOIN station d ON d.name = '贵阳北' AND d.deleted = 0 AND d.status = 1
WHERE s.name = '重庆西' AND s.deleted = 0 AND s.status = 1
AND NOT EXISTS (SELECT 1 FROM train WHERE train_number = 'G9033');
INSERT INTO `train` (`train_number`, `train_type`, `from_station_id`, `to_station_id`, `depart_time`, `arrive_time`, `duration`, `business_seats`, `business_price`, `first_seats`, `first_price`, `second_seats`, `second_price`, `running_days`, `status`)
SELECT 'G9034', 0, s.id, d.id, '14:00:00', '16:10:00', 130, 12, 417.00, 40, 222.40, 300, 139.00, '1234567', 1
FROM station s JOIN station d ON d.name = '重庆西' AND d.deleted = 0 AND d.status = 1
WHERE s.name = '贵阳北' AND s.deleted = 0 AND s.status = 1
AND NOT EXISTS (SELECT 1 FROM train WHERE train_number = 'G9034');
INSERT INTO `train` (`train_number`, `train_type`, `from_station_id`, `to_station_id`, `depart_time`, `arrive_time`, `duration`, `business_seats`, `business_price`, `first_seats`, `first_price`, `second_seats`, `second_price`, `running_days`, `status`)
SELECT 'G9035', 0, s.id, d.id, '08:00:00', '12:20:00', 260, 12, 969.00, 40, 516.80, 300, 323.00, '1234567', 1
FROM station s JOIN station d ON d.name = '广州南' AND d.deleted = 0 AND d.status = 1
WHERE s.name = '贵阳北' AND s.deleted = 0 AND s.status = 1
AND NOT EXISTS (SELECT 1 FROM train WHERE train_number = 'G9035');
INSERT INTO `train` (`train_number`, `train_type`, `from_station_id`, `to_station_id`, `depart_time`, `arrive_time`, `duration`, `business_seats`, `business_price`, `first_seats`, `first_price`, `second_seats`, `second_price`, `running_days`, `status`)
SELECT 'G9036', 0, s.id, d.id, '14:00:00', '18:20:00', 260, 12, 969.00, 40, 516.80, 300, 323.00, '1234567', 1
FROM station s JOIN station d ON d.name = '贵阳北' AND d.deleted = 0 AND d.status = 1
WHERE s.name = '广州南' AND s.deleted = 0 AND s.status = 1
AND NOT EXISTS (SELECT 1 FROM train WHERE train_number = 'G9036');
INSERT INTO `train` (`train_number`, `train_type`, `from_station_id`, `to_station_id`, `depart_time`, `arrive_time`, `duration`, `business_seats`, `business_price`, `first_seats`, `first_price`, `second_seats`, `second_price`, `running_days`, `status`)
SELECT 'G9037', 0, s.id, d.id, '08:00:00', '10:00:00', 120, 12, 507.00, 40, 270.40, 300, 169.00, '1234567', 1
FROM station s JOIN station d ON d.name = '贵阳北' AND d.deleted = 0 AND d.status = 1
WHERE s.name = '昆明南' AND s.deleted = 0 AND s.status = 1
AND NOT EXISTS (SELECT 1 FROM train WHERE train_number = 'G9037');
INSERT INTO `train` (`train_number`, `train_type`, `from_station_id`, `to_station_id`, `depart_time`, `arrive_time`, `duration`, `business_seats`, `business_price`, `first_seats`, `first_price`, `second_seats`, `second_price`, `running_days`, `status`)
SELECT 'G9038', 0, s.id, d.id, '14:00:00', '16:00:00', 120, 12, 507.00, 40, 270.40, 300, 169.00, '1234567', 1
FROM station s JOIN station d ON d.name = '昆明南' AND d.deleted = 0 AND d.status = 1
WHERE s.name = '贵阳北' AND s.deleted = 0 AND s.status = 1
AND NOT EXISTS (SELECT 1 FROM train WHERE train_number = 'G9038');
INSERT INTO `train` (`train_number`, `train_type`, `from_station_id`, `to_station_id`, `depart_time`, `arrive_time`, `duration`, `business_seats`, `business_price`, `first_seats`, `first_price`, `second_seats`, `second_price`, `running_days`, `status`)
SELECT 'G9039', 0, s.id, d.id, '08:00:00', '09:00:00', 60, 12, 202.50, 40, 108.00, 300, 67.50, '1234567', 1
FROM station s JOIN station d ON d.name = '南京南' AND d.deleted = 0 AND d.status = 1
WHERE s.name = '合肥南' AND s.deleted = 0 AND s.status = 1
AND NOT EXISTS (SELECT 1 FROM train WHERE train_number = 'G9039');
INSERT INTO `train` (`train_number`, `train_type`, `from_station_id`, `to_station_id`, `depart_time`, `arrive_time`, `duration`, `business_seats`, `business_price`, `first_seats`, `first_price`, `second_seats`, `second_price`, `running_days`, `status`)
SELECT 'G9040', 0, s.id, d.id, '14:00:00', '15:00:00', 60, 12, 202.50, 40, 108.00, 300, 67.50, '1234567', 1
FROM station s JOIN station d ON d.name = '合肥南' AND d.deleted = 0 AND d.status = 1
WHERE s.name = '南京南' AND s.deleted = 0 AND s.status = 1
AND NOT EXISTS (SELECT 1 FROM train WHERE train_number = 'G9040');
INSERT INTO `train` (`train_number`, `train_type`, `from_station_id`, `to_station_id`, `depart_time`, `arrive_time`, `duration`, `business_seats`, `business_price`, `first_seats`, `first_price`, `second_seats`, `second_price`, `running_days`, `status`)
SELECT 'G9041', 0, s.id, d.id, '08:00:00', '10:00:00', 120, 12, 534.00, 40, 284.80, 300, 178.00, '1234567', 1
FROM station s JOIN station d ON d.name = '杭州东' AND d.deleted = 0 AND d.status = 1
WHERE s.name = '合肥南' AND s.deleted = 0 AND s.status = 1
AND NOT EXISTS (SELECT 1 FROM train WHERE train_number = 'G9041');
INSERT INTO `train` (`train_number`, `train_type`, `from_station_id`, `to_station_id`, `depart_time`, `arrive_time`, `duration`, `business_seats`, `business_price`, `first_seats`, `first_price`, `second_seats`, `second_price`, `running_days`, `status`)
SELECT 'G9042', 0, s.id, d.id, '14:00:00', '16:00:00', 120, 12, 534.00, 40, 284.80, 300, 178.00, '1234567', 1
FROM station s JOIN station d ON d.name = '合肥南' AND d.deleted = 0 AND d.status = 1
WHERE s.name = '杭州东' AND s.deleted = 0 AND s.status = 1
AND NOT EXISTS (SELECT 1 FROM train WHERE train_number = 'G9042');
INSERT INTO `train` (`train_number`, `train_type`, `from_station_id`, `to_station_id`, `depart_time`, `arrive_time`, `duration`, `business_seats`, `business_price`, `first_seats`, `first_price`, `second_seats`, `second_price`, `running_days`, `status`)
SELECT 'G9043', 0, s.id, d.id, '08:00:00', '09:30:00', 90, 12, 471.00, 40, 251.20, 300, 157.00, '1234567', 1
FROM station s JOIN station d ON d.name = '长沙南' AND d.deleted = 0 AND d.status = 1
WHERE s.name = '南昌西' AND s.deleted = 0 AND s.status = 1
AND NOT EXISTS (SELECT 1 FROM train WHERE train_number = 'G9043');
INSERT INTO `train` (`train_number`, `train_type`, `from_station_id`, `to_station_id`, `depart_time`, `arrive_time`, `duration`, `business_seats`, `business_price`, `first_seats`, `first_price`, `second_seats`, `second_price`, `running_days`, `status`)
SELECT 'G9044', 0, s.id, d.id, '14:00:00', '15:30:00', 90, 12, 471.00, 40, 251.20, 300, 157.00, '1234567', 1
FROM station s JOIN station d ON d.name = '南昌西' AND d.deleted = 0 AND d.status = 1
WHERE s.name = '长沙南' AND s.deleted = 0 AND s.status = 1
AND NOT EXISTS (SELECT 1 FROM train WHERE train_number = 'G9044');
INSERT INTO `train` (`train_number`, `train_type`, `from_station_id`, `to_station_id`, `depart_time`, `arrive_time`, `duration`, `business_seats`, `business_price`, `first_seats`, `first_price`, `second_seats`, `second_price`, `running_days`, `status`)
SELECT 'G9045', 0, s.id, d.id, '08:00:00', '09:30:00', 90, 12, 255.00, 40, 136.00, 300, 85.00, '1234567', 1
FROM station s JOIN station d ON d.name = '厦门北' AND d.deleted = 0 AND d.status = 1
WHERE s.name = '福州南' AND s.deleted = 0 AND s.status = 1
AND NOT EXISTS (SELECT 1 FROM train WHERE train_number = 'G9045');
INSERT INTO `train` (`train_number`, `train_type`, `from_station_id`, `to_station_id`, `depart_time`, `arrive_time`, `duration`, `business_seats`, `business_price`, `first_seats`, `first_price`, `second_seats`, `second_price`, `running_days`, `status`)
SELECT 'G9046', 0, s.id, d.id, '14:00:00', '15:30:00', 90, 12, 255.00, 40, 136.00, 300, 85.00, '1234567', 1
FROM station s JOIN station d ON d.name = '福州南' AND d.deleted = 0 AND d.status = 1
WHERE s.name = '厦门北' AND s.deleted = 0 AND s.status = 1
AND NOT EXISTS (SELECT 1 FROM train WHERE train_number = 'G9046');
INSERT INTO `train` (`train_number`, `train_type`, `from_station_id`, `to_station_id`, `depart_time`, `arrive_time`, `duration`, `business_seats`, `business_price`, `first_seats`, `first_price`, `second_seats`, `second_price`, `running_days`, `status`)
SELECT 'G9047', 0, s.id, d.id, '08:00:00', '12:20:00', 260, 12, 855.00, 40, 456.00, 300, 285.00, '1234567', 1
FROM station s JOIN station d ON d.name = '杭州东' AND d.deleted = 0 AND d.status = 1
WHERE s.name = '福州南' AND s.deleted = 0 AND s.status = 1
AND NOT EXISTS (SELECT 1 FROM train WHERE train_number = 'G9047');
INSERT INTO `train` (`train_number`, `train_type`, `from_station_id`, `to_station_id`, `depart_time`, `arrive_time`, `duration`, `business_seats`, `business_price`, `first_seats`, `first_price`, `second_seats`, `second_price`, `running_days`, `status`)
SELECT 'G9048', 0, s.id, d.id, '14:00:00', '18:20:00', 260, 12, 855.00, 40, 456.00, 300, 285.00, '1234567', 1
FROM station s JOIN station d ON d.name = '福州南' AND d.deleted = 0 AND d.status = 1
WHERE s.name = '杭州东' AND s.deleted = 0 AND s.status = 1
AND NOT EXISTS (SELECT 1 FROM train WHERE train_number = 'G9048');
