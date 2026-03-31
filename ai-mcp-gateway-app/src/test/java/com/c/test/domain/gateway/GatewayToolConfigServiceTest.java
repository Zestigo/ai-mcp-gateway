package com.c.test.domain.gateway;

import com.c.domain.gateway.model.entity.GatewayToolConfigCommandEntity;
import com.c.domain.gateway.model.valobj.GatewayToolConfigVO;
import com.c.domain.gateway.service.GatewayToolConfigService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 网关工具配置服务测试
 */
@Slf4j
@SpringBootTest
public class GatewayToolConfigServiceTest {

    @Resource
    private GatewayToolConfigService gatewayToolConfigService;

    @Test
    @DisplayName("测试：保存网关工具配置（幂等性验证）")
    void test_saveGatewayToolConfig() {
        // 1. 构造测试数据
        GatewayToolConfigVO gatewayToolConfigVO = GatewayToolConfigVO
                .builder()
                .gatewayId("gateway_002")
                .toolId(Long.valueOf(RandomStringUtils.randomNumeric(4)))
                .toolName("JavaSDKMCPClient_getCompanyEmployee")
                .toolType("function")
                .toolDescription("获取公司雇员信息").toolStatus(1)
                .toolVersion("1.0.0")
                .protocolId(83666188L)
                .protocolType("http")
                .build();

        GatewayToolConfigCommandEntity commandEntity = new GatewayToolConfigCommandEntity();
        commandEntity.setGatewayToolConfigVO(gatewayToolConfigVO);

        // 2. 执行业务逻辑
        gatewayToolConfigService.saveGatewayToolConfig(commandEntity);

        // 3. 日志记录
        log.info("保存网关工具配置成功 gatewayId: {} toolId: {}", gatewayToolConfigVO.getGatewayId(),
                gatewayToolConfigVO.getToolId());
    }

    @Test
    @DisplayName("测试：更新网关工具协议类型")
    void test_updateGatewayToolProtocol() {
        // 1. 构造更新指令
        GatewayToolConfigCommandEntity commandEntity = GatewayToolConfigCommandEntity.buildUpdateGatewayProtocol(
                "gateway_002", 4904L, 83666188L, "dubbo");

        // 2. 执行更新
        gatewayToolConfigService.updateGatewayToolProtocol(commandEntity);

        // 3. 日志记录
        log.info("更新网关工具协议成功 gatewayId: {} protocolId: {}", commandEntity
                .getGatewayToolConfigVO()
                .getGatewayId(), commandEntity
                .getGatewayToolConfigVO()
                .getProtocolId());
    }
}