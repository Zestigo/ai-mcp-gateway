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

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class ApiTest {

    /** AI聊天客户端构建器 */
    @Resource
    private ChatClient.Builder chatClientBuilder;

    /** 服务完整地址 */
    @Value("${service.full-url}")
    private String serviceFullUrl;

    /** 应用上下文路径 */
    @Value("${server.servlet.context-path:}")
    private String contextPath;

    /**
     * 测试MCP工具调用能力，通过ChatClient调用MCP客户端提供的工具
     */
    @Test
    public void test_mcp() {
        // 构建MCP同步客户端
        McpSyncClient mcpClient = sseMcpClient02();

        // 构建ChatClient，注入MCP工具回调
        ChatClient chatClient = chatClientBuilder
                .defaultOptions(OpenAiChatOptions
                        .builder()
                        .toolCallbacks(new SyncMcpToolCallbackProvider(mcpClient).getToolCallbacks())
                        .build())
                .build();

        // 发送测试指令，查询可用工具列表
        log.info("测试结果:{}", chatClient
                .prompt("帮我搜索一下 2026 年 3 月最新的 AI 大模型新闻。")
                .call()
                .content());
    }

    /**
     * 构建百度AI搜索MCP同步客户端（SSE传输）
     * 文档地址：https://sai.baidu.com/zh/detail/e014c6ffd555697deabf00d058baf388
     * API Key申请：https://console.bce.baidu.com/iam/?_=1753597622044#/iam/apikey/list
     *
     * @return 百度MCP同步客户端实例
     */
    public McpSyncClient sseMcpClient01() {
        // 从环境变量获取API Key
        String apiKey = System.getenv("BAIDU_API_KEY");
        if (apiKey == null) {
            apiKey = "bce-v3/ALTAK-IfztT89f0wQhg5qXYWBKC/af5bec8877b2402a44718e1a9946b1683418ca41";
        }

        // 构建百度MCP的SSE传输层，指定地址和带API Key的端点
        HttpClientSseClientTransport sseClientTransport = HttpClientSseClientTransport
                .builder("http://appbuilder.baidu.com")
                .sseEndpoint("/v2/ai_search/mcp/sse?api_key=" + apiKey)
                .build();

        // 构建同步客户端，设置36000分钟请求超时
        McpSyncClient mcpSyncClient = McpClient
                .sync(sseClientTransport)
                .requestTimeout(Duration.ofMinutes(36000))
                .build();
        // 初始化MCP客户端并打印日志
        var init_sse = mcpSyncClient.initialize();
        log.info("Tool SSE MCP01 Initialized {}", init_sse);

        return mcpSyncClient;
    }

    /**
     * 构建本地MCP同步客户端（SSE传输），提供自定义工具能力
     *
     * @return 本地MCP同步客户端实例
     */
    public McpSyncClient sseMcpClient02() {
        // 构建完整的 SSE 端点 URL
        String sseEndpoint = "/api/v1/gateways/test001/sse";
        String fullEndpoint = contextPath + sseEndpoint;

        HttpClientSseClientTransport sseClientTransport = HttpClientSseClientTransport
                .builder(serviceFullUrl)
                .sseEndpoint(sseEndpoint)
                .build();

        McpSyncClient mcpSyncClient = McpClient
                .sync(sseClientTransport)
                .requestTimeout(Duration.ofMinutes(5))
                .build();

        var init = mcpSyncClient.initialize();
        log.info("本地工具端 Initialized {}", init);

        return mcpSyncClient;
    }
}