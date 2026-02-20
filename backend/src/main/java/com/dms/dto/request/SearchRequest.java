package com.dms.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
    @Min(0)
    @Builder.Default
    private int page = 0;
    @Min(1)
    @Max(100)
    @Builder.Default
    private int pageSize = 20;
}
