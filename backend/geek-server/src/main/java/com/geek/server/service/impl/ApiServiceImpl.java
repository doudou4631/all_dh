package com.geek.server.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geek.common.core.domain.entity.SysUser;
import com.geek.common.utils.http.HttpUtils;
import com.geek.common.utils.sign.Md5Utils;
import com.geek.common.utils.SecurityUtils;
import com.geek.server.domain.UserAggregateConfig;
import com.geek.server.domain.UserApiQueryRecord;
import com.geek.server.domain.UserPointRecord;
import com.geek.server.domain.vo.ApiRequestVO;
import com.geek.server.domain.vo.PreActionConfig;
import com.geek.server.domain.vo.UserInfo360VO;
import com.geek.server.service.IApiService;
import com.geek.server.service.IUserAggregateConfigService;
import com.geek.server.service.IUserApiQueryRecordService;
import com.geek.server.service.IUserPointRecordService;
import com.geek.system.service.ISysUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.util.*;


/**
 * API-服务层实现类
 */
@Service
public class ApiServiceImpl implements IApiService {

    private static final Logger log = LoggerFactory.getLogger(ApiServiceImpl.class);

    @Autowired
    private IUserAggregateConfigService userAggregateConfigService;

    @Autowired
    private IUserApiQueryRecordService userApiQueryRecordService;

    @Autowired
    private IUserPointRecordService userPointRecordService;

    @Autowired
    private ISysUserService sysUserService;

    @Override
    public Map<String, Object> single(ApiRequestVO apiRequestVO) throws UnsupportedEncodingException {
        if (apiRequestVO == null) {
            return null;
        }
        // 验证必要参数
        if (apiRequestVO.getUrl() == null || apiRequestVO.getPhoneNumber() == null || apiRequestVO.getPreActionType() == null) {
            return Map.of("success", false, "message", "缺少必要参数");
        }

        // 获取当前用户ID
        Long currentUserId = SecurityUtils.getUserId();
        String currentUserName = SecurityUtils.getUsername();

        // 获取当前用户积分
        SysUser sysUser = sysUserService.selectUserById(currentUserId);
        if (sysUser.getPoints() < 1) {
            return Map.of("success", false, "message", "用户积分不足");
        }

        // 创建API查询记录对象
        UserApiQueryRecord apiQueryRecord = new UserApiQueryRecord();
        apiQueryRecord.setQueryType(apiRequestVO.getQueryType() != null ? apiRequestVO.getQueryType() : "2"); // 2表示单条查询
        apiQueryRecord.setPlatformId(apiRequestVO.getPlatformId() != null ? Long.valueOf(apiRequestVO.getPlatformId()) : null);
        apiQueryRecord.setPlatformName(apiRequestVO.getPlatformName());
        apiQueryRecord.setUserId(currentUserId);
        apiQueryRecord.setPhone(apiRequestVO.getPhoneNumber());
        apiQueryRecord.setCreateBy(currentUserName);
        apiQueryRecord.setCreateTime(new Date());
        apiQueryRecord.setTaskId(apiRequestVO.getTaskId());

        // 记录请求开始时间
        long startTime = System.currentTimeMillis();
        // 构建请求参数
        Map<String, Object> requestParams = new HashMap<>();
        String phoneNumber = apiRequestVO.getPhoneNumber();
        requestParams.put("phone", phoneNumber);

        // 构建请求头
        Map<String, String> headers = new HashMap<>();

        // 判断是否存在前置操作
        if (apiRequestVO.getPreActionType() == 1) {
            // 执行获取Token操作
            Map<String, Object> tokenResult = executePreAction(apiRequestVO.getPreActionConfig());
            if (!tokenResult.get("success").equals(true)) {
                return tokenResult;
            }
            // 将token添加到请求头中，而不是请求参数中
            headers.put("Authorization", "" + tokenResult.get("token"));

            // 调用360平台默认传递的platforms参数
            requestParams.put("platforms", "4");
        }else{
            UserAggregateConfig userAggregateConfig = userAggregateConfigService.selectUserAggregateConfigById(1L);

            String skey = userAggregateConfig.getsKey();
            String sid = userAggregateConfig.getSid();

            String sign = Md5Utils.encryptMd5(sid+phoneNumber+skey);
            requestParams.put("sj", phoneNumber);
            requestParams.put("uid", sid);
            requestParams.put("sign", sign);
        }

        // 如果有默认请求头模板，解析并添加到headers中
        if (apiRequestVO.getHeadersTemplate() != null && !apiRequestVO.getHeadersTemplate().isEmpty()) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, String> templateHeaders = objectMapper.readValue(apiRequestVO.getHeadersTemplate(), Map.class);
                headers.putAll(templateHeaders);
            } catch (Exception e) {
                System.err.println("解析请求头模板失败: " + e.getMessage());
            }
        }
        apiQueryRecord.setRequestParams(requestParams.toString());
        // 调用HttpUtils.postJson()方法发送请求，支持自定义headers
        String response = null;
        try {
            // 如果有headers，则使用支持headers的postJson方法
            if (!headers.isEmpty()) {
                response = HttpUtils.postJson(
                        apiRequestVO.getUrl(),
                        requestParams,
                        headers
                );
            } else {
                // 如果没有headers，使用原来的postCall方法
                response = HttpUtils.postCall(
                        apiRequestVO.getUrl(),
                        requestParams
                );
            }
        } catch (Exception e) {
            // 记录详细的异常信息
            System.err.println("HTTP请求异常: " + e.getMessage());

            // 记录HTTP请求异常到数据库
            apiQueryRecord.setRequestStatus("1"); // 1表示失败
            apiQueryRecord.setRequestTime(0L); // 请求失败时设置为0
            apiQueryRecord.setResponseResult("HTTP请求异常: " + e.getMessage());
            userApiQueryRecordService.insertUserApiQueryRecord(apiQueryRecord);

            return Map.of("success", false, "message", "请求异常: " + e.getMessage());
        }

        // 记录请求结束时间和耗时
        long endTime = System.currentTimeMillis();
        long requestTime = endTime - startTime;
        apiQueryRecord.setRequestTime(requestTime);

        // 解析响应结果
        if (response != null) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);

                // 1. 处理data字段：保持原有逻辑，不判断status
                if (responseMap.containsKey("data")) {
                    Object dataObj = responseMap.get("data");
                    if (dataObj instanceof Map) {
                        Map<String, Object> dataMap = (Map<String, Object>) dataObj;
                        Map<String, Object> result = parseInnerResponse(dataMap, "default");

                        // 记录API查询结果
                        recordApiQueryResult(apiQueryRecord, result, response);

                        // 记录查询结果日志（成功和失败都记录）
                        if (Boolean.TRUE.equals(result.get("success"))) {
                            // 如果查询成功，记录积分扣减
                            recordPointDeduction(currentUserId, apiRequestVO.getPlatformName());
                            log.info("API查询成功 - 用户ID: {}, 平台: {}", currentUserId, apiRequestVO.getPlatformName());
                        } else {
                            // 如果查询失败，记录失败日志
                            String errorMessage = (String) result.getOrDefault("message", "未知错误");
                            log.warn("API查询失败 - 用户ID: {}, 平台: {}, 错误信息: {}", currentUserId, apiRequestVO.getPlatformName(), errorMessage);
                        }

                        return result;
                    } else {
                        // 记录失败的API查询
                        apiQueryRecord.setRequestStatus("1"); // 1表示失败
                        apiQueryRecord.setRequestTime(0L); // 请求失败时设置为0
                        apiQueryRecord.setResponseResult("data字段不是Map类型，无法解析");
                        userApiQueryRecordService.insertUserApiQueryRecord(apiQueryRecord);

                        return Map.of("success", false, "message", "data字段不是Map类型，无法解析");
                    }
                }
                // 2. 处理results字段：仅此处判断status==success
                else if (responseMap.containsKey("results")) {
                    // 先判断外层status是否为success（仅results字段需要）
                    String outerStatus = (String) responseMap.get("status");
                    if ("success".equals(outerStatus)) {
                        Object resultsObj = responseMap.get("results");
                        if (resultsObj instanceof List) {
                            List<Object> resultsList = (List<Object>) resultsObj;
                            if (!resultsList.isEmpty()) {
                                Object firstResult = resultsList.get(0);
                                if (firstResult instanceof Map) {
                                    Map<String, Object> firstResultMap = (Map<String, Object>) firstResult;
                                    Map<String, Object> result = parseInnerResponse(firstResultMap, "default");

                                    // 记录API查询结果
                                    recordApiQueryResult(apiQueryRecord, result, response);

                                    // 记录查询结果日志（成功和失败都记录）
                                    if (Boolean.TRUE.equals(result.get("success"))) {
                                        // 如果查询成功，记录积分扣减
                                        recordPointDeduction(currentUserId, apiRequestVO.getPlatformName());
                                        log.info("API查询成功 - 用户ID: {}, 平台: {}", currentUserId, apiRequestVO.getPlatformName());
                                    } else {
                                        // 如果查询失败，记录失败日志
                                        String errorMessage = (String) result.getOrDefault("message", "未知错误");
                                        log.warn("API查询失败 - 用户ID: {}, 平台: {}, 错误信息: {}", currentUserId, apiRequestVO.getPlatformName(), errorMessage);
                                    }

                                    return result;
                                } else {
                                    // 记录失败的API查询
                                    apiQueryRecord.setRequestStatus("1"); // 1表示失败
                                    apiQueryRecord.setRequestTime(0L); // 请求失败时设置为0
                                    apiQueryRecord.setResponseResult("results列表第一个元素不是Map类型");
                                    userApiQueryRecordService.insertUserApiQueryRecord(apiQueryRecord);

                                    return Map.of("success", false, "message", "results列表第一个元素不是Map类型");
                                }
                            } else {
                                // 记录失败的API查询
                                apiQueryRecord.setRequestStatus("1"); // 1表示失败
                                apiQueryRecord.setRequestTime(0L); // 请求失败时设置为0
                                apiQueryRecord.setResponseResult("results列表为空，无数据可解析");
                                userApiQueryRecordService.insertUserApiQueryRecord(apiQueryRecord);

                                return Map.of("success", false, "message", "results列表为空，无数据可解析");
                            }
                        } else {
                            // 记录失败的API查询
                            apiQueryRecord.setRequestStatus("1"); // 1表示失败
                            apiQueryRecord.setRequestTime(0L); // 请求失败时设置为0
                            apiQueryRecord.setResponseResult("results字段不是List类型，无法解析");
                            userApiQueryRecordService.insertUserApiQueryRecord(apiQueryRecord);

                            return Map.of("success", false, "message", "results字段不是List类型，无法解析");
                        }
                    } else {
                        // 记录失败的API查询
                        apiQueryRecord.setRequestStatus("1"); // 1表示失败
                        apiQueryRecord.setRequestTime(0L); // 请求失败时设置为0
                        apiQueryRecord.setResponseResult("处理results字段失败：外层status=" + outerStatus);
                        userApiQueryRecordService.insertUserApiQueryRecord(apiQueryRecord);

                        // status≠success时，直接返回失败
                        return Map.of("success", false, "message", "处理results字段失败：外层status=" + outerStatus);
                    }
                }
                // 3. 无data/results字段：保持原有逻辑，不判断status
                else {
                    Map<String, Object> result = parseInnerResponse(responseMap, "default");

                    // 记录API查询结果
                    recordApiQueryResult(apiQueryRecord, result, response);

                    // 记录查询结果日志（成功和失败都记录）
                    if (Boolean.TRUE.equals(result.get("success"))) {
                        // 如果查询成功，记录积分扣减
                        recordPointDeduction(currentUserId, apiRequestVO.getPlatformName());
                        log.info("API查询成功 - 用户ID: {}, 平台: {}", currentUserId, apiRequestVO.getPlatformName());
                    } else {
                        // 如果查询失败，记录失败日志
                        String errorMessage = (String) result.getOrDefault("message", "未知错误");
                        log.warn("API查询失败 - 用户ID: {}, 平台: {}, 错误信息: {}", currentUserId, apiRequestVO.getPlatformName(), errorMessage);
                    }

                    return result;
                }
            } catch (JsonProcessingException e) {
                System.err.println("解析响应JSON失败: " + e.getMessage());

                // 记录JSON解析失败到数据库
                apiQueryRecord.setRequestStatus("1"); // 1表示失败
                apiQueryRecord.setRequestTime(0L); // 请求失败时设置为0
                apiQueryRecord.setResponseResult("解析响应JSON失败: " + e.getMessage());
                userApiQueryRecordService.insertUserApiQueryRecord(apiQueryRecord);

                return Map.of("success", false, "message", "解析响应JSON失败: " + e.getMessage());
            } catch (Exception e) {
                System.err.println("解析响应结果失败: " + e.getMessage());

                // 记录响应解析失败到数据库
                apiQueryRecord.setRequestStatus("1"); // 1表示失败
                apiQueryRecord.setRequestTime(0L); // 请求失败时设置为0
                apiQueryRecord.setResponseResult("解析响应结果失败: " + e.getMessage());
                userApiQueryRecordService.insertUserApiQueryRecord(apiQueryRecord);

                return Map.of("success", false, "message", "解析响应结果失败: " + e.getMessage());
            }
        } else {
            // 记录失败的API查询（响应为空）
            apiQueryRecord.setRequestStatus("1"); // 1表示失败
            apiQueryRecord.setRequestTime(0L); // 请求失败时设置为0
            apiQueryRecord.setResponseResult("请求失败，响应为空");
            userApiQueryRecordService.insertUserApiQueryRecord(apiQueryRecord);

            return Map.of("success", false, "message", "请求失败，响应为空");
        }
    }

    /**
     * 记录API查询结果
     * @param apiQueryRecord API查询记录对象
     * @param result 查询结果
     * @param response 原始响应内容
     */
    private void recordApiQueryResult(UserApiQueryRecord apiQueryRecord, Map<String, Object> result, String response) {
        try {
            // 设置请求状态
            if (Boolean.TRUE.equals(result.get("success"))) {
                apiQueryRecord.setRequestStatus("0"); // 0表示成功
            } else {
                apiQueryRecord.setRequestStatus("1"); // 1表示失败
            }

            // 设置响应结果（截断过长的响应内容）
            if (result != null) {
                apiQueryRecord.setResponseResult(result.toString());
                if (result.containsKey("platformResults")) {
                    Object platformResultsObj = result.get("platformResults");
                    if (platformResultsObj instanceof List) {
                        List<Map<String, Object>> platformResults = (List<Map<String, Object>>) platformResultsObj;
                        if (!platformResults.isEmpty()) {
                            Map<String, Object> firstPlatform = platformResults.get(0);
                            if (firstPlatform.containsKey("status")) {
                                String status = (String) firstPlatform.get("status");
                                apiQueryRecord.setResults(status);
                            }
                        }
                    }
                }
            } else {
                apiQueryRecord.setResponseResult(response);
            }

            // 保存API查询记录
            userApiQueryRecordService.insertUserApiQueryRecord(apiQueryRecord);
        } catch (Exception e) {
            System.err.println("记录API查询结果失败: " + e.getMessage());
        }
    }

    /**
     * 记录积分扣减
     * @param userId 用户ID
     * @param platformName 平台名称
     */
    private void recordPointDeduction(Long userId, String platformName) {
        try {
            // 1. 先获取当前用户信息
            SysUser sysUser = sysUserService.selectUserById(userId);
            if (sysUser == null) {
                System.err.println("用户不存在，无法记录积分扣减: " + userId);
                return;
            }

            // 2. 更新用户积分（扣减1积分）
            Integer currentPoints = sysUser.getPoints();
            if (currentPoints == null || currentPoints < 1) {
                System.err.println("用户积分不足，无法扣减: " + userId);
                return;
            }

            sysUser.setPoints(currentPoints - 1);
            boolean updateSuccess = sysUserService.updateUser(sysUser);

            if (!updateSuccess) {
                System.err.println("更新用户积分失败: " + userId);
                return;
            }

            // 3. 记录积分变更记录

            UserPointRecord pointRecord = new UserPointRecord();
            pointRecord.setUserId(userId);
            pointRecord.setPointAmount(-1L); // 每次查询扣减1积分
            pointRecord.setPointType("3"); // 3表示查询扣减
            pointRecord.setReason("API查询扣减 - " + (platformName != null ? platformName : "未知平台"));
            pointRecord.setOperatorId(userId); // 操作人ID为当前用户
            pointRecord.setCreateBy(SecurityUtils.getUsername()); // 创建人ID为当前用户
            pointRecord.setCreateTime(new Date());

            userPointRecordService.insertUserPointRecord(pointRecord);

        } catch (Exception e) {
            System.err.println("记录积分扣减失败: " + e.getMessage());
        }
    }

    /**
     * 解析内部响应数据
     * @param dataMap 内部数据Map
     * @param platformName 平台名称
     * @return 标准化的响应结果
     */
    private Map<String, Object> parseInnerResponse(Map<String, Object> dataMap, String platformName) {
        String phone = null;
        List<Map<String, Object>> platformResults = new ArrayList<>();

        // 提取手机号
        if (dataMap.containsKey("phone")) {
            phone = String.valueOf(dataMap.get("phone"));
        }

        // 定义平台字段列表
        String[] platformFields = {"xiaomi", "taidixiong", "baidu", "dianhuabang", "sghmt",
                "sanliuling", "yidonggaopin", "ltgj","tengxun"};

        // 1. 单独处理通用的 "platform" 字段 (移出循环，避免重复添加)
        if (dataMap.containsKey("platform")) {
            Map<String, Object> result = new HashMap<>(2);
            result.put("platform", String.valueOf(dataMap.get("platform")));
            result.put("status", String.valueOf(dataMap.get("status")));
            platformResults.add(result);
        }

        // 2. 遍历特定字段
        for (String field : platformFields) {
            // 如果 field 就是 "platform"，则跳过
            if ("platform".equals(field)) continue;

            if (dataMap.containsKey(field)) {
                Map<String, Object> result = new HashMap<>(2);
                result.put("platform", field);
                Object value = dataMap.get(field);
                result.put("status", value != null ? String.valueOf(value) : null);
                platformResults.add(result);
            }
        }

        // 构建标准化结果
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("phone", phone);
        result.put("platformResults", platformResults);
//        result.put("originalData", dataMap);

        return result;
    }

    /**
     * 执行前置操作，如获取Token
     * @param preActionConfig 前置操作配置JSON字符串
     * @return 包含token的Map
     */
    private Map<String, Object> executePreAction(String preActionConfig) {
        if (preActionConfig == null || preActionConfig.isEmpty()) {
            return Map.of("success", false, "message", "前置操作配置为空");
        }
        try {
            // 使用方式
            ObjectMapper objectMapper = new ObjectMapper();
            PreActionConfig config = objectMapper.readValue(preActionConfig, PreActionConfig.class);
            String url = config.getUrl();
            Map<String, Object> params = config.getParams();
            // 验证必要参数
            if (url == null || url.isEmpty()) {
                return Map.of("success", false, "message", "前置操作URL不能为空");
            }

            // 发送请求获取token - 统一使用JSON格式发送
            String response = null;
            try {
                // 将参数转换为JSON字符串
                UserInfo360VO userInfo360VO = objectMapper.convertValue(params, UserInfo360VO.class);
                // 使用postJson方法发送JSON格式的POST请求
                response = HttpUtils.postJson(url, userInfo360VO);
            } catch (Exception e) {
                // 捕获HTTP请求异常，特别是500状态码
                String errorMessage = e.getMessage();
                if (errorMessage != null ) {
                    return Map.of("success", false, "message", "请求错误");
                } else {
                    return Map.of("success", false, "message", "前置操作请求异常: " + errorMessage);
                }
            }

            if (response != null) {
                // 直接解析响应，不需要Unicode解码（JSON库会自动处理）
                Map<?, ?> responseMap = objectMapper.readValue(response, Map.class);
                Object token = responseMap.get("token");
                if (token != null) {
                    return Map.of("success", true, "token", token);
                } else {
                    // 检查响应中是否有错误信息
                    Object errorMsg = responseMap.get("message") != null ? responseMap.get("message") :
                            responseMap.get("error") != null ? responseMap.get("error") :
                                    responseMap.get("msg");
                    if (errorMsg != null) {
                        return Map.of("success", false, "message", "获取token失败: " + errorMsg);
                    } else {
                        return Map.of("success", false, "message", "获取token失败，响应中未找到token字段");
                    }
                }
            } else {
                return Map.of("success", false, "message", "获取token请求失败，响应为空");
            }
        } catch (Exception e) {
            System.err.println("执行前置操作失败: " + e.getMessage());
            return Map.of("success", false, "message", "执行前置操作失败: " + e.getMessage());
        }
    }
}
