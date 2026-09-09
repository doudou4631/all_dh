package com.geek.server.controller;

import com.geek.common.core.controller.BaseController;
import com.geek.common.core.domain.AjaxResult;
import com.geek.server.service.IFreeQueryStatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/server/freeQuerySettings/statistics")
public class FreeQueryStatisticsController extends BaseController {
    @Autowired
    private IFreeQueryStatisticsService statisticsService;

    @PreAuthorize("@ss.hasPermi('freeQuery:statistics:list')")
    @GetMapping("/summary")
    public AjaxResult summary() {
        return success(statisticsService.getStatistics());
    }
}
