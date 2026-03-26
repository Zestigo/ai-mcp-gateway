package com.c.test.domain.session.message;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.c.domain.session.model.valobj.McpSchemaVO;
import com.c.domain.session.service.message.handler.IRequestHandler;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.core.publisher.Flux;

import java.util.HashMap;

/**
 * MCP工具列表查询处理器单元测试
 * 验证工具列表查询、响应数据返回逻辑
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class ToolsListHandlerTest {

    /** 工具列表查询处理器 */
    @Resource(name = "toolsListHandler")
    private IRequestHandler toolsListHandler;

    /**
     * 测试工具列表查询功能
     */
    @Test
    public void test_handle() {
        // 构建工具列表查询请求
        McpSchemaVO.JSONRPCRequest request = new McpSchemaVO.JSONRPCRequest("2.0", "tools/list", "test-req-id-001",
                new HashMap<>());

        // 执行请求处理
        Flux<McpSchemaVO.JSONRPCResponse> responseFlux = toolsListHandler.handle("gateway_001", request);

        // 阻塞获取响应结果（单元测试必需）
        McpSchemaVO.JSONRPCResponse response = responseFlux.blockFirst();

        // 打印并校验响应结果
        if (response != null) {
            log.info("测试结果: \n{}", JSON.toJSONString(response, SerializerFeature.PrettyFormat));
            log.info("工具列表详情: {}", response.result());
        } else {
            log.warn("未获取到响应结果，请检查网关 gateway_001 是否有配置工具数据");
        }
    }
}