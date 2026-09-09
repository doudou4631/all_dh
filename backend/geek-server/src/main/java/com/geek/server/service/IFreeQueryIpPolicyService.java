package com.geek.server.service;

import com.geek.server.domain.FreeQueryIpPolicy;

import java.util.concurrent.locks.ReentrantLock;

public interface IFreeQueryIpPolicyService {
    FreeQueryIpPolicy getConfig();

    int updateConfig(FreeQueryIpPolicy config);

    ReentrantLock lockFor(String ipAddr);

    String validateAccess(String ipAddr);

    void waitForConfiguredDelay(long requestStartedAt);
}
