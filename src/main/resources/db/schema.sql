-- =============================================
-- 火车票智能客服管理系统 数据库初始化脚本
-- 数据库: ticket_booking
-- 字符集: utf8mb4
-- =============================================

CREATE TABLE IF NOT EXISTS `user` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username`    VARCHAR(50)   NOT NULL COMMENT '用户名',
    `password`    VARCHAR(100)  NOT NULL COMMENT '密码(加密)',
    `nickname`    VARCHAR(50)   DEFAULT NULL COMMENT '昵称',
    `phone`       VARCHAR(20)   DEFAULT NULL COMMENT '手机号',
    `id_card`     VARCHAR(20)   DEFAULT NULL COMMENT '身份证号',
    `email`       VARCHAR(100)  DEFAULT NULL COMMENT '邮箱',
    `avatar`      VARCHAR(255)  DEFAULT NULL COMMENT '头像URL',
    `role`        TINYINT       NOT NULL DEFAULT 0 COMMENT '角色: 0-普通用户 1-管理员',
    `status`      TINYINT       NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-正常',
    `balance`     DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '钱包余额',
    `deleted`     TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
    `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE IF NOT EXISTS `passenger` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '乘客ID',
    `user_id`     BIGINT        NOT NULL COMMENT '用户ID',
    `name`        VARCHAR(50)   NOT NULL COMMENT '乘客姓名',
    `id_card`     VARCHAR(20)   NOT NULL COMMENT '身份证号',
    `phone`       VARCHAR(20)   DEFAULT NULL COMMENT '手机号',
    `type`        TINYINT       NOT NULL DEFAULT 0 COMMENT '票种: 0-成人 1-学生 2-儿童 3-军人',
    `status`      TINYINT       NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-正常',
    `deleted`     TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='乘客信息表';

CREATE TABLE IF NOT EXISTS `station` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '站点ID',
    `name`        VARCHAR(50)   NOT NULL COMMENT '站点名称',
    `code`        VARCHAR(20)   NOT NULL COMMENT '站点代码',
    `pinyin`      VARCHAR(100)  DEFAULT NULL COMMENT '拼音',
    `city`        VARCHAR(50)   DEFAULT NULL COMMENT '所属城市',
    `province`    VARCHAR(50)   DEFAULT NULL COMMENT '所属省份',
    `status`      TINYINT       NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-正常',
    `deleted`     TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`),
    KEY `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站点表';

CREATE TABLE IF NOT EXISTS `train` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '车次ID',
    `train_number`    VARCHAR(20)   NOT NULL COMMENT '车次号',
    `train_type`      TINYINT       NOT NULL DEFAULT 0 COMMENT '车型: 0-G高速 1-D动车 2-K快速 3-Z直达 4-T特快',
    `from_station_id` BIGINT        NOT NULL COMMENT '起始站ID',
    `to_station_id`   BIGINT        NOT NULL COMMENT '终点站ID',
    `depart_time`     TIME          NOT NULL COMMENT '发车时间',
    `arrive_time`     TIME          NOT NULL COMMENT '到达时间',
    `duration`        INT           NOT NULL COMMENT '历时(分钟)',
    `business_seats`  INT           DEFAULT 0 COMMENT '商务座数量',
    `business_price`  DECIMAL(10,2) DEFAULT 0.00 COMMENT '商务座价格',
    `first_seats`     INT           DEFAULT 0 COMMENT '一等座数量',
    `first_price`     DECIMAL(10,2) DEFAULT 0.00 COMMENT '一等座价格',
    `second_seats`    INT           DEFAULT 0 COMMENT '二等座数量',
    `second_price`    DECIMAL(10,2) DEFAULT 0.00 COMMENT '二等座价格',
    `soft_sleepers`   INT           DEFAULT 0 COMMENT '软卧数量',
    `soft_price`      DECIMAL(10,2) DEFAULT 0.00 COMMENT '软卧价格',
    `hard_sleepers`   INT           DEFAULT 0 COMMENT '硬卧数量',
    `hard_price`      DECIMAL(10,2) DEFAULT 0.00 COMMENT '硬卧价格',
    `hard_seats`      INT           DEFAULT 0 COMMENT '硬座数量',
    `seat_price`      DECIMAL(10,2) DEFAULT 0.00 COMMENT '硬座价格',
    `running_days`    VARCHAR(50)   DEFAULT '1234567' COMMENT '运行日(1-7代表周一到周日)',
    `status`          TINYINT       NOT NULL DEFAULT 1 COMMENT '状态: 0-停运 1-运行',
    `deleted`         TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_train_number` (`train_number`),
    KEY `idx_from_to` (`from_station_id`, `to_station_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车次表';

CREATE TABLE IF NOT EXISTS `order` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    `order_number`  VARCHAR(50)   NOT NULL COMMENT '订单号',
    `user_id`       BIGINT        NOT NULL COMMENT '用户ID',
    `train_id`      BIGINT        NOT NULL COMMENT '车次ID',
    `train_number`  VARCHAR(20)   NOT NULL COMMENT '车次号',
    `from_station`  VARCHAR(50)   NOT NULL DEFAULT '' COMMENT '出发站',
    `to_station`    VARCHAR(50)   NOT NULL DEFAULT '' COMMENT '到达站',
    `depart_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发车时间',
    `arrive_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '到达时间',
    `seat_type`     TINYINT       NOT NULL DEFAULT 2 COMMENT '座位类型: 0-商务座 1-一等座 2-二等座 3-软卧 4-硬卧 5-硬座',
    `seat_number`   VARCHAR(20)   DEFAULT NULL COMMENT '座位号',
    `passenger_id`  BIGINT        NOT NULL DEFAULT 0 COMMENT '乘客ID',
    `passenger_name` VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '乘客姓名',
    `passenger_id_card` VARCHAR(20) NOT NULL DEFAULT '' COMMENT '乘客身份证',
    `total_amount`  DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '订单金额',
    `status`        TINYINT       NOT NULL DEFAULT 0 COMMENT '订单状态: 0-待支付 1-已支付 2-已出票 3-已退票 4-已改签 5-已取消',
    `pay_time`      DATETIME      DEFAULT NULL COMMENT '支付时间',
    `refund_time`   DATETIME      DEFAULT NULL COMMENT '退票时间',
    `refund_amount` DECIMAL(10,2) DEFAULT NULL COMMENT '退票金额',
    `refund_fee`    DECIMAL(10,2) DEFAULT NULL COMMENT '退票手续费',
    `cancel_time`   DATETIME      DEFAULT NULL COMMENT '取消时间',
    `remark`        VARCHAR(255)  DEFAULT NULL COMMENT '备注',
    `deleted`       TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_number` (`order_number`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

CREATE TABLE IF NOT EXISTS `ai_chat_record` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `user_id`     BIGINT        DEFAULT NULL COMMENT '用户ID(可为空,匿名)',
    `session_id`  VARCHAR(50)   NOT NULL COMMENT '会话ID',
    `role`        VARCHAR(20)   NOT NULL COMMENT '角色: user-assistant',
    `content`     TEXT          NOT NULL COMMENT '对话内容',
    `intent`      VARCHAR(50)   DEFAULT NULL COMMENT '识别的意图',
    `tokens_used` INT           DEFAULT 0 COMMENT '消耗Token数',
    `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `deleted`     TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_session_id` (`session_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI对话记录表';

CREATE TABLE IF NOT EXISTS `operation_log` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `user_id`     BIGINT        DEFAULT NULL COMMENT '操作用户ID',
    `username`    VARCHAR(50)   DEFAULT NULL COMMENT '操作用户名',
    `module`      VARCHAR(50)   NOT NULL COMMENT '模块',
    `operation`   VARCHAR(100)  NOT NULL COMMENT '操作内容',
    `method`      VARCHAR(200)  DEFAULT NULL COMMENT '请求方法',
    `params`      TEXT          DEFAULT NULL COMMENT '请求参数',
    `ip`          VARCHAR(50)   DEFAULT NULL COMMENT 'IP地址',
    `cost_time`   INT           DEFAULT 0 COMMENT '耗时(ms)',
    `status`      TINYINT       NOT NULL DEFAULT 1 COMMENT '状态: 0-失败 1-成功',
    `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_module` (`module`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- 兼容已有数据库：为user表添加钱包余额字段（如果列已存在会报错，但continue-on-error=true会跳过）
ALTER TABLE `user` ADD COLUMN `balance` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '钱包余额' AFTER `status`;
