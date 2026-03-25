package com.c.test.message;

import com.alibaba.fastjson.JSON;
import com.c.domain.session.model.valobj.McpSchemaVO;
import com.c.domain.session.service.message.handler.IRequestHandler;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;

/**
 * MCP 工具列表处理器测试
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class RequestHandlerTest {

    @Resource(name = "toolsListHandler")
    private IRequestHandler toolsListHandler;

    @Test
    public void test_handle() {
        // 1. 构建标准的 MCP JSON-RPC 请求对象
        // 注意：方法名应为 "tools/list"，params 通常是一个 Map 或者特定的 VO，这里传入空 Map
        McpSchemaVO.JSONRPCRequest request = new McpSchemaVO.JSONRPCRequest("2.0", "tools/list", "a355a5f7-0",
                Map.of());

        // 2. 调用处理器获取 Flux 流
        var responseFlux = toolsListHandler.handle("gateway_001", request);

        // 3. 正确的测试姿势 A：同步阻塞获取结果（便于查看 Log）
        McpSchemaVO.JSONRPCResponse result = responseFlux.blockFirst(); // 阻塞获取第一条响应

        if (result != null) {
            log.info("测试结果 (JSON): {}", JSON.toJSONString(result));
        } else {
            log.error("未获取到响应结果");
        }
    }
}