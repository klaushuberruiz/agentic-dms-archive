package com.dms.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HybridSearchRequest {

    @NotBlank
    private String query;

    private String[] filterTags;

    @Builder.Default
    private boolean includeVectorSearch = true;
}
