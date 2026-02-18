package com.dms.service;

import com.dms.domain.Document;
import com.dms.domain.DocumentType;
import com.dms.domain.Group;
import com.dms.domain.UserGroup;
import com.dms.exception.TenantMismatchException;
import com.dms.exception.UnauthorizedAccessException;
import com.dms.repository.GroupRepository;
import com.dms.repository.UserGroupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

    @Mock
    private TenantContext tenantContext;
    @Mock
    private UserGroupRepository userGroupRepository;
    @Mock
    private GroupRepository groupRepository;

    private AuthorizationService authorizationService;

    @BeforeEach
    void setUp() {
        authorizationService = new AuthorizationService(tenantContext, userGroupRepository, groupRepository);
    }

    @Test
    void shouldGrantAccess_whenUserInAllowedGroup() {
        UUID tenantId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();

        Group group = Group.builder().id(groupId).tenantId(tenantId).name("finance").build();
        UserGroup assignment = UserGroup.builder().tenantId(tenantId).userId("alice").group(group).build();

        DocumentType type = DocumentType.builder().allowedGroups(new UUID[]{groupId}).build();
        Document document = Document.builder().tenantId(tenantId).documentType(type).build();

        when(tenantContext.getCurrentTenantId()).thenReturn(tenantId);
        when(tenantContext.getCurrentUserId()).thenReturn("alice");
        when(userGroupRepository.findAllByTenantIdAndUserId(tenantId, "alice")).thenReturn(List.of(assignment));
        when(groupRepository.findAllByTenantId(tenantId)).thenReturn(List.of(group));

        assertDoesNotThrow(() -> authorizationService.assertCanAccessDocument(document));
    }

    @Test
    void shouldDenyAccess_whenUserNotInAnyAllowedGroup() {
        UUID tenantId = UUID.randomUUID();
        UUID allowedGroup = UUID.randomUUID();
        UUID userGroup = UUID.randomUUID();

        Group group = Group.builder().id(userGroup).tenantId(tenantId).name("sales").build();
        UserGroup assignment = UserGroup.builder().tenantId(tenantId).userId("bob").group(group).build();

        DocumentType type = DocumentType.builder().allowedGroups(new UUID[]{allowedGroup}).build();
        Document document = Document.builder().tenantId(tenantId).documentType(type).build();

        when(tenantContext.getCurrentTenantId()).thenReturn(tenantId);
        when(tenantContext.getCurrentUserId()).thenReturn("bob");
        when(userGroupRepository.findAllByTenantIdAndUserId(tenantId, "bob")).thenReturn(List.of(assignment));
        when(groupRepository.findAllByTenantId(tenantId)).thenReturn(List.of(group));

        assertThrows(UnauthorizedAccessException.class, () -> authorizationService.assertCanAccessDocument(document));
    }

    @Test
    void shouldDenyAccess_whenTenantMismatch() {
        UUID tenantId = UUID.randomUUID();
        UUID otherTenant = UUID.randomUUID();

        DocumentType type = DocumentType.builder().allowedGroups(new UUID[0]).build();
        Document document = Document.builder().tenantId(otherTenant).documentType(type).build();

        when(tenantContext.getCurrentTenantId()).thenReturn(tenantId);

        assertThrows(TenantMismatchException.class, () -> authorizationService.assertCanAccessDocument(document));
    }
}
