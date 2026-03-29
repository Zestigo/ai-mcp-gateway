package com.c.domain.protocol.service;

import com.c.domain.protocol.model.entity.StorageCommandEntity;
import com.c.domain.protocol.model.valobj.http.HTTPProtocolVO;

import java.util.List;

/**
 * 协议存储服务接口
 * 定义协议解析结果的持久化契约
 *
 * @author cyh
 * @date 2026/03/29
 */
public interface ProtocolStorage {

    /**
     * 执行协议数据持久化操作
     *
     * @param commandEntity 协议存储命令实体
     * @return 协议唯一标识集合
     */
    List<Long> doStorage(StorageCommandEntity commandEntity);

}