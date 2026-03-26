package com.c.infrastructure.dao;

import com.c.infrastructure.dao.po.McpGatewayAuthPO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 用户网关权限数据访问接口
 *
 * @author cyh
 * @date 2026/03/23
 */
@Mapper
public interface McpGatewayAuthDao {

    /**
     * 新增网关权限配置
     *
     * @param po 网关权限PO对象
     * @return 受影响行数
     */
    int insert(McpGatewayAuthPO po);

    /**
     * 根据ID删除网关权限
     *
     * @param id 主键ID
     * @return 受影响行数
     */
    int deleteById(Long id);

    /**
     * 根据ID更新网关权限
     *
     * @param po 网关权限PO对象
     * @return 受影响行数
     */
    int updateById(McpGatewayAuthPO po);

    /**
     * 根据ID查询网关权限
     *
     * @param gatewayId 网关ID
     * @return 网关权限PO对象
     */
    McpGatewayAuthPO queryByGatewayId(String gatewayId);

    /**
     * 查询所有网关权限配置
     *
     * @return 网关权限PO列表
     */
    List<McpGatewayAuthPO> queryAll();

}