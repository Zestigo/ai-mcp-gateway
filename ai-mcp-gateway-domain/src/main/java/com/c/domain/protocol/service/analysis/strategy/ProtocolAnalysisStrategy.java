package com.c.domain.protocol.service.analysis.strategy;

import com.alibaba.fastjson2.JSONObject;
import com.c.domain.protocol.model.valobj.http.HTTPProtocolVO;

import java.util.List;

/**
 * 协议解析策略接口
 * 定义协议解析的匹配与执行契约
 *
 * @author cyh
 * @date 2026/03/28
 */
public interface ProtocolAnalysisStrategy {

    /**
     * 判断策略是否匹配当前解析节点
     *
     * @param operation OpenAPI操作节点
     * @return 匹配返回true，否则false
     */
    boolean match(JSONObject operation);

    /**
     * 执行解析逻辑
     *
     * @param operation OpenAPI操作节点
     * @param schemas   全局组件定义池
     * @param mappings  映射结果集合
     */
    void doAnalysis(JSONObject operation, JSONObject schemas, List<HTTPProtocolVO.ProtocolMapping> mappings);
}