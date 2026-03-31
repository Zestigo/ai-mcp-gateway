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

}