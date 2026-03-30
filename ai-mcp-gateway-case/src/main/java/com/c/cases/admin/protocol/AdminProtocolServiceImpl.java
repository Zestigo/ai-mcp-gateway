package com.c.cases.admin.protocol;

import com.c.cases.admin.AdminProtocolService;
import com.c.domain.protocol.model.entity.StorageCommandEntity;
import com.c.domain.protocol.service.ProtocolStorage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 协议配置管理 - 应用层实现
 * 优化点：
 * 1. 增加事务控制，确保协议头与映射明细的存储原子性。
 * 2. 增强日志追踪，便于分析配置变更链路。
 */
@Slf4j
@Service
public class AdminProtocolServiceImpl implements AdminProtocolService {

    @Resource
    private ProtocolStorage protocolStorage;

    /**
     * 保存网关协议配置（应用层实现）
     * 负责协调领域服务，并提供全局事务保障。
     *
     * @param commandEntity 包含 HTTP 协议及其映射规则的命令实体
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveGatewayProtocol(StorageCommandEntity commandEntity) {
        // 1. 基础校验：防止非法调用进入事务流程
        if (null == commandEntity || null == commandEntity.getHttpProtocolVOS()) {
            log.warn("保存网关协议配置中止：命令实体或协议列表为空");
            return;
        }

        try {
            int protocolCount = commandEntity
                    .getHttpProtocolVOS()
                    .size();
            log.info("开始执行网关协议持久化，涉及协议数量: {}", protocolCount);

            // 2. 调用领域服务执行核心存储逻辑
            protocolStorage.doStorage(commandEntity);

            log.info("网关协议持久化任务顺利完成");
        } catch (Exception e) {
            // 3. 错误现场留存：记录 Command 详情，方便根据 protocolId 回溯
            log.error("网关协议配置保存失败，当前执行上下文: {}", commandEntity, e);

            // 重要：必须原样抛出 RuntimeException 及其子类，触发 Spring 事务回滚
            // 如果 e 是 Checked Exception 且未在 rollbackFor 中定义，事务将不会回滚
            throw e;
        }
    }
}