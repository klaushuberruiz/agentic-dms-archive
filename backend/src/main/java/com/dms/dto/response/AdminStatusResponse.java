package com.dms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatusResponse {
    private long expiredDocumentsCount;
    private long documentsWithActiveLegalHoldsCount;
    private long timestamp;
}
