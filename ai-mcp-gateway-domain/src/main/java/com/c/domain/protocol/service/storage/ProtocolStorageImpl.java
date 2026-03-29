package com.c.domain.protocol.service.storage;

import com.c.domain.protocol.model.valobj.http.HTTPProtocolVO;
import com.c.domain.protocol.service.ProtocolStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * 协议存储服务实现类
 * 职责：负责将解析后的协议配置及映射规则持久化至数据库。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProtocolStorageImpl implements ProtocolStorage {

    // private final IProtocolRepository protocolRepository;

    /**
     * 批量存储协议配置（全量覆盖模式）
     * 关键点：由外部调用此方法或通过代理调用以确保事务生效。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void store(String gatewayId, List<HTTPProtocolVO> protocolVOs) {
        if (gatewayId == null || CollectionUtils.isEmpty(protocolVOs)) {
            log.warn("存储请求参数不完整，gatewayId: {}", gatewayId);
            return;
        }

        try {
            log.info("开始执行协议持久化，网关ID: {}, 接口数量: {}", gatewayId, protocolVOs.size());

            // 1. 【核心优化】清理旧数据：保证“更新”语义是全量替换，防止规则冗余
            // protocolRepository.deleteByGatewayId(gatewayId);

            for (HTTPProtocolVO vo : protocolVOs) {
                if (vo.getHttpUrl() == null || vo.getHttpMethod() == null) {
                    continue;
                }

                // 2. 【核心动作】持久化到数据库
                // 提示：Repository 内部应处理 mappings 列表到 LONGTEXT 的 JSON 转换
                // protocolRepository.saveProtocol(gatewayId, vo);
            }

            log.info("网关 {} 的协议配置同步完成", gatewayId);
        } catch (Exception e) {
            log.error("协议持久化失败，触发回滚。gatewayId: {}", gatewayId, e);
            throw new RuntimeException("Protocol storage error", e);
        }
    }

    /**
     * 更新协议配置
     * 修正：直接在接口方法上声明事务，或者确保 store 能够被代理拦截。
     */
    @Override
    @Transactional(rollbackFor = Exception.class) // 修正：在入口处加事务，避免自调用失效
    public void update(String gatewayId, List<HTTPProtocolVO> protocolVOs) {
        // 在同一个 Bean 内部，直接调用 store() 现在是安全的，
        // 因为事务是在 update 被外部代理调用时开启的，store 会加入这个事务。
        this.store(gatewayId, protocolVOs);
    }
}