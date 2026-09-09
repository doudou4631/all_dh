package com.geek.server.controller;

import com.geek.common.core.controller.BaseController;
import com.geek.common.core.domain.AjaxResult;
import com.geek.common.core.page.TableDataInfo;
import com.geek.server.domain.FreeQueryAuditQuery;
import com.geek.server.domain.FreeQueryAuditRecord;
import com.geek.server.service.IFreeQueryAuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/server/freeQuerySettings/ipRecord")
public class FreeQueryAuditController extends BaseController {
    @Autowired
    private IFreeQueryAuditService auditService;

    @PreAuthorize("@ss.hasPermi('freeQuery:ipRecord:list')")
    @GetMapping("/list")
    public TableDataInfo list(FreeQueryAuditQuery query) {
        startPage();
        List<FreeQueryAuditRecord> records = auditService.selectList(query);
        return getDataTable(records);
    }

    @PreAuthorize("@ss.hasPermi('freeQuery:ipRecord:query')")
    @GetMapping("/{id}")
    public AjaxResult details(@PathVariable Long id) {
        return success(auditService.selectDetails(id));
    }
}
