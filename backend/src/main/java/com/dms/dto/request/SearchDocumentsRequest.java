package com.dms.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchDocumentsRequest {

    @NotBlank
    private String query;

    private String documentType;

    @Min(1)
    @Max(100)
    @Builder.Default
    private Integer pageSize = 20;
}
