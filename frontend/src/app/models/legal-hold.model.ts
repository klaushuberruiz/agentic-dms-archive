export interface LegalHold {
  id: string;
  tenantId: string;
  documentId: string;
  caseReference: string;
  reason: string;
  placedAt: string;
  placedBy: string;
  releasedAt: string | null;
  releasedBy: string | null;
  releaseReason: string | null;
}

export interface PlaceLegalHoldResponse {
  legalHoldId?: string;
  status?: string;
}
