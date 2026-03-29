package com.c.domain.protocol.service.analysis.parser;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * OpenAPI原始数据解析器
 * 将原始文本转换为标准JSONObject，完成版本识别与数据清洗
 *
 * @author cyh
 * @date 2026-03-28
 */
@Slf4j
@Component
public class OpenApiParser {

    /**
     * 解析原始OpenAPI内容，支持JSON格式
     *
     * @param rawContent 原始文档内容
     * @return 格式化后的JSONObject，解析失败返回空对象
     */
    public JSONObject parse(String rawContent) {
        // 空内容校验，直接返回空对象
        if (rawContent == null || rawContent.isBlank()) {
            log.warn("OpenAPI 解析内容为空");
            return new JSONObject();
        }

        try {
            // 去除首尾空白字符
            String trimmedContent = rawContent.trim();

            // 判断是否为标准JSON格式（{} / []）
            if (trimmedContent.startsWith("{") || trimmedContent.startsWith("[")) {
                // 解析为JSON对象
                JSONObject root = JSON.parseObject(trimmedContent);
                // 校验并清洗文档结构
                return validateAndClean(root);
            }

            // 非JSON格式直接拒绝解析
            log.error("检测到非 JSON 格式文档，目前仅支持标准的 OpenAPI JSON 格式");
            return new JSONObject();

        } catch (Exception e) {
            // 解析异常捕获，避免流程中断
            log.error("OpenAPI 原始数据解析失败，非法的内容格式: {}", e.getMessage());
            return new JSONObject();
        }
    }

    /**
     * 校验并清洗OpenAPI根节点数据
     *
     * @param root 原始根节点对象
     * @return 清洗后的根节点对象
     */
    private JSONObject validateAndClean(JSONObject root) {
        // 空对象防护
        if (root == null) return new JSONObject();

        // 兼容获取版本号：优先openapi，其次swagger
        String version = root.getString("openapi");
        if (version == null) {
            version = root.getString("swagger");
        }

        log.info("成功加载 OpenAPI 文档，版本标识: {}", version != null ? version : "Unknown");

        // 校验核心节点paths是否存在，缺失则告警
        if (!root.containsKey("paths")) {
            log.warn("解析的文档中不包含任何 'paths' 定义，请检查来源数据是否完整");
        }

        return root;
    }
}