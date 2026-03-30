package com.c.test.demo;

import com.c.api.common.Response;
import com.c.api.model.request.GatewayConfigRequestDTO;
import com.c.api.model.response.GatewayConfigResponseDTO;
import com.c.api.service.AdminOpenService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import jakarta.annotation.Resource;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * 网关后台开放服务集成测试
 * 覆盖基础配置、工具配置、协议配置及认证配置
 */
@SpringBootTest
public class AdminOpenServiceTest {

    @Resource
    private AdminOpenService adminOpenService;

    @Test
    @DisplayName("1. 测试保存网关基础配置")
    void testSaveGatewayConfig() {
        GatewayConfigRequestDTO.GatewayConfig config = GatewayConfigRequestDTO.GatewayConfig
                .builder()
                .gatewayId("GW_001")
                .gatewayName("生产网关-01")
                .gatewayDescription("核心业务网关")
                .gatewayVersion("v1.0.0")
                .auth(1)   // 对应 GatewayEnum.GatewayAuthStatusEnum
                .status(1) // 对应 GatewayEnum.GatewayStatus
                .build();

        Response<GatewayConfigResponseDTO> response = adminOpenService.saveGatewayConfig(config);

        // 断言：Response 外层状态码为成功
        Assertions.assertEquals("0000", response.getCode());
        // 断言：内部业务 DTO 的 success 字段为 true
        Assertions.assertTrue(response
                .getData()
                .getSuccess());
    }

    @Test
    @DisplayName("2. 测试保存网关认证配置 - 验证 Date 传值")
    void testSaveGatewayAuth() {
        // 构造包含 Date 的请求
        Date expireDate = new Date(System.currentTimeMillis() + 86400000L); // 明天此时

        GatewayConfigRequestDTO.GatewayAuth authRequest = GatewayConfigRequestDTO.GatewayAuth
                .builder()
                .gatewayId("GW_001")
                .rateLimit(500)
                .expireTime(expireDate) // 传入 java.util.Date
                .build();

        Response<GatewayConfigResponseDTO> response = adminOpenService.saveGatewayAuth(authRequest);

        Assertions.assertNotNull(response);
        Assertions.assertEquals("0000", response.getCode());
        Assertions.assertTrue(response
                .getData()
                .getSuccess());
    }

    @Test
    @DisplayName("3. 测试保存网关协议配置 - 复杂嵌套对象")
    void testSaveGatewayProtocol() {
        // 构建 Mapping 列表
        GatewayConfigRequestDTO.GatewayProtocol.ProtocolMapping mapping =
                GatewayConfigRequestDTO.GatewayProtocol.ProtocolMapping
                .builder()
                .mappingType("request")
                .fieldName("orderId")
                .mcpPath("$.order.id")
                .isRequired(1)
                .mcpType("string")
                .build();

        // 构建 HTTP 协议详情
        GatewayConfigRequestDTO.GatewayProtocol.HTTPProtocol httpProtocol =
                GatewayConfigRequestDTO.GatewayProtocol.HTTPProtocol
                .builder()
                .protocolId(2001L)
                .httpUrl("https://api.internal.com/query")
                .httpMethod("POST")
                .timeout(5000)
                .mappings(Collections.singletonList(mapping))
                .build();

        // 构造总请求
        GatewayConfigRequestDTO.GatewayProtocol protocolRequest = GatewayConfigRequestDTO.GatewayProtocol
                .builder()
                .httpProtocols(List.of(httpProtocol))
                .build();

        Response<GatewayConfigResponseDTO> response = adminOpenService.saveGatewayProtocol(protocolRequest);

        Assertions.assertEquals("0000", response.getCode());
        Assertions.assertTrue(response
                .getData()
                .getSuccess());
    }

    @Test
    @DisplayName("4. 测试保存网关工具配置")
    void testSaveGatewayToolConfig() {
        // 构造完整的请求对象，确保所有数据库非空字段都有值
        GatewayConfigRequestDTO.GatewayToolConfig toolRequest = GatewayConfigRequestDTO.GatewayToolConfig
                .builder()
                .gatewayId("GW_001")
                .toolId(3001L)
                .toolName("InventoryTool")
                .toolType("function")
                // 工具描述
                .toolDescription("库存查询工具，用于实时获取仓库剩余量")
                // 版本号，通常这也是数据库必填或业务必需字段
                .toolVersion("1.0.0")
                .protocolId(2001L)
                .protocolType("http")
                .build();

        // 执行调用
        Response<GatewayConfigResponseDTO> response = adminOpenService.saveGatewayToolConfig(toolRequest);

        // 断言结果
        // 如果返回 0001，说明 Controller 捕获到了异常；返回 0000 才是真正的成功
        Assertions.assertEquals("0000", response.getCode(), "接口应返回成功码 0000");
        Assertions.assertNotNull(response.getData(), "返回数据不应为空");
        Assertions.assertTrue(response
                .getData()
                .getSuccess(), "业务逻辑应返回成功标识 true");
    }
}