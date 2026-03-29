package com.c.test.domain.protocol;

import com.alibaba.fastjson.JSON;
import com.c.domain.protocol.model.entity.AnalysisCommandEntity;
import com.c.domain.protocol.model.entity.StorageCommandEntity;
import com.c.domain.protocol.model.valobj.enums.AnalysisTypeEnum;
import com.c.domain.protocol.model.valobj.http.HTTPProtocolVO;
import com.c.domain.protocol.service.ProtocolAnalysis;
import com.c.domain.protocol.service.ProtocolStorage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * 协议存储服务单元测试
 * 验证协议解析、数据持久化、事件发布整体流程
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class ProtocolStorageTest {

    /** 测试用Swagger接口文档资源 */
    @Value("classpath:swagger/api-docs-test03.json")
    private org.springframework.core.io.Resource apiDocs;

    /** 协议解析服务 */
    @Autowired
    private ProtocolAnalysis protocolAnalysis;

    /** 协议存储服务 */
    @Resource
    private ProtocolStorage protocolStorage;

    /**
     * 测试协议解析与持久化完整流程
     * 1. 读取Swagger JSON文件
     * 2. 解析指定接口生成协议配置
     * 3. 调用存储服务持久化协议数据
     *
     * @throws IOException 文件读取异常
     */
    @Test
    public void test_storage() throws IOException {
        // 读取Swagger接口文档JSON字符串
        String json = new String(FileCopyUtils.copyToByteArray(apiDocs.getInputStream()), StandardCharsets.UTF_8);

        // 配置需要解析的接口路径列表
        List<String> endpoints = Arrays.asList("/api/v1/mcp/get_company_employee");

        // 构建协议解析命令实体
        AnalysisCommandEntity commandEntity = AnalysisCommandEntity
                .builder()
                .openApiJson(json)
                .type(AnalysisTypeEnum.SWAGGER)
                .endpoints(endpoints)
                .build();

        // 执行协议解析，生成HTTP协议配置
        List<HTTPProtocolVO> httpProtocolVOS = protocolAnalysis.doAnalysis(commandEntity);
        log.info("解析协议:{}", JSON.toJSONString(httpProtocolVOS));

        // 执行协议数据持久化
        List<Long> protocolIdList = protocolStorage.doStorage(StorageCommandEntity
                .builder()
                .httpProtocolVOS(httpProtocolVOS)
                .build());

        // 输出持久化结果
        log.info("存储协议:{}", JSON.toJSONString(protocolIdList));
    }

}