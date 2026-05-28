package com.geek.server.service;

import com.geek.server.domain.vo.ApiRequestVO;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

/**
 * API-服务层
 */
public interface IApiService {

    /**
     * 单词查询号码
     */
    Map<String, Object> single(ApiRequestVO apiRequestVO) throws UnsupportedEncodingException;
}
