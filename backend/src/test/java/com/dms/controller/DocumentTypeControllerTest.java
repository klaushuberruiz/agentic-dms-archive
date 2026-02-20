package com.dms.controller;

import com.dms.domain.DocumentType;
import com.dms.security.RateLimitingFilter;
import com.dms.service.DocumentTypeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentTypeController.class)
@AutoConfigureMockMvc(addFilters = false)
class DocumentTypeControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private DocumentTypeService documentTypeService;
    @MockBean
    private RateLimitingFilter rateLimitingFilter;

    @Test
    void shouldReturnDocumentType_whenTypeExists() throws Exception {
        UUID id = UUID.randomUUID();
        when(documentTypeService.getDocumentType(id)).thenReturn(DocumentType.builder()
            .id(id)
            .name("invoice")
            .displayName("Invoice")
            .metadataSchema(Map.of("type", "object"))
            .build());

        mockMvc.perform(get("/api/v1/document-types/{typeId}", id).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id.toString()));
    }
}
