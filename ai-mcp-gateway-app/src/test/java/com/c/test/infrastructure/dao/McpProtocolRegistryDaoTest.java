package com.c.test.infrastructure.dao;

import com.c.infrastructure.dao.McpProtocolHttpDao;
import lombok.extern.slf4j.Slf4j;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional
public class McpProtocolRegistryDaoTest {

    @Resource
    private McpProtocolHttpDao mcpProtocolRegistryDao;

}