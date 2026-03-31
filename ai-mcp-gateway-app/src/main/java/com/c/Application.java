package com.c;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 应用程序启动类
 * 作为Spring Boot应用的入口点，负责初始化和启动整个网关服务
 * 
 * @author cyh
 * @date 2026/03/31
 */
@EnableAspectJAutoProxy(exposeProxy = true)
@SpringBootApplication
public class Application {
    /**
     * 应用程序主方法
     * 启动Spring Boot应用，初始化容器并加载所有配置
     * 
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}