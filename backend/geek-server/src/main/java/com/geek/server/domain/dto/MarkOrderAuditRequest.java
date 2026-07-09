package com.geek.server.domain.dto;

import lombok.Data;

/**
 * Agent order audit request.
 */
@Data
public class MarkOrderAuditRequest {

    /** Audit opinion (recommended for reject/return). */
    private String auditOpinion;
}
