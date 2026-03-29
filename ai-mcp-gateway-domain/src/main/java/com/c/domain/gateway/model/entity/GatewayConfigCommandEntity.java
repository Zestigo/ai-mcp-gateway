package com.c.domain.gateway.model.entity;

import com.c.domain.gateway.model.valobj.GatewayConfigVO;
import com.c.types.enums.GatewayEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 网关配置命令实体
 * 用于封装网关配置相关操作的入参数据
 *
 * @author cyh
 * @date 2026/03/29
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GatewayConfigCommandEntity {

    /** 网关配置值对象 */
    private GatewayConfigVO gatewayConfigVO;

    /**
     * 构建更新网关认证状态的命令实体
     *
     * @param gatewayId 网关唯一标识
     * @param auth      网关认证状态枚举
     * @return 网关配置命令实体
     */
    public static GatewayConfigCommandEntity buildUpdateGatewayAuthStatusVO(String gatewayId,
                                                                            GatewayEnum.GatewayAuthStatusEnum auth) {
        return GatewayConfigCommandEntity
                .builder()
                .gatewayConfigVO(GatewayConfigVO
                        .builder()
                        .gatewayId(gatewayId)
                        .auth(auth)
                        .build())
                .build();
    }

}