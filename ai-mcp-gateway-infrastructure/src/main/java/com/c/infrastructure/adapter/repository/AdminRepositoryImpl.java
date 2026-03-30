package com.c.infrastructure.adapter.repository;

import com.c.domain.admin.adapter.repository.AdminRepository;
import com.c.domain.admin.model.entity.GatewayConfigEntity;
import com.c.infrastructure.dao.McpGatewayDao;
import com.c.infrastructure.dao.po.McpGatewayPO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理员仓储实现
 * 负责网关配置数据的持久化操作，实现领域层定义的数据访问接口
 * 提供网关配置查询、状态更新等数据操作能力
 *
 * @author cyh
 * @date 2026/03/29
 */
@Slf4j
@Repository
public class AdminRepositoryImpl implements AdminRepository {

    /** 网关配置数据访问对象，负责数据库交互 */
    @Resource
    private McpGatewayDao mcpGatewayDao;

    /**
     * 查询网关配置列表
     * 从数据库获取所有网关配置，并转换为领域实体对象
     *
     * @return 网关配置领域实体列表，查询异常返回空集合
     */
    @Override
    public List<GatewayConfigEntity> queryGatewayConfigList() {
        try {
            // 调用DAO层查询全量网关配置数据
            List<McpGatewayPO> mcpGatewayPOS = mcpGatewayDao.queryAll();

            // 空集合判断，避免空指针异常
            if (null == mcpGatewayPOS || mcpGatewayPOS.isEmpty()) {
                return Collections.emptyList();
            }

            // 数据对象转换：持久化对象PO -> 领域实体Entity
            // 网关版本号用于配置下发时的顺序控制与乐观锁
            return mcpGatewayPOS
                    .stream()
                    .map(po -> GatewayConfigEntity
                            .builder()
                            .gatewayId(po.getGatewayId())
                            .gatewayName(po.getGatewayName())
                            .gatewayDescription(po.getGatewayDescription())
                            .gatewayVersion(po.getGatewayVersion())
                            .auth(po.getAuth())
                            .status(po.getStatus())
                            .build())
                    .collect(Collectors.toList());

        } catch (Exception e) {
            // 捕获查询异常，打印日志并返回空集合，保证上层业务不中断
            log.error("查询网关配置列表异常", e);
            return Collections.emptyList();
        }
    }

    /**
     * 基于乐观锁更新网关状态
     * 依赖版本号保证并发场景下的数据一致性
     *
     * @param gatewayId  网关业务标识
     * @param oldVersion 网关当前版本号
     * @param status     待更新的网关状态
     * @return 更新成功返回true，失败返回false
     */
    public boolean updateGatewayStatus(String gatewayId, String oldVersion, Integer status) {
        // 执行乐观锁更新，根据返回影响行数判断是否更新成功
        return mcpGatewayDao.updateStatusWithVersion(gatewayId, oldVersion, status) > 0;
    }
}