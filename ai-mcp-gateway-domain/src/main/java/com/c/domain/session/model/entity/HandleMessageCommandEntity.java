package com.c.domain.session.model.entity;

import com.c.domain.session.model.valobj.McpSchemaVO.JSONRPCMessage;
import com.c.domain.session.model.valobj.McpSchemaVO;
import lombok.*;
import org.springframework.util.Assert;

/**
 * 消息处理命令实体
 * 适配纯函数式折叠（Fold）结构的 MCP 协议消息
 * 用于封装 MCP 消息处理所需的全部上下文参数
 *
 * @author cyh
 * @date 2026/03/28
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HandleMessageCommandEntity {

    /** 网关唯一标识 - 用于定位路由配置 */
    private String gatewayId;

    /** 接口访问密钥 - 用于鉴权与限流上下文 */
    private String apiKey;

    /** 会话唯一标识 - 用于关联上下文 */
    private String sessionId;

    /**
     * 核心协议消息（不透明对象）
     * 外部必须通过 .fold() 方法进行分支处理
     */
    private JSONRPCMessage jsonrpcMessage;

    /**
     * 带参数的领域构造方法
     * 完成参数非空校验、消息体解析与对象初始化
     *
     * @param gatewayId   网关唯一标识
     * @param apiKey      接口访问密钥
     * @param sessionId   会话唯一标识
     * @param messageBody MCP协议消息原始字符串
     * @throws IllegalArgumentException 参数为空或消息解析失败时抛出
     */
    public HandleMessageCommandEntity(String gatewayId, String apiKey, String sessionId, String messageBody) {
        // 快速失败参数校验
        Assert.hasText(gatewayId, "gatewayId 不能为空");
        Assert.hasText(apiKey, "apiKey 不能为空");
        Assert.hasText(sessionId, "sessionId 不能为空");
        Assert.hasText(messageBody, "messageBody 不能为空");

        // 基础属性赋值
        this.gatewayId = gatewayId;
        this.apiKey = apiKey;
        this.sessionId = sessionId;

        try {
            // 解析字符串为JSONRPC消息对象
            this.jsonrpcMessage = McpSchemaVO.deserialize(messageBody);
        } catch (Exception e) {
            // 解析异常封装为业务异常抛出
            throw new IllegalArgumentException("MCP 消息解析失败: " + e.getMessage());
        }

        // 校验解析结果非空
        Assert.notNull(this.jsonrpcMessage, "解析结果不能为空");
    }
}