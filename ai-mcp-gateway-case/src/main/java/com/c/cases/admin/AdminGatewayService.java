package com.c.cases.admin;

import com.c.domain.admin.model.entity.GatewayConfigEntity;
import com.c.domain.admin.model.valobj.PageResponse;

/**
 * 管理后台网关配置服务接口
 * 定义网关基础配置相关的业务操作标准
 *
 * @author cyh
 * @date 2026/03/31
 */
public interface AdminGatewayService {

    /**
     * 分页查询网关配置列表
     *
     * @param pageNo   当前页码
     * @param pageSize 每页条数
     * @param keyword  搜索关键词
     * @param status   网关状态
     * @return 分页网关配置数据
     */
    PageResponse<GatewayConfigEntity> queryGatewayConfigPage(int pageNo, int pageSize, String keyword, Integer status);

    /**
     * 创建网关配置
     *
     * @param entity 网关配置实体
     * @return 创建结果
     */
    boolean createGateway(GatewayConfigEntity entity);

    /**
     * 更新网关配置（带CAS乐观锁）
     *
     * @param entity     网关配置实体
     * @param oldVersion 乐观锁旧版本号
     * @return 更新结果
     */
    boolean updateGateway(GatewayConfigEntity entity, Long oldVersion);

    /**
     * 删除网关配置
     *
     * @param gatewayId 网关唯一标识
     * @return 删除结果
     */
    boolean deleteGateway(String gatewayId);

    /**
     * 根据网关ID查询网关详情
     *
     * @param gatewayId 网关唯一标识
     * @return 网关配置实体
     */
    GatewayConfigEntity findGatewayById(String gatewayId);

    /**
     * 启用网关（带CAS乐观锁）
     *
     * @param gatewayId  网关唯一标识
     * @param oldVersion 乐观锁旧版本号
     * @return 启用结果
     */
    boolean enableGateway(String gatewayId, Long oldVersion);

    /**
     * 禁用网关（带CAS乐观锁）
     *
     * @param gatewayId  网关唯一标识
     * @param oldVersion 乐观锁旧版本号
     * @return 禁用结果
     */
    boolean disableGateway(String gatewayId, Long oldVersion);

}