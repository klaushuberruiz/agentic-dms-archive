package com.dms.controller;

import com.dms.domain.Group;
import com.dms.domain.UserGroup;
import com.dms.dto.request.GroupRequest;
import com.dms.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Group> create(@Valid @RequestBody GroupRequest request) {
        return ResponseEntity.status(201).body(groupService.createGroup(
            request.getName(),
            request.getDisplayName(),
            request.getDescription(),
            request.getParentGroupId()));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<Group>> list(Pageable pageable) {
        return ResponseEntity.ok(groupService.listGroups(pageable));
    }

    @GetMapping("/{groupId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Group> getById(@PathVariable UUID groupId) {
        return ResponseEntity.ok(groupService.getGroup(groupId));
    }

    @GetMapping("/{groupId}/hierarchy")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getHierarchy(@PathVariable UUID groupId) {
        return ResponseEntity.ok(groupService.getGroupHierarchy(groupId));
    }

    @PutMapping("/{groupId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Group> update(@PathVariable UUID groupId, @Valid @RequestBody GroupRequest request) {
        return ResponseEntity.ok(groupService.updateGroup(
            groupId,
            request.getDisplayName(),
            request.getDescription(),
            request.getParentGroupId()));
    }

    @DeleteMapping("/{groupId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID groupId) {
        groupService.deleteGroup(groupId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{groupId}/members/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> addMember(@PathVariable UUID groupId, @PathVariable String userId) {
        groupService.addUserToGroup(groupId, userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{groupId}/members/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> removeMember(@PathVariable UUID groupId, @PathVariable String userId) {
        groupService.removeUserFromGroup(groupId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{groupId}/members")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserGroup>> listMembers(@PathVariable UUID groupId, Pageable pageable) {
        return ResponseEntity.ok(groupService.getGroupMembers(groupId, pageable));
    }
}
