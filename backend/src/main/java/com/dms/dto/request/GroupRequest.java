package com.dms.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
public class GroupRequest {
    @NotBlank
    private String name;
    
    private String displayName;
    
    private String description;
    
    private UUID parentGroupId;
}
