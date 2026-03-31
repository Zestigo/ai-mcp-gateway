package com.c.cases.admin;

import com.c.domain.admin.model.entity.GatewayProtocolEntity;
import com.c.domain.admin.model.valobj.PageResponse;

import java.util.List;

/**
 * 管理后台网关协议服务接口
 * 定义网关协议配置相关的业务操作标准
 *
 * @author cyh
 * @date 2026/03/31
 */
public interface AdminProtocolService {

    /**
     * 分页查询协议配置列表
     *
     * @param pageNo     当前页码
     * @param pageSize   每页条数
     * @param protocolId 协议ID
     * @param status     协议状态
     * @return 分页协议配置数据
     */
    PageResponse<GatewayProtocolEntity> queryProtocolConfigPage(int pageNo, int pageSize, Long protocolId,
                                                                Integer status);

    /**
     * 查询协议配置列表（不分页）
     *
     * @param protocolId 协议ID
     * @param status     协议状态
     * @return 协议配置列表
     */
    List<GatewayProtocolEntity> getProtocolConfigList(Long protocolId, Integer status);

    /**
     * 创建网关协议配置
     *
     * @param entity 协议配置实体
     * @return 创建结果
     */
    boolean createProtocol(GatewayProtocolEntity entity);

    /**
     * 更新协议配置（带CAS乐观锁）
     *
     * @param entity     协议配置实体
     * @param oldVersion 乐观锁旧版本号
     * @return 更新结果
     */
    boolean updateProtocolConfig(GatewayProtocolEntity entity, Long oldVersion);

    /**
     * 根据协议ID查询协议详情
     *
     * @param protocolId 协议唯一标识
     * @return 协议配置实体
     */
    GatewayProtocolEntity findProtocolById(Long protocolId);

    /**
     * 根据协议ID删除协议配置
     *
     * @param protocolId 协议唯一标识
     * @return 删除结果
     */
    boolean deleteProtocolById(Long protocolId);

    /**
     * 修改协议状态（带CAS乐观锁）
     *
     * @param protocolId 协议唯一标识
     * @param status     目标状态
     * @param oldVersion 乐观锁旧版本号
     * @return 修改结果
     */
    boolean updateProtocolStatus(Long protocolId, Integer status, Long oldVersion);

}