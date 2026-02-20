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
public class SearchRequirementsRequest {

    @NotBlank
    private String query;

    @Min(1)
    @Max(50)
    @Builder.Default
    private Integer limit = 10;
}
