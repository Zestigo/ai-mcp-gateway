package com.c.test.demo;

import com.c.api.common.Response;
import com.c.api.model.request.GatewayConfigRequestDTO;
import com.c.api.model.response.GatewayConfigResponseDTO;
import com.c.api.service.AdminOpenService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import jakarta.annotation.Resource;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * 网关后台开放服务集成测试
 * 覆盖基础配置、工具配置、协议配置及认证配置
 */
@SpringBootTest
public class AdminOpenServiceTest {

    @Resource
    private AdminOpenService adminOpenService;


}