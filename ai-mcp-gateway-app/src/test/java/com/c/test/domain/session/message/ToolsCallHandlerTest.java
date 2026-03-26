package com.c.test.domain.session.message;

import com.alibaba.fastjson.JSON;
import com.c.domain.session.model.valobj.McpSchemaVO;
import com.c.domain.session.service.message.handler.impl.ToolsCallHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * MCP工具调用处理器单元测试
 * 验证POST/GET请求调用、响应结果处理逻辑
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class ToolsCallHandlerTest {

    /** 工具调用处理器 */
    @Resource
    private ToolsCallHandler toolsCallHandler;

    /** JSON序列化工具 */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 测试POST类型工具调用
     *
     * @throws JsonProcessingException JSON解析异常
     */
    @Test
    public void test_post() throws JsonProcessingException {
        // 模拟工具调用请求参数
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

        // 构建JSON-RPC请求对象
        McpSchemaVO.JSONRPCRequest request = new McpSchemaVO.JSONRPCRequest("2.0", "tools/call", "req-post-001",
                objectMapper.readValue(jsonStr, Map.class));

        // 执行请求处理
        Flux<McpSchemaVO.JSONRPCResponse> responseFlux = toolsCallHandler.handle("gateway_001", request);

        // 阻塞获取响应结果（单元测试必需）
        McpSchemaVO.JSONRPCResponse result = responseFlux.blockFirst();

        // 打印格式化响应结果
        log.info("测试结果(post): \n{}", JSON.toJSONString(result, true));
    }

    /**
     * 测试GET类型工具调用
     *
     * @throws JsonProcessingException JSON解析异常
     */
    @Test
    public void test_get() throws JsonProcessingException {
        // 模拟工具调用请求参数
        String jsonStr = """
                {
                  "name": "JavaSDKMCPClient_queryAiClientById",
                  "arguments": {
                    "req": { "id": 10001 }
                  }
                }
                """;

        // 构建JSON-RPC请求对象并执行处理
        Flux<McpSchemaVO.JSONRPCResponse> responseFlux = toolsCallHandler.handle("gateway_002",
                new McpSchemaVO.JSONRPCRequest("2.0", "tools/call", "req-get-001", objectMapper.readValue(jsonStr,
                        Map.class)));

        // 阻塞获取响应结果
        McpSchemaVO.JSONRPCResponse result = responseFlux.blockFirst();

        // 打印并校验响应结果
        log.info("测试结果(get): \n{}", JSON.toJSONString(result, true));
        assert result != null;
        assert result.error() == null;
    }
}