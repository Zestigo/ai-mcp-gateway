package com.c.test.domain.session.message;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.c.domain.session.model.valobj.McpSchemaVO;
import com.c.domain.session.service.message.handler.IRequestHandler;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.HashMap;

/**
 * MCP 工具列表查询处理器单元测试
 * 职责：验证 tools/list 协议在网关层面的解析与响应逻辑。
 */
@Slf4j
@SpringBootTest
public class ToolsListHandlerTest {

    /** 工具列表查询处理器：对应 Spring 容器中的 toolsListHandler Bean */
    @Resource(name = "toolsListHandler")
    private IRequestHandler toolsListHandler;

    /**
     * 测试工具列表查询功能
     * 验证：发送标准 JSON-RPC 请求，通过响应式流断言获取工具列表响应。
     */
    @Test
    public void test_handle_tools_list_success() {
        // 1. 构建标准 MCP JSON-RPC 请求 (使用全参构造函数替代 Builder)
        String gatewayId = "gateway_001";
        McpSchemaVO.JSONRPCRequest request = new McpSchemaVO.JSONRPCRequest("2.0", "tools/list", "test-req-id-001",
                new HashMap<>());

        // 2. 执行请求处理，获取响应流 (Reactor Flux)
        Flux<McpSchemaVO.JSONRPCResponse> responseFlux = toolsListHandler.handle(gatewayId, request);

        // 3. 使用 StepVerifier 进行响应式流验证
        StepVerifier
                .create(responseFlux)
                .assertNext(response -> {
                    // 校验响应基本属性 (如果是 Record 则使用 id()，如果是普通 POJO 则使用 getId())
                    Assertions.assertNotNull(response, "响应对象不能为空");
                    Assertions.assertEquals("test-req-id-001", response.id());

                    // 使用 Fastjson2 标准的 PrettyFormat 进行日志打印
                    log.info("MCP 响应结果明细: \n{}", JSON.toJSONString(response, JSONWriter.Feature.PrettyFormat));

                    // 业务逻辑校验：验证返回的 Result 结果集
                    Assertions.assertNotNull(response.result(), "响应结果中的 Result 节点不能为空");
                })
                .expectComplete() // 验证流是否正常结束
                .verify();
    }
}