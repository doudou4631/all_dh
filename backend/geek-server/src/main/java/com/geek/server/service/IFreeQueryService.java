package com.geek.server.service;

import java.util.Map;

public interface IFreeQueryService {
    Map<String, Object> query(String phoneNumber);
}
