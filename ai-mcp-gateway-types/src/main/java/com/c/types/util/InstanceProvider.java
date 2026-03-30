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
 * 提供服务节点IP地址获取能力，支持多优先级寻址策略
 *
 * @author cyh
 * @date 2026/03/30
 */
@Slf4j
@Component
public class InstanceProvider {

    /** 配置文件自定义节点IP，无配置时默认为空字符串 */
    @Value("${mcp.node.ip:}")
    private String customIp;

    /** 最终确定的服务节点IP地址 */
    private String nodeIp;

    /**
     * Bean初始化后执行IP寻址逻辑
     * 按照配置中心 > 环境变量 > 自动获取的优先级确定节点IP
     */
    @PostConstruct
    public void init() {
        // 优先级1：使用手动配置的IP，来源于Nacos或YML配置文件
        if (StrUtil.isNotBlank(customIp)) {
            this.nodeIp = customIp;
        }
        // 优先级2：读取K8s环境变量中的POD_IP
        else {
            String podIp = System.getenv("POD_IP");
            if (StrUtil.isNotBlank(podIp)) {
                this.nodeIp = podIp;
            }
            // 优先级3：调用JDK原生方法获取本地IP作为兜底方案
            else {
                this.nodeIp = getIpByJdk();
            }
        }
        log.info("MCP 节点寻址 IP 已确定: {}", this.nodeIp);
    }

    /**
     * 通过JDK原生API获取本地IP地址
     *
     * @return 本机IP地址，获取失败时返回127.0.0.1
     */
    private String getIpByJdk() {
        try {
            // 注意：getLocalHost 在部分Linux环境下可能返回127.0.0.1
            // 生产环境如需高稳健性，可遍历网卡接口获取真实IP
            return InetAddress
                    .getLocalHost()
                    .getHostAddress();
        } catch (UnknownHostException e) {
            log.error("本地 IP 获取异常", e);
            return "127.0.0.1";
        }
    }

    /**
     * 获取最终确定的服务节点IP地址
     *
     * @return 节点IP地址
     */
    public String getHostIp() {
        return nodeIp;
    }
}