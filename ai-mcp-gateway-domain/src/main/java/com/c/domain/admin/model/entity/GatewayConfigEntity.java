package com.c.domain.admin.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 网关配置领域实体
 * 封装网关基础配置信息
 *
 * @author cyh
 * @date 2026/03/29
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GatewayConfigEntity {

    /** 网关唯一标识，业务主键 */
    private String gatewayId;

    /** 网关名称，用于界面展示 */
    private String gatewayName;

    /** 网关描述，说明网关用途与作用 */
    private String gatewayDescription;

    /** 网关版本，用于乐观锁与配置下发顺序控制 */
    private String gatewayVersion;

    /** 鉴权状态，0-关闭 1-开启 */
    private Integer auth;

    /** 启用状态，0-禁用 1-启用 */
    private Integer status;

}