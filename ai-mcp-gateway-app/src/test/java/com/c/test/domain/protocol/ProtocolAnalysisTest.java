package com.c.test.domain.protocol;

import com.alibaba.fastjson.JSON;
import com.c.domain.protocol.model.entity.AnalysisCommandEntity;
import com.c.domain.protocol.model.valobj.enums.AnalysisTypeEnum;
import com.c.domain.protocol.model.valobj.http.HTTPProtocolVO;
import com.c.domain.protocol.service.ProtocolAnalysis;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.FileCopyUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * Swagger/OpenAPI 协议解析功能测试类，用于验证协议解析服务的正确性与完整性
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class ProtocolAnalysisTest {

    /** 测试用Swagger接口文档JSON资源 */
    @Value("classpath:swagger/api-docs-test03.json")
    private Resource apiDocs;

    /** 协议解析核心服务 */
    @Autowired
    private ProtocolAnalysis protocolAnalysis;

    /**
     * 解析Swagger文档并构建HTTP协议映射VO对象
     *
     * @throws Exception 文件读取、数据流操作、解析过程可能抛出异常
     */
    @Test
    public void parseSwaggerAndBuildHTTPProtocolVO() throws Exception {
        // 读取资源文件中的Swagger JSON字符串
        String json = new String(FileCopyUtils.copyToByteArray(apiDocs.getInputStream()), StandardCharsets.UTF_8);
        // 设置需要解析的接口路径
        List<String> endpoints = Arrays.asList("/api/v1/mcp/get_company_employee");
//        List<String> endpoints = Arrays.asList("/api/v1/mcp/query-test03");
//        List<String> endpoints = Arrays.asList("/api/v1/mcp/query-test02");
//        List<String> endpoints = Arrays.asList("/api/v1/mcp/query-by-id-01");
//        List<String> endpoints = Arrays.asList("/api/v1/mcp/query-by-id-02");
//        List<String> endpoints = Arrays.asList("/api/v1/mcp/query-by-id-03");

        // 构建解析命令实体
        AnalysisCommandEntity commandEntity = AnalysisCommandEntity
                .builder()
                .openApiJson(json)
                .endpoints(endpoints)
                .type(AnalysisTypeEnum.SWAGGER)
                .build();
        // 执行解析
        List<HTTPProtocolVO> result = protocolAnalysis.doAnalysis(commandEntity);
        // 输出解析结果
        log.info("测试结果:{}", JSON.toJSONString(result));
    }

}