package com.c.test;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.openai.OpenAiChatOptions;
import lombok.extern.slf4j.Slf4j;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import jakarta.annotation.Resource;
import org.junit.Test;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;

/**
 * MCP 客户端功能测试类
 * 用于验证 MCP 网关连接、工具发现、AI 集成调用等核心功能
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class ApiTest {

    /** AI 对话客户端构建器 */
    @Resource
    private ChatClient.Builder chatClientBuilder;

    /** 服务对外完整访问地址 */
    @Value("${service.full-url}")
    private String serviceFullUrl;

    /** 应用上下文路径，用于拼接接口地址 */
    @Value("${server.servlet.context-path:}")
    private String contextPath;

    /**
     * 测试 AI + MCP 工具调用流程
     * 连接本地 MCP 网关，加载工具，并通过 AI 对话查询可用工具
     */
    @Test
    public void test_mcp() {
        // 创建本地 MCP 同步客户端，建立网关连接
        McpSyncClient mcpClient = sseMcpClient02();

        // 构建 AI 对话客户端，注入 MCP 工具回调
        ChatClient chatClient = chatClientBuilder
                .defaultOptions(OpenAiChatOptions
                        .builder()
                        .toolCallbacks(new SyncMcpToolCallbackProvider(mcpClient).getToolCallbacks())
                        .build())
                .build();

        // 发送测试指令，验证工具发现与调用能力
        log.info("测试结果:{}", chatClient
                .prompt("你有什么工具可以使用")
                .call()
                .content());
    }

    /**
     * 创建连接百度智能云 AI Search 的 MCP 客户端
     * 使用 SSE 长连接传输协议，对接第三方 MCP 服务
     *
     * @return 百度 MCP 同步客户端
     */
    public McpSyncClient sseMcpClient01() {
        // 从系统环境变量读取百度 API Key，不存在则使用默认值
        String apiKey = System.getenv("BAIDU_API_KEY");
        if (apiKey == null) {
            apiKey = "bce-v3/ALTAK-IfztT89f0wQhg5qXYWBKC/af5bec8877b2402a44718e1a9946b1683418ca41";
        }

        // 构建百度 MCP 服务的 SSE 传输客户端
        HttpClientSseClientTransport sseClientTransport = HttpClientSseClientTransport
                .builder("http://appbuilder.baidu.com")
                .sseEndpoint("/v2/ai_search/mcp/sse?api_key=" + apiKey)
                .build();

        // 构建 MCP 同步客户端，设置超长超时时间
        McpSyncClient mcpSyncClient = McpClient
                .sync(sseClientTransport)
                .requestTimeout(Duration.ofMinutes(36000))
                .build();

        // 初始化 MCP 连接，完成握手与工具发现
        var init_sse = mcpSyncClient.initialize();
        log.info("Tool SSE MCP01 Initialized {}", init_sse);

        return mcpSyncClient;
    }

    /**
     * 创建连接本地 MCP 网关服务的客户端
     * 用于测试自定义网关、自定义工具的注册与调用
     *
     * @return 本地 MCP 同步客户端
     */
    public McpSyncClient sseMcpClient02() {
        // 本地 MCP 网关 SSE 连接地址
        String sseEndpoint = "/api/v1/gateways/gateway_001/sse";
        String fullEndpoint = contextPath + sseEndpoint;

        // 构建本地服务的 SSE 传输客户端
        HttpClientSseClientTransport sseClientTransport = HttpClientSseClientTransport
                .builder(serviceFullUrl)
                .sseEndpoint(fullEndpoint)
                .build();

        // 构建 MCP 同步客户端
        McpSyncClient mcpSyncClient = McpClient
                .sync(sseClientTransport)
                .requestTimeout(Duration.ofMinutes(5))
                .build();

        // 初始化本地 MCP 网关连接
        var init = mcpSyncClient.initialize();
        log.info("本地工具端 Initialized {}", init);

        return mcpSyncClient;
    }
}