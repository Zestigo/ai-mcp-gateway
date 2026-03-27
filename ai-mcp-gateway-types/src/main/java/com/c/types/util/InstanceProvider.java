package com.c.types.util;

import cn.hutool.core.util.StrUtil;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;


/**
 * 分布式节点实例标识提供者
 * 优先级：配置中心 > 环境变量 > 自动获取
 */
@Slf4j
@Component
public class InstanceProvider {
    @Value("${mcp.node.ip:}")
    private String customIp;

    private String nodeIp;

    @PostConstruct
    public void init() {
        // 1. 优先使用手动配置 (Nacos/YML)
        if (StrUtil.isNotBlank(customIp)) {
            this.nodeIp = customIp;
        }
        // 2. 其次读取 K8s 环境变量
        else {
            String podIp = System.getenv("POD_IP");
            if (StrUtil.isNotBlank(podIp)) {
                this.nodeIp = podIp;
            }
            // 3. 最后使用 JDK 原生方法兜底
            else {
                this.nodeIp = getIpByJdk();
            }
        }
        log.info("MCP 节点寻址 IP 已确定: {}", this.nodeIp);
    }

    private String getIpByJdk() {
        try {
            // 注意：getLocalHost 在某些 Linux 环境下会直接返回 127.0.0.1
            // 如果追求极致稳健，实际生产中会遍历 NetworkInterface (如我之前给你的代码)
            return InetAddress
                    .getLocalHost()
                    .getHostAddress();
        } catch (UnknownHostException e) {
            log.error("本地 IP 获取异常", e);
            return "127.0.0.1";
        }
    }

    public String getHostIp() {
        return nodeIp;
    }
}