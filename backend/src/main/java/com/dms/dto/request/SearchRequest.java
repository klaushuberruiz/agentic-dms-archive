package com.dms.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequest {

    private String documentType;
    private Map<String, Object> metadata;
    private LocalDate dateFrom;
    private LocalDate dateTo;
    @Builder.Default
    private boolean includeDeleted = false;
    @Builder.Default
    private int page = 0;
    @Builder.Default
    private int pageSize = 20;
}
