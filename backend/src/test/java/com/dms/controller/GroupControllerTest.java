package com.dms.controller;

import com.dms.domain.Group;
import com.dms.security.RateLimitingFilter;
import com.dms.service.GroupService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GroupController.class)
@AutoConfigureMockMvc(addFilters = false)
class GroupControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private GroupService groupService;
    @MockBean
    private RateLimitingFilter rateLimitingFilter;

    @Test
    void shouldReturnGroup_whenGroupExists() throws Exception {
        UUID id = UUID.randomUUID();
        when(groupService.getGroup(id)).thenReturn(Group.builder()
            .id(id)
            .name("finance")
            .displayName("Finance")
            .createdAt(Instant.now())
            .createdBy("admin")
            .tenantId(UUID.randomUUID())
            .build());

        mockMvc.perform(get("/api/v1/groups/{groupId}", id).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id.toString()));
    }
}
