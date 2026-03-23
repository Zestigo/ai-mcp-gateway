package com.c.infrastructure.dao.po;

import lombok.Getter;

import java.time.Instant;

@Getter
public class McpSession {

    private final String sessionId;
    private final String gatewayId;
    private Instant lastAccessTime;
    private boolean active = true;

    public McpSession(String sessionId, String gatewayId) {
        this.sessionId = sessionId;
        this.gatewayId = gatewayId;
        this.lastAccessTime = Instant.now();
    }

    public void touch() {
        this.lastAccessTime = Instant.now();
    }

    public void deactivate() {
        this.active = false;
    }
}