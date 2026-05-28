package com.geek.online.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.geek.common.annotation.Anonymous;
import com.geek.common.core.controller.BaseController;
import com.geek.common.core.domain.BaseEntity;
import com.geek.common.core.page.TableDataInfo;
import com.geek.online.mapper.OnlineDbMapper;


/**
 * mysql数据库Controller接口
 * 
 * @author Dftre
 * @date 2024-01-26
 */
@RestController
@RequestMapping("/online/db")
@Anonymous
public class OnlineDbController extends BaseController {

    @Autowired
    private OnlineDbMapper onlineDbMapper;

    @GetMapping("/table/list")
    public TableDataInfo<Map<String, String>> selectDbTableList(BaseEntity baseEntity){
        startPage();
        return getDataTable(onlineDbMapper.selectDbTableList(baseEntity));
    }

    @GetMapping("/column/list")
    public TableDataInfo<Map<String, String>> selectDbColumnsListByTableName(String tableName){
        return getDataTable(onlineDbMapper.selectDbColumnsListByTableName(tableName));
    }
}
