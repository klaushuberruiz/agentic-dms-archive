package com.dms.mapper;

import com.dms.domain.Group;
import com.dms.dto.response.GroupResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GroupMapperTest {

    private final GroupMapper mapper = new GroupMapper();

    @Test
    void shouldMapGroupToResponse() {
        UUID parentId = UUID.randomUUID();
        Group parent = Group.builder()
            .id(parentId)
            .name("parent")
            .build();

        UUID id = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        Instant now = Instant.now();

        Group group = Group.builder()
            .id(id)
            .tenantId(tenantId)
            .name("finance")
            .displayName("Finance")
            .description("Finance team")
            .parentGroup(parent)
            .createdAt(now)
            .createdBy("admin")
            .modifiedAt(now)
            .modifiedBy("admin")
            .entityVersion(0L)
            .build();

        GroupResponse response = mapper.toResponse(group);

        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getParentGroupId()).isEqualTo(parentId);
        assertThat(response.getDisplayName()).isEqualTo("Finance");
    }
}
