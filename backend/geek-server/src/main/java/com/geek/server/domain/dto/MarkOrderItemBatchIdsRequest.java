package com.geek.server.domain.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * Batch update mark order items by ids.
 */
@Data
public class MarkOrderItemBatchIdsRequest {

    @NotEmpty(message = "itemIds must not be empty")
    private List<Long> itemIds;
}
