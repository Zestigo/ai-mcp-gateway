package com.c.domain.gateway.model.entity;

import com.c.domain.gateway.model.valobj.GatewayToolConfigVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 网关工具配置命令实体
 * 用于封装网关工具配置相关操作的入参数据
 *
 * @author cyh
 * @date 2026/03/29
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GatewayToolConfigCommandEntity {

    /** 网关工具配置值对象 */
    private GatewayToolConfigVO gatewayToolConfigVO;

    /**
     * 构建更新网关工具协议的命令实体
     *
     * @param gatewayId    网关唯一标识
     * @param toolId       工具ID
     * @param protocolId   协议ID
     * @param protocolType 协议类型
     * @return 网关工具配置命令实体
     */
    public static GatewayToolConfigCommandEntity buildUpdateGatewayProtocol(String gatewayId, Long toolId,
                                                                            Long protocolId, String protocolType) {
        return GatewayToolConfigCommandEntity
                .builder()
                .gatewayToolConfigVO(GatewayToolConfigVO
                        .builder()
                        .gatewayId(gatewayId)
                        .toolId(toolId)
                        .protocolId(protocolId)
                        .protocolType(protocolType)
                        .build())
                .build();
    }

}