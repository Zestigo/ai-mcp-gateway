package com.c.api.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 网关配置应答对象
 *
 * @author cyh
 * @date 2026/03/29
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GatewayConfigResponseDTO {

    /** 操作结果 */
    private Boolean success;

}