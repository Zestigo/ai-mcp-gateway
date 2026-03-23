package com.c.infrastructure.dao;

import com.c.infrastructure.dao.po.McpProtocolMappingPO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * MCP映射配置数据访问接口
 *
 * @author cyh
 * @date 2026/03/23
 */
@Mapper
public interface IMcpProtocolMappingDao {

    /**
     * 新增MCP映射配置
     *
     * @param po MCP映射配置PO对象
     * @return 受影响行数
     */
    int insert(McpProtocolMappingPO po);

    /**
     * 根据ID删除MCP映射配置
     *
     * @param id 主键ID
     * @return 受影响行数
     */
    int deleteById(Long id);

    /**
     * 根据ID更新MCP映射配置
     *
     * @param po MCP映射配置PO对象
     * @return 受影响行数
     */
    int updateById(McpProtocolMappingPO po);

    /**
     * 根据ID查询MCP映射配置
     *
     * @param id 主键ID
     * @return MCP映射配置PO对象
     */
    McpProtocolMappingPO queryById(Long id);

    /**
     * 查询所有MCP映射配置
     *
     * @return MCP映射配置PO列表
     */
    List<McpProtocolMappingPO> queryAll();
}