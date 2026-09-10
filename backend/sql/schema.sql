-- ============================================================
-- 轻量化研发缺陷全生命周期管理平台 - 数据库初始化脚本
-- 目标：MySQL 8.0
-- 说明：所有表名以 t_ 前缀、下划线命名；关键字段均建立索引
-- ============================================================

CREATE DATABASE IF NOT EXISTS `defect_platform` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `defect_platform`;

-- ------------------------------------------------------------
-- 1. 用户表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_user`;
CREATE TABLE `t_user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`    VARCHAR(50)  NOT NULL COMMENT '登录用户名',
    `password`    VARCHAR(100) NOT NULL COMMENT '密码(BCrypt加密)',
    `nickname`    VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
    `email`       VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `role`        VARCHAR(20)  NOT NULL DEFAULT 'DEVELOPER' COMMENT '角色: ADMIN-管理员/TESTER-测试/DEVELOPER-开发/GUEST-访客',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 1-启用 0-禁用',
    `avatar`      VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_role` (`role`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户表';

-- ------------------------------------------------------------
-- 2. 项目表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_project`;
CREATE TABLE `t_project` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        VARCHAR(100) NOT NULL COMMENT '项目名称',
    `code`        VARCHAR(50)  NOT NULL COMMENT '项目编码(唯一)',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '项目描述',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 1-进行中 0-已归档',
    `owner_id`    BIGINT       DEFAULT NULL COMMENT '项目负责人用户ID',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`),
    KEY `idx_owner_id` (`owner_id`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '项目表';

-- ------------------------------------------------------------
-- 3. 项目成员表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_project_member`;
CREATE TABLE `t_project_member` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `project_id`  BIGINT      NOT NULL COMMENT '项目ID',
    `user_id`     BIGINT      NOT NULL COMMENT '用户ID',
    `role`        VARCHAR(20) NOT NULL DEFAULT 'DEVELOPER' COMMENT '项目内角色: OWNER/DEV/TESTER/VIEWER',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_project_user` (`project_id`, `user_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '项目成员表';

-- ------------------------------------------------------------
-- 4. 缺陷表（核心业务表）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_defect`;
CREATE TABLE `t_defect` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `project_id`      BIGINT       NOT NULL COMMENT '所属项目ID',
    `title`           VARCHAR(200) NOT NULL COMMENT '缺陷标题',
    `description`     TEXT         COMMENT '缺陷描述',
    `type`            VARCHAR(30)  DEFAULT NULL COMMENT '缺陷类型: FUNCTIONAL/PERFORMANCE/UI/COMPATIBILITY/OPTIMIZATION',
    `priority`        VARCHAR(20)  DEFAULT NULL COMMENT '优先级: URGENT/HIGH/MEDIUM/LOW',
    `severity`        VARCHAR(20)  DEFAULT NULL COMMENT '严重程度: BLOCKER/CRITICAL/MAJOR/MINOR',
    `status`          VARCHAR(30)  NOT NULL DEFAULT 'NEW' COMMENT '状态: NEW/ASSIGNED/PROCESSING/PENDING_RETEST/CLOSED/REJECTED',
    `reporter_id`     BIGINT       NOT NULL COMMENT '提交人用户ID',
    `assignee_id`     BIGINT       DEFAULT NULL COMMENT '当前处理人用户ID',
    `module`          VARCHAR(100) DEFAULT NULL COMMENT '所属模块',
    `environment`     VARCHAR(200) DEFAULT NULL COMMENT '运行环境',
    `reproduce_steps` TEXT         COMMENT '复现步骤',
    `expected_result` TEXT         COMMENT '预期结果',
    `actual_result`   TEXT         COMMENT '实际结果',
    `solution`        TEXT         COMMENT '解决方案',
    `root_cause`      VARCHAR(100) DEFAULT NULL COMMENT '根因分类',
    `reopen_count`    INT          NOT NULL DEFAULT 0 COMMENT '重新打开次数',
    `resolved_time`   DATETIME     DEFAULT NULL COMMENT '解决时间',
    `closed_time`     DATETIME     DEFAULT NULL COMMENT '关闭时间',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_status` (`status`),
    KEY `idx_assignee_id` (`assignee_id`),
    KEY `idx_reporter_id` (`reporter_id`),
    KEY `idx_type` (`type`),
    KEY `idx_priority` (`priority`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '缺陷表';

-- ------------------------------------------------------------
-- 5. 缺陷评论表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_defect_comment`;
CREATE TABLE `t_defect_comment` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `defect_id`   BIGINT   NOT NULL COMMENT '缺陷ID',
    `user_id`     BIGINT   NOT NULL COMMENT '评论人用户ID',
    `content`     TEXT     NOT NULL COMMENT '评论内容',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_defect_id` (`defect_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '缺陷评论表';

-- ------------------------------------------------------------
-- 6. 缺陷操作日志表（只追加、不逻辑删除，保证留痕）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_defect_log`;
CREATE TABLE `t_defect_log` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `defect_id`   BIGINT       NOT NULL COMMENT '缺陷ID',
    `operator_id` BIGINT       NOT NULL COMMENT '操作人用户ID',
    `action`      VARCHAR(30)  NOT NULL COMMENT '操作类型: CREATE/UPDATE/ASSIGN/START/RESOLVE/RETEST/CLOSE/REJECT/COMMENT',
    `from_status` VARCHAR(30)  DEFAULT NULL COMMENT '变更前状态',
    `to_status`   VARCHAR(30)  DEFAULT NULL COMMENT '变更后状态',
    `remark`      VARCHAR(500) DEFAULT NULL COMMENT '操作备注',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_defect_id` (`defect_id`),
    KEY `idx_operator_id` (`operator_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '缺陷操作日志表';

-- ------------------------------------------------------------
-- 7. 附件表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_attachment`;
CREATE TABLE `t_attachment` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `business_type` VARCHAR(30)  NOT NULL COMMENT '业务类型: DEFECT/COMMENT',
    `business_id`   BIGINT       NOT NULL COMMENT '业务主键ID',
    `file_name`     VARCHAR(255) NOT NULL COMMENT '原始文件名',
    `file_path`     VARCHAR(500) NOT NULL COMMENT '存储路径',
    `file_size`     BIGINT       DEFAULT NULL COMMENT '文件大小(字节)',
    `content_type`  VARCHAR(100) DEFAULT NULL COMMENT 'MIME类型',
    `uploader_id`   BIGINT       NOT NULL COMMENT '上传人用户ID',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_business` (`business_type`, `business_id`),
    KEY `idx_uploader_id` (`uploader_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '附件表';

-- ------------------------------------------------------------
-- 8. 知识库表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `t_knowledge`;
CREATE TABLE `t_knowledge` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `title`       VARCHAR(200) NOT NULL COMMENT '知识标题',
    `defect_id`   BIGINT       DEFAULT NULL COMMENT '来源缺陷ID',
    `type`        VARCHAR(30)  DEFAULT NULL COMMENT '缺陷类型',
    `root_cause`  VARCHAR(100) DEFAULT NULL COMMENT '根因分类',
    `solution`    TEXT         COMMENT '解决方案',
    `tags`        VARCHAR(255) DEFAULT NULL COMMENT '标签(逗号分隔)',
    `view_count`  INT          NOT NULL DEFAULT 0 COMMENT '浏览次数',
    `create_by`   BIGINT       DEFAULT NULL COMMENT '创建人用户ID',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_type` (`type`),
    KEY `idx_defect_id` (`defect_id`),
    FULLTEXT KEY `ft_knowledge` (`title`, `root_cause`, `solution`) WITH PARSER ngram
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '知识库表';
