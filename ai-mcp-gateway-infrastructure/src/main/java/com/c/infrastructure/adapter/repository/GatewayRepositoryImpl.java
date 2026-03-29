package com.c.infrastructure.adapter.repository;

import com.c.domain.gateway.adapter.repository.GatewayRepository;
import com.c.domain.gateway.model.entity.GatewayConfigCommandEntity;
import com.c.domain.gateway.model.entity.GatewayToolConfigCommandEntity;
import com.c.domain.gateway.model.valobj.GatewayConfigVO;
import com.c.domain.gateway.model.valobj.GatewayToolConfigVO;
import com.c.infrastructure.dao.McpGatewayDao;
import com.c.infrastructure.dao.McpGatewayToolDao;
import com.c.infrastructure.dao.po.McpGatewayPO;
import com.c.infrastructure.dao.po.McpGatewayToolPO;
import com.c.types.enums.GatewayEnum;
import com.c.types.exception.AppException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import static com.c.types.enums.ResponseCode.DB_UPDATE_FAIL;

/**
 * 网关配置仓储层实现
 * 提供网关基础配置、工具配置、鉴权状态、协议信息的持久化与更新能力
 * 采用先查询后操作模式保证事务安全，增强空值防御
 *
 * @author cyh
 * @date 2026/03/29
 */
@Slf4j
@Repository
public class GatewayRepositoryImpl implements GatewayRepository {

    /** 网关基础配置数据访问对象 */
    @Resource
    private McpGatewayDao mcpGatewayDao;

    /** 网关工具配置数据访问对象 */
    @Resource
    private McpGatewayToolDao mcpGatewayToolDao;

    /**
     * 保存网关配置，存在则更新，不存在则新增
     *
     * @param commandEntity 网关配置操作命令实体
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveGatewayConfig(GatewayConfigCommandEntity commandEntity) {
        // 获取网关配置视图对象，为空直接返回
        GatewayConfigVO vo = commandEntity.getGatewayConfigVO();
        if (null == vo) return;

        // 构建持久化对象，赋值基础属性
        McpGatewayPO po = McpGatewayPO
                .builder()
                .gatewayId(vo.getGatewayId())
                .gatewayName(vo.getGatewayName())
                .gatewayDescription(vo.getGatewayDescription())
                .gatewayVersion(vo.getGatewayVersion())
                .build();
        // 鉴权状态为空则使用默认启用状态
        po.setAuth(null != vo.getAuth() ? vo
                .getAuth()
                .getCode() : GatewayEnum.GatewayAuthStatusEnum.ENABLE.getCode());
        // 网关状态为空则使用默认未核验状态
        po.setStatus(null != vo.getStatus() ? vo
                .getStatus()
                .getCode() : GatewayEnum.GatewayStatus.NOT_VERIFIED.getCode());

        // 先查询是否存在，避免SQL异常导致事务失效
        McpGatewayPO existPo = mcpGatewayDao.queryByGatewayId(vo.getGatewayId());
        if (null == existPo) {
            // 不存在执行新增
            mcpGatewayDao.insert(po);
            log.info("[仓储层] 新增网关配置成功: {}", vo.getGatewayId());
        } else {
            // 已存在执行覆盖更新
            mcpGatewayDao.updateByGatewayId(po);
            log.info("[仓储层] 更新网关配置成功: {}", vo.getGatewayId());
        }
    }

    /**
     * 更新网关鉴权状态
     *
     * @param commandEntity 网关配置操作命令实体
     * @throws AppException 数据库更新失败时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateGatewayAuthStatus(GatewayConfigCommandEntity commandEntity) {
        // 获取配置并校验非空
        GatewayConfigVO vo = commandEntity.getGatewayConfigVO();
        if (null == vo || null == vo.getAuth()) return;

        // 构建仅包含鉴权状态的更新对象
        McpGatewayPO po = new McpGatewayPO();
        po.setGatewayId(vo.getGatewayId());
        po.setAuth(vo
                .getAuth()
                .getCode());

        // 执行更新并校验影响行数
        int count = mcpGatewayDao.updateAuthStatusByGatewayId(po);
        if (count < 1) {
            log.error("[仓储层] 更新网关鉴权状态失败, gatewayId: {}", vo.getGatewayId());
            throw new AppException(DB_UPDATE_FAIL.getCode(), DB_UPDATE_FAIL.getInfo());
        }
    }

    /**
     * 保存网关工具配置，存在则更新，不存在则新增
     *
     * @param commandEntity 网关工具配置操作命令实体
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveGatewayToolConfig(GatewayToolConfigCommandEntity commandEntity) {
        // 获取工具配置视图对象，为空直接返回
        GatewayToolConfigVO vo = commandEntity.getGatewayToolConfigVO();
        if (null == vo) return;

        // 构建工具配置持久化对象
        McpGatewayToolPO po = McpGatewayToolPO
                .builder()
                .gatewayId(vo.getGatewayId())
                .toolId(vo.getToolId())
                .toolName(vo.getToolName())
                .toolType(vo.getToolType())
                .toolDescription(vo.getToolDescription())
                .toolVersion(vo.getToolVersion())
                .protocolId(vo.getProtocolId())
                .protocolType(vo.getProtocolType())
                .build();

        // 查询是否已存在工具配置
        McpGatewayToolPO existTool = mcpGatewayToolDao.queryToolConfig(po);
        if (null == existTool) {
            // 不存在执行新增
            mcpGatewayToolDao.insert(po);
            log.info("[仓储层] 新增工具配置成功: {}:{}", vo.getGatewayId(), vo.getToolId());
        } else {
            // 已存在执行更新
            mcpGatewayToolDao.updateToolConfig(po);
            log.info("[仓储层] 更新工具配置成功: {}:{}", vo.getGatewayId(), vo.getToolId());
        }
    }

    /**
     * 更新网关工具协议配置
     *
     * @param commandEntity 网关工具配置操作命令实体
     * @throws AppException 数据库更新失败时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateGatewayToolProtocol(GatewayToolConfigCommandEntity commandEntity) {
        // 获取配置并校验非空
        GatewayToolConfigVO vo = commandEntity.getGatewayToolConfigVO();
        if (null == vo || null == vo.getToolId()) return;

        // 构建协议更新对象
        McpGatewayToolPO po = McpGatewayToolPO
                .builder()
                .gatewayId(vo.getGatewayId())
                .toolId(vo.getToolId())
                .protocolId(vo.getProtocolId())
                .protocolType(vo.getProtocolType())
                .build();

        // 执行更新并校验结果
        int count = mcpGatewayToolDao.updateProtocolByGatewayId(po);
        if (count < 1) {
            log.error("[仓储层] 更新工具协议失败, toolId: {}", vo.getToolId());
            throw new AppException(DB_UPDATE_FAIL.getCode(), DB_UPDATE_FAIL.getInfo());
        }
    }
}