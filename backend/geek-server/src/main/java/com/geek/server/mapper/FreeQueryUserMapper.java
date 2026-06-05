package com.geek.server.mapper;

import com.geek.server.domain.FreeQueryUser;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface FreeQueryUserMapper {

    FreeQueryUser selectFreeQueryUserById(Long id);

    FreeQueryUser selectFreeQueryUserByAccount(@Param("account") String account);

    FreeQueryUser selectFreeQueryUserByPhone(@Param("phone") String phone);

    List<FreeQueryUser> selectFreeQueryUserList(FreeQueryUser query);

    int insertFreeQueryUser(FreeQueryUser user);

    int updateFreeQueryUser(FreeQueryUser user);

    int updateFreeQueryUserPassword(@Param("id") Long id,
                                    @Param("password") String password,
                                    @Param("updateBy") String updateBy,
                                    @Param("updateTime") Date updateTime);

    int updateFreeQueryUserLoginInfo(@Param("id") Long id,
                                     @Param("lastLoginIp") String lastLoginIp,
                                     @Param("lastLoginTime") Date lastLoginTime,
                                     @Param("updateBy") String updateBy,
                                     @Param("updateTime") Date updateTime);

    int increasePoints(@Param("id") Long id,
                       @Param("pointDelta") Integer pointDelta,
                       @Param("updateBy") String updateBy,
                       @Param("updateTime") Date updateTime);

    int deductPoints(@Param("id") Long id,
                     @Param("pointDelta") Integer pointDelta,
                     @Param("updateBy") String updateBy,
                     @Param("updateTime") Date updateTime);

    int deleteFreeQueryUserByIds(@Param("ids") Long[] ids,
                                 @Param("updateBy") String updateBy,
                                 @Param("updateTime") Date updateTime);
}
