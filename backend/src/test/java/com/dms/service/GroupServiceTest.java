package com.dms.service;

import com.dms.domain.Group;
import com.dms.repository.GroupRepository;
import com.dms.repository.UserGroupRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock
    private GroupRepository groupRepository;
    @Mock
    private UserGroupRepository userGroupRepository;
    @Mock
    private TenantContext tenantContext;
    @Mock
    private AuditService auditService;
    @InjectMocks
    private GroupService groupService;

    @Test
    void shouldCreateGroup_whenValid() {
        UUID tenantId = UUID.randomUUID();
        when(tenantContext.getCurrentTenantId()).thenReturn(tenantId);
        when(tenantContext.getCurrentUserId()).thenReturn("admin");
        when(groupRepository.findByNameAndTenantId("finance", tenantId)).thenReturn(Optional.empty());
        when(groupRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Group result = groupService.createGroup("finance", "Finance", "Finance docs", null);
        assertEquals("finance", result.getName());
        assertEquals("Finance", result.getDisplayName());
    }

    @Test
    void shouldThrow_whenGroupAlreadyExists() {
        UUID tenantId = UUID.randomUUID();
        when(tenantContext.getCurrentTenantId()).thenReturn(tenantId);
        when(groupRepository.findByNameAndTenantId("finance", tenantId)).thenReturn(Optional.of(Group.builder().build()));
        assertThrows(RuntimeException.class, () -> groupService.createGroup("finance", "Finance", "Finance docs", null));
    }
}

