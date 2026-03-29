package com.c.domain.protocol.adapter.repository;

import com.c.domain.protocol.model.valobj.http.HTTPProtocolVO;

/**
 * 协议缓存检索接口
 * 定义协议配置的缓存读取、强制刷新标准行为，实现缓存层依赖倒置
 *
 * @author cyh
 * @date 2026/03/29
 */
public interface ProtocolCacheRetriever {

    /**
     * 获取协议配置信息
     * 优先从本地内存缓存获取，未命中时回源加载数据
     *
     * @param url    请求路径
     * @param method 请求方法
     * @return 协议配置视图对象
     */
    HTTPProtocolVO getProtocol(String url, String method);

    /**
     * 强制刷新本地缓存
     * 从数据库加载最新协议数据并覆盖内存缓存
     *
     * @param protocolId 协议唯一标识
     */
    void forceRefresh(Long protocolId);

}