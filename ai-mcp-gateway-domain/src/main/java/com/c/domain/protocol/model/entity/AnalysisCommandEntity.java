package com.c.domain.protocol.model.entity;

import com.c.domain.protocol.model.valobj.enums.AnalysisTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 协议解析命令实体
 * 承载从外部导入的原始协议数据及解析指令，作为协议解析流程的入参载体
 *
 * @author cyh
 * @date 2026/03/28
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisCommandEntity {

    /** 解析类型枚举 */
    private AnalysisTypeEnum type;

    /** 原始OpenAPI/Swagger JSON字符串内容 */
    private String openApiJson;

    /** 待解析的接口端点路径列表，为空时按业务规则处理 */
    private List<String> endpoints;

    /**
     * 校验解析命令的合法性
     *
     * @return 合法返回true，不合法返回false
     */
    public boolean isValid() {
        return type != null && StringUtils.hasText(openApiJson) && !CollectionUtils.isEmpty(endpoints);
    }

    /**
     * 校验命令合法性，不合法则抛出异常
     *
     * @throws IllegalArgumentException 解析类型、OpenAPI内容、端点列表任一为空时抛出
     */
    public void validate() {
        if (type == null) {
            throw new IllegalArgumentException("解析类型不能为空");
        }
        if (!StringUtils.hasText(openApiJson)) {
            throw new IllegalArgumentException("OpenApiJson 内容不能为空");
        }
        if (CollectionUtils.isEmpty(endpoints)) {
            throw new IllegalArgumentException("待解析的 Endpoints 列表不能为空");
        }
    }
}