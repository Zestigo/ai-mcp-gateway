package com.c.test.trigger;

import com.c.api.model.request.GatewayConfigRequestDTO;
import com.c.cases.admin.AdminAuthService;
import com.c.cases.admin.AdminGatewayService;
import com.c.cases.admin.AdminManageService;
import com.c.cases.admin.AdminProtocolService;
import com.c.domain.admin.model.entity.GatewayConfigEntity;
import com.c.trigger.http.AdminOpenController;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminOpenControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AdminGatewayService adminGatewayService;
    @Mock
    private AdminAuthService adminAuthService;
    @Mock
    private AdminProtocolService adminProtocolService;
    @Mock
    private AdminManageService adminManageService;

    @InjectMocks
    private AdminOpenController adminOpenController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(adminOpenController).build();
    }

    @Test
    @DisplayName("测试保存网关基础配置 - 成功流")
    void testSaveGatewayConfig_Success() throws Exception {
        // 准备数据
        GatewayConfigRequestDTO.GatewayConfig request = new GatewayConfigRequestDTO.GatewayConfig();
        request.setGatewayId("GW_001");
        request.setGatewayName("测试网关");
        request.setAuth(1); // 假设 1 是已开启
        request.setStatus(1);

        // 执行请求
        mockMvc.perform(post("/admin/save_gateway_config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0000")) // 假设 success 是 0000
                .andExpect(jsonPath("$.data.info").value(true));

        // 验证 Service 是否被调用
        verify(adminGatewayService, times(1)).saveGatewayConfig(any());
    }

    @Test
    @DisplayName("测试保存网关认证配置 - 成功流")
    void testSaveGatewayAuth_Success() throws Exception {
        GatewayConfigRequestDTO.GatewayAuth authRequest = new GatewayConfigRequestDTO.GatewayAuth();
        authRequest.setGatewayId("GW_001");
        authRequest.setRateLimit(100);
        Date futureDate = new Date(System.currentTimeMillis() + 86400000L); // 明天的现在
        authRequest.setExpireTime(futureDate);

        mockMvc.perform(post("/admin/save_gateway_auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0000"));

        verify(adminAuthService).saveGatewayAuth(any());
    }

    @Test
    @DisplayName("测试查询网关列表 - 返回数据转换")
    void testQueryGatewayConfigList_Success() throws Exception {
        // Mock 领域层返回的数据
        GatewayConfigEntity entity = new GatewayConfigEntity();
        entity.setGatewayId("GW_001");
        entity.setGatewayName("Gate-A");
        
        when(adminManageService.queryGatewayConfigList()).thenReturn(Collections.singletonList(entity));

        mockMvc.perform(get("/admin/query_gateway_config_list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].gatewayId").value("GW_001"))
                .andExpect(jsonPath("$.data[0].gatewayName").value("Gate-A"));
    }

    @Test
    @DisplayName("测试保存配置时发生异常 - 异常捕获流")
    void testSaveGatewayConfig_Exception() throws Exception {
        // 让 Service 抛出异常
        doThrow(new RuntimeException("DB Error")).when(adminGatewayService).saveGatewayConfig(any());

        GatewayConfigRequestDTO.GatewayConfig request = new GatewayConfigRequestDTO.GatewayConfig();
        request.setGatewayId("GW_ERR");

        mockMvc.perform(post("/admin/save_gateway_config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk()) // Controller 捕获了异常并返回 Response.fail
                .andExpect(jsonPath("$.code").value("0001")); // 假设 UN_ERROR 是 0001
    }
}