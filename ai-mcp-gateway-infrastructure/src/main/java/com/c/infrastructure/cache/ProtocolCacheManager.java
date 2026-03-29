package com.c.infrastructure.cache;

import com.c.domain.protocol.adapter.repository.ProtocolCacheRetriever;
import com.c.domain.protocol.adapter.repository.ProtocolRepository;
import com.c.domain.protocol.model.valobj.http.HTTPProtocolVO;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 协议本地缓存管理器
 * 基于Guava Cache实现协议配置的内存缓存、读取与强制刷新
 *
 * @author cyh
 * @date 2026/03/29
 */
@Slf4j
@Component
public class ProtocolCacheManager implements ProtocolCacheRetriever {

    /** 协议仓储服务，用于缓存未命中时回源查询数据库 */
    @Resource
    private ProtocolRepository protocolRepository;

    /** Guava本地缓存：Key=请求方法:URL，Value=协议完整信息 */
    private final Cache<String, HTTPProtocolVO> protocolCache = CacheBuilder
            .newBuilder()
            .expireAfterWrite(24, TimeUnit.HOURS)
            .maximumSize(10000)
            .build();

    /**
     * 获取协议配置信息，优先从本地缓存读取，未命中则查询数据库并缓存
     *
     * @param url    请求URL
     * @param method 请求方法
     * @return 协议信息视图对象
     */
    @Override
    public HTTPProtocolVO getProtocol(String url, String method) {
        // 构建缓存唯一Key
        String key = buildKey(url, method);

        // 从Guava缓存中获取协议数据
        HTTPProtocolVO vo = protocolCache.getIfPresent(key);
        if (null == vo) {
            log.debug("缓存未命中，尝试从数据库加载协议: {}", key);
            // 缓存未命中，从数据库查询数据
            vo = protocolRepository.queryByUrl(url, method);
            // 查询结果非空，写入本地缓存
            if (null != vo) {
                protocolCache.put(key, vo);
            }
        }
        return vo;
    }

    /**
     * 强制刷新本地缓存，从数据库加载最新协议数据覆盖缓存
     *
     * @param protocolId 协议唯一标识
     */
    @Override
    public void forceRefresh(Long protocolId) {
        // 从数据库查询最新的协议完整信息
        HTTPProtocolVO latestVO = protocolRepository.queryProtocolDetail(protocolId);

        if (null != latestVO) {
            // 构建缓存Key
            String key = buildKey(latestVO.getHttpUrl(), latestVO.getHttpMethod());
            // 覆盖缓存，保证网关使用最新配置
            protocolCache.put(key, latestVO);
            log.info("本地缓存(Guava)已强制刷新完成: protocolId={}, key={}", protocolId, key);
        } else {
            log.warn("强制刷新失败，未找到协议定义: protocolId={}", protocolId);
        }
    }

    /**
     * 构建缓存唯一键，格式：请求方法:URL
     *
     * @param url    请求URL
     * @param method 请求方法
     * @return 缓存键字符串
     */
    private String buildKey(String url, String method) {
        // 请求方法统一转为大写，避免大小写导致缓存不匹配
        return (method != null ? method.toUpperCase() : "GET") + ":" + url;
    }
}