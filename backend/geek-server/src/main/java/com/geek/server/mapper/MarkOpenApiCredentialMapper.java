package com.geek.server.mapper;

import com.geek.server.domain.MarkOpenApiCredential;
import com.geek.server.domain.vo.MarkCustomerApiAgentOptionVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface MarkOpenApiCredentialMapper {
    List<MarkOpenApiCredential> selectList(@Param("name") String name, @Param("status") String status,
                                           @Param("credentialType") String credentialType);
    MarkOpenApiCredential selectByApiKey(@Param("apiKey") String apiKey);
    MarkOpenApiCredential selectById(@Param("id") Long id);
    int insert(MarkOpenApiCredential credential);
    int updateStatus(@Param("id") Long id, @Param("status") String status);
    int updateSecret(@Param("id") Long id, @Param("secretHash") String secretHash);
    int updateLastUsed(@Param("id") Long id);
    int updateCustomer(MarkOpenApiCredential credential);
    int deleteCustomer(@Param("id") Long id);
    int deletePlatforms(@Param("credentialId") Long credentialId);
    int insertPlatform(@Param("credentialId") Long credentialId, @Param("platformCode") String platformCode);
    List<String> selectPlatforms(@Param("credentialId") Long credentialId);
    String selectItemPlatform(@Param("itemId") Long itemId);
    List<MarkCustomerApiAgentOptionVO> selectActiveAgents();
    int countActiveAgent(@Param("userId") Long userId);
    int insertAccessLog(@Param("credentialId") Long credentialId, @Param("apiKey") String apiKey,
                        @Param("requestPath") String requestPath, @Param("httpMethod") String httpMethod,
                        @Param("platformCode") String platformCode, @Param("success") boolean success,
                        @Param("message") String message, @Param("elapsedMs") long elapsedMs);
}
