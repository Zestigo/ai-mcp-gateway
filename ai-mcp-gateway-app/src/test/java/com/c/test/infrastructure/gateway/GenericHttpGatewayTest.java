package com.c.test.infrastructure.gateway;

import com.alibaba.fastjson.JSON;
import com.c.infrastructure.dao.McpProtocolRegistryDao;
import com.c.infrastructure.dao.po.McpProtocolRegistryPO;
import com.c.infrastructure.gateway.GenericHttpGateway;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 通用HTTP网关功能测试类
 * 验证GenericHttpGateway的POST请求调用能力
 *
 * @author cyh
 * @date 2026/03/25
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Transactional
public class GenericHttpGatewayTest {

    /** 通用HTTP请求网关 */
    @Resource
    private GenericHttpGateway gateway;

    /** MCP协议注册数据访问对象 */
    @javax.annotation.Resource
    private McpProtocolRegistryDao mcpProtocolRegistryDao;

    /**
     * 测试POST请求调用
     * 从数据库加载配置，构造请求并执行HTTP调用
     *
     * @throws Exception 请求执行异常
     */
    @Test
    public void test_post() throws Exception {
        // 从数据库查询MCP协议注册配置信息
        McpProtocolRegistryPO mcpProtocolRegistryPO = mcpProtocolRegistryDao.queryById(1L);

        // 获取请求地址、请求头、超时时间配置
        String httpUrl = mcpProtocolRegistryPO.getHttpUrl();
        String httpHeaders = mcpProtocolRegistryPO.getHttpHeaders();
        Integer timeout = mcpProtocolRegistryPO.getTimeout();

        // 构造业务请求参数
        Map<String, Object> params = new java.util.HashMap<>();
        params.put("city", "beijing");

        Map<String, Object> company = new java.util.HashMap<>();
        company.put("name", "alibaba");
        company.put("type", "internet");
        params.put("company", company);

        // 构造HTTP请求头
        Map<String, Object> headers = new java.util.HashMap<>();
        headers.put("Content-Type", "application/json");

        // 构建JSON格式请求体
        RequestBody requestBody = RequestBody.create(JSON.toJSONString(params), MediaType.parse("application/json"));

        // 执行POST请求并获取响应结果
        retrofit2.Call<ResponseBody> call = gateway.post(httpUrl, headers, requestBody);
        ResponseBody responseBody = call
                .execute()
                .body();

        // 输出响应结果
        log.info("测试结果：{}", responseBody != null ? responseBody.string() : null);
    }

}