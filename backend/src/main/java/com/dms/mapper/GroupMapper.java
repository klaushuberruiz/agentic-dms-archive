package com.dms.mapper;

import com.dms.domain.Group;
import com.dms.dto.response.GroupResponse;
import org.springframework.stereotype.Component;

@Component
public class GroupMapper {

    public GroupResponse toResponse(Group group) {
        if (group == null) {
            return null;
        }

        return GroupResponse.builder()
            .id(group.getId())
            .tenantId(group.getTenantId())
            .name(group.getName())
            .displayName(group.getDisplayName())
            .description(group.getDescription())
            .parentGroupId(group.getParentGroup() == null ? null : group.getParentGroup().getId())
            .createdAt(group.getCreatedAt())
            .createdBy(group.getCreatedBy())
            .modifiedAt(group.getModifiedAt())
            .modifiedBy(group.getModifiedBy())
            .build();
    }
}
