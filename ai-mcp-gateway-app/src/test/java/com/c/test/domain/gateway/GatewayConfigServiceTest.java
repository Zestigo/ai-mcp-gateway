package com.c.test.domain.gateway;

import com.c.domain.gateway.model.entity.GatewayConfigCommandEntity;
import com.c.domain.gateway.model.valobj.GatewayConfigVO;
import com.c.domain.gateway.service.GatewayConfigService;
import com.c.types.enums.GatewayEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
public class GatewayConfigServiceTest {

    @Resource
    private GatewayConfigService gatewayConfigService;

    @Test
    public void test_saveGatewayConfig() {
        GatewayConfigCommandEntity commandEntity = new GatewayConfigCommandEntity();
        GatewayConfigVO gatewayConfigVO = GatewayConfigVO
                .builder()
                .gatewayId("gateway_002")
                .gatewayName("员工信息查询网关")
                .gatewayDescription("用于查询公司员工信息的MCP网关")
                .gatewayVersion("1.0.0")
                .auth(GatewayEnum.GatewayAuthStatusEnum.ENABLE)
                .status(GatewayEnum.GatewayStatus.STRONG_VERIFIED)
                .build();
        commandEntity.setGatewayConfigVO(gatewayConfigVO);

        gatewayConfigService.saveGatewayConfig(commandEntity);
        log.info("保存网关配置成功 gatewayId: {}", gatewayConfigVO.getGatewayId());
    }

    @Test
    public void test_updateGatewayAuthStatus() {
        GatewayConfigCommandEntity commandEntity = GatewayConfigCommandEntity.buildUpdateGatewayAuthStatusVO(
                "gateway_002", GatewayEnum.GatewayAuthStatusEnum.DISABLE);
        gatewayConfigService.updateGatewayAuthStatus(commandEntity);
        log.info("更新网关鉴权状态成功 gatewayId: {}", commandEntity
                .getGatewayConfigVO()
                .getGatewayId());
    }

}