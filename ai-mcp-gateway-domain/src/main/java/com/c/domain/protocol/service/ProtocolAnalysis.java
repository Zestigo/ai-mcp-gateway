package com.c.domain.protocol.service;

import com.c.domain.protocol.model.entity.AnalysisCommandEntity;
import com.c.domain.protocol.model.valobj.http.HTTPProtocolVO;

import java.util.List;

/**
 * 协议解析领域服务接口
 * 定义协议解析核心业务契约，支持多类型协议解析扩展
 *
 * @author cyh
 * @date 2026/03/28
 */
public interface ProtocolAnalysis {

    /**
     * 执行协议解析
     *
     * @param commandEntity 解析命令实体
     * @return 解析完成的HTTP协议值对象列表
     * @throws IllegalArgumentException 命令实体不合法时抛出
     */
    List<HTTPProtocolVO> doAnalysis(AnalysisCommandEntity commandEntity);

}