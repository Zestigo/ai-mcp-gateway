package com.c.test.domain.session.message;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.c.domain.session.model.valobj.McpSchemaVO;
import com.c.domain.session.service.message.handler.impl.ToolsCallHandler;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.Map;

/**
 * MCP 工具调用处理器单元测试
 * 职责：验证基于 tools/call 协议的 POST/GET 请求转发与响应逻辑。
 */
@Slf4j
@SpringBootTest
public class ToolsCallHandlerTest {

    /** 工具调用处理器：负责将 MCP 请求路由至实际的 HTTP/RPC 接口 */
    @Resource
    private ToolsCallHandler toolsCallHandler;

    /**
     * 测试 POST 类型工具调用
     * 场景：带有复杂嵌套对象的 POST 请求映射。
     */
    @Test
    public void test_post_tools_call() {
        // 1. 模拟标准的工具调用请求参数
        String jsonStr = """
                                {
                                  "name": "JavaSDKMCPClient_getCompanyEmployee",
                                  "arguments": {
                                    "xxxRequest01": {
                                      "city": "北京",
                                      "company": { "name": "jd", "type": "tech" }
                                }
                    }
                }
                """;

        // 2. 构建 JSON-RPC 请求对象（使用 Fastjson2 解析为 Map）
        McpSchemaVO.JSONRPCRequest request = new McpSchemaVO.JSONRPCRequest("2.0", "tools/call", "req-post-001",
                JSON.parseObject(jsonStr, Map.class));

        // 3. 执行请求处理并使用 StepVerifier 验证响应式流
        Flux<McpSchemaVO.JSONRPCResponse> responseFlux = toolsCallHandler.handle("gateway_001", request);

        StepVerifier
                .create(responseFlux)
                .assertNext(response -> {
                    Assertions.assertNotNull(response);
                    Assertions.assertNull(response.error(), "POST 请求不应返回错误");
                    log.info("测试结果(post): \n{}", JSON.toJSONString(response, JSONWriter.Feature.PrettyFormat));
                })
                .verifyComplete();
    }

    /**
     * 测试 GET 类型工具调用
     * 场景：简单 ID 查询的 GET 请求映射。
     */
    @Test
    public void test_get_tools_call() {
        // 1. 模拟查询参数
        String jsonStr = """
                {
                  "name": "JavaSDKMCPClient_queryAiClientById",
                  "arguments": {
                    "req": { "id": 10001 }
                  }
                }
                """;

        // 2. 构建请求并执行
        McpSchemaVO.JSONRPCRequest request = new McpSchemaVO.JSONRPCRequest("2.0", "tools/call", "req-get-001",
                JSON.parseObject(jsonStr, Map.class));

        Flux<McpSchemaVO.JSONRPCResponse> responseFlux = toolsCallHandler.handle("gateway_002", request);

        // 3. 验证结果
        StepVerifier
                .create(responseFlux)
                .assertNext(response -> {
                    Assertions.assertNotNull(response);
                    Assertions.assertNull(response.error(), "GET 请求不应返回错误");
                    log.info("测试结果(get): \n{}", JSON.toJSONString(response, JSONWriter.Feature.PrettyFormat));

                    // 额外逻辑校验：result 节点必须存在内容
                    Assertions.assertNotNull(response.result(), "响应结果 Result 不能为空");
                })
                .verifyComplete();
    }
}