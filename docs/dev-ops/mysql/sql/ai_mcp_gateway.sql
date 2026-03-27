# ************************************************************
# Sequel Ace SQL dump
# 数据库: ai_mcp_gateway
# 生成时间: 2026-03-27 (已根据 Java 业务逻辑优化)
# ************************************************************

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
SET NAMES utf8mb4;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE='NO_AUTO_VALUE_ON_ZERO', SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

CREATE DATABASE IF NOT EXISTS `ai_mcp_gateway` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE `ai_mcp_gateway`;

# 1. MCP 网关基础配置表
# ------------------------------------------------------------
DROP TABLE IF EXISTS `mcp_gateway`;
CREATE TABLE `mcp_gateway` (
                               `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                               `gateway_id` varchar(64) NOT NULL COMMENT '网关唯一标识',
                               `gateway_name` varchar(128) NOT NULL COMMENT '网关名称',
                               `gateway_desc` varchar(512) DEFAULT NULL COMMENT '网关描述',
                               `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
                               `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                               `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                               PRIMARY KEY (`id`),
                               UNIQUE KEY `uk_gateway_id` (`gateway_id`),
                               KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='MCP网关配置表';

# 2. 网关权限与限流表
# ------------------------------------------------------------
DROP TABLE IF EXISTS `mcp_gateway_auth`;
CREATE TABLE `mcp_gateway_auth` (
                                    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                    `gateway_id` varchar(64) NOT NULL COMMENT '网关ID',
                                    `api_key` varchar(128) DEFAULT NULL COMMENT 'API密钥',
                                    `rate_limit` int DEFAULT '1000' COMMENT '速率限制（次/小时）',
                                    `expire_time` datetime DEFAULT NULL COMMENT '过期时间',
                                    `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
                                    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                    PRIMARY KEY (`id`),
                                    UNIQUE KEY `uk_user_gateway` (`gateway_id`),
                                    KEY `idx_api_key` (`api_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户网关权限表';

# 3. MCP 工具注册表 (核心：存储后端 HTTP 映射)
# ------------------------------------------------------------
DROP TABLE IF EXISTS `mcp_protocol_registry`;
CREATE TABLE `mcp_protocol_registry` (
                                         `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                         `gateway_id` varchar(64) NOT NULL COMMENT '所属网关ID',
                                         `tool_id` bigint NOT NULL COMMENT '工具ID',
                                         `tool_name` varchar(128) NOT NULL COMMENT 'MCP工具名称',
                                         `tool_type` varchar(32) NOT NULL DEFAULT 'function' COMMENT '工具类型：function/resource',
                                         `tool_description` varchar(512) DEFAULT NULL COMMENT '工具描述',
                                         `tool_version` varchar(16) NOT NULL DEFAULT '1.0.0' COMMENT '工具版本',
                                         `http_url` varchar(512) NOT NULL COMMENT 'HTTP接口地址',
                                         `http_method` varchar(16) NOT NULL DEFAULT 'POST' COMMENT '请求方法',
                                         `http_headers` text COMMENT 'HTTP请求头 (JSON)',
                                         `timeout` int DEFAULT '30000' COMMENT '超时(ms)',
                                         `retry_times` tinyint DEFAULT '0' COMMENT '重试次数',
                                         `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态',
                                         `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                         `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                         PRIMARY KEY (`id`),
                                         UNIQUE KEY `uk_gateway_tool` (`gateway_id`,`tool_name`),
                                         KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='MCP工具注册表';

# 4. MCP 协议参数映射表 (递归结构实现嵌套对象解析)
# ------------------------------------------------------------
DROP TABLE IF EXISTS `mcp_protocol_mapping`;
CREATE TABLE `mcp_protocol_mapping` (
                                        `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                        `gateway_id` varchar(64) NOT NULL COMMENT '所属网关ID',
                                        `tool_id` bigint NOT NULL COMMENT '所属工具ID',
                                        `mapping_type` varchar(32) NOT NULL COMMENT '类型：request/response',
                                        `parent_path` varchar(256) DEFAULT NULL COMMENT '父级路径 (根节点为NULL)',
                                        `field_name` varchar(128) NOT NULL COMMENT '字段名',
                                        `mcp_path` varchar(256) NOT NULL COMMENT '完整路径 (如 user.info.name)',
                                        `mcp_type` varchar(32) NOT NULL COMMENT '数据类型',
                                        `mcp_desc` varchar(512) DEFAULT NULL COMMENT '字段描述',
                                        `is_required` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否必填',
                                        `http_path` varchar(256) DEFAULT NULL COMMENT '映射到后端的JSON路径',
                                        `http_location` varchar(32) DEFAULT 'body' COMMENT '位置：body/query/path/header',
                                        `sort_order` int DEFAULT '0' COMMENT '排序',
                                        `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                        `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                        PRIMARY KEY (`id`),
                                        KEY `idx_tool_id` (`tool_id`),
                                        KEY `idx_mapping_type` (`mapping_type`),
                                        KEY `idx_mcp_path` (`mcp_path`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='MCP参数映射配置表';

# 5. 会话管理表 (支撑 SessionManagementServiceImpl)
# ------------------------------------------------------------
DROP TABLE IF EXISTS `mcp_session`;
CREATE TABLE `mcp_session` (
                               `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                               `session_id` varchar(64) NOT NULL COMMENT '会话唯一标识(UUID)',
                               `gateway_id` varchar(64) NOT NULL COMMENT '所属网关标识',
                               `host_ip` varchar(32) DEFAULT NULL COMMENT '创建该会话的宿主机IP',
                               `active` tinyint(1) DEFAULT '1' COMMENT '是否有效 (1-有效, 0-失效)',
                               `timeout_seconds` int(11) DEFAULT '1800' COMMENT '超时时间(秒)',
                               `last_access_time` datetime NOT NULL COMMENT '最后访问时间 (用于过期判定)',
                               `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                               `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                               PRIMARY KEY (`id`),
                               UNIQUE KEY `uk_session_id` (`session_id`),
                               KEY `idx_active_access` (`active`, `last_access_time`),
                               KEY `idx_gateway_host` (`gateway_id`, `host_ip`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MCP会话管理表'

# 初始化演示数据
# ------------------------------------------------------------
INSERT INTO `mcp_gateway` (`gateway_id`, `gateway_name`, `gateway_desc`) VALUES ('gateway_001','员工查询网关','演示MCP协议与后端API映射');
INSERT INTO `mcp_gateway_auth` (`gateway_id`, `api_key`) VALUES ('gateway_001', 'RS590LKPOD8877DDLMFKS4');

/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;