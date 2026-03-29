package com.c.domain.protocol.adapter.repository;

import com.c.domain.protocol.model.valobj.enums.ProtocolStatusEnum;
import com.c.domain.protocol.model.valobj.http.HTTPProtocolVO;

import java.util.List;

/**
 * 协议仓储服务接口
 * 定义协议数据的持久化与查询操作契约
 *
 * @author cyh
 * @date 2026/03/29
 */
public interface ProtocolRepository {

    /**
     * 保存或更新协议信息，保证操作幂等性
     *
     * @param protocolVO 协议信息视图对象
     * @return 协议唯一标识
     */
    Long saveOrUpdateProtocol(HTTPProtocolVO protocolVO);

    /**
     * 批量保存协议信息，用于初始化数据导入场景
     *
     * @param protocolVOS 协议信息视图对象集合
     * @return 协议唯一标识集合
     */
    List<Long> batchSaveProtocols(List<HTTPProtocolVO> protocolVOS);

    /**
     * 查询单个协议的完整配置信息，包含所有字段映射规则
     * 网关执行引擎加载协议配置的核心方法
     *
     * @param protocolId 协议唯一标识
     * @return 协议完整信息视图对象
     */
    HTTPProtocolVO queryProtocolDetail(Long protocolId);

    /**
     * 根据请求URL和请求方法精确查询协议信息
     *
     * @param url    请求URL
     * @param method 请求方法
     * @return 协议信息视图对象
     */
    HTTPProtocolVO queryByUrl(String url, String method);

    /**
     * 更新协议启用/禁用状态
     *
     * @param protocolId 协议唯一标识
     * @param status     协议状态枚举
     */
    void updateStatus(Long protocolId, ProtocolStatusEnum status);

    /**
     * 分页查询协议列表，供管理后台界面使用
     *
     * @param urlKeyword URL模糊查询关键字
     * @param page       页码
     * @param size       每页条数
     * @return 协议信息视图对象集合
     */
    List<HTTPProtocolVO> queryProtocolPage(String urlKeyword, Integer page, Integer size);

    /**
     * 获取所有处于启用状态的协议唯一标识集合
     *
     * @return 活跃协议 ID 列表，用于网关本地缓存的增量同步或全量补偿
     */
    List<Long> queryAllActiveProtocolIds();
}