package com.dms.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class DocumentUploadRequest {
    @NotNull
    private UUID documentTypeId;
    
    @NotNull
    @Valid
    private Map<String, Object> metadata;
    
    private String idempotencyKey;
}
