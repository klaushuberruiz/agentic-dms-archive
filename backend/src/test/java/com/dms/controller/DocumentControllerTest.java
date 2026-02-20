package com.dms.controller;

import com.dms.dto.response.DocumentResponse;
import com.dms.security.RateLimitingFilter;
import com.dms.service.DocumentService;
import com.dms.service.SearchService;
import com.dms.service.VersioningService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentController.class)
@AutoConfigureMockMvc(addFilters = false)
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private DocumentService documentService;
    @MockBean
    private VersioningService versioningService;
    @MockBean
    private SearchService searchService;
    @MockBean
    private RateLimitingFilter rateLimitingFilter;

    @Test
    void shouldReturnDocument_whenDocumentExists() throws Exception {
        UUID id = UUID.randomUUID();
        when(documentService.getDocument(id)).thenReturn(DocumentResponse.builder()
            .id(id)
            .documentTypeName("invoice")
            .currentVersion(1)
            .metadata(Map.of("invoiceNumber", "INV-1"))
            .createdAt(Instant.now())
            .createdBy("alice")
            .build());

        mockMvc.perform(get("/api/v1/documents/{id}", id).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id.toString()));
    }
}
