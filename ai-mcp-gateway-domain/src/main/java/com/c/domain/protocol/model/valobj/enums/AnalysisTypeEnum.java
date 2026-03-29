package com.c.domain.protocol.model.valobj.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 协议解析类型枚举
 * 定义系统支持的协议解析来源类型，用于区分不同协议解析策略
 *
 * @author cyh
 * @date 2026/03/28
 */
@Getter
@AllArgsConstructor
public enum AnalysisTypeEnum {

    /** Swagger/OpenAPI协议解析类型 */
    SWAGGER("swagger", "Swagger/OpenAPI解析");

    /** 类型编码 */
    private final String code;
    /** 类型描述信息 */
    private final String info;

    /**
     * Swagger解析动作子枚举
     * 定义Swagger解析的具体动作类型，对应策略Bean名称
     */
    @Getter
    @AllArgsConstructor
    public enum SwaggerAnalysisAction {

        /** 请求体解析策略 */
        REQUEST_BODY_ANALYSIS("requestBodyAnalysis", "解析请求体对象"),

        /** 路径与查询参数解析策略 */
        PARAMETERS_ANALYSIS("parametersAnalysis", "解析路径与查询属性"),

        /** 响应体解析策略 */
        RESPONSE_ANALYSIS("responseAnalysis", "解析响应体结果数据");

        /** 动作编码 */
        private final String code;
        /** 动作描述 */
        private final String info;
    }
}