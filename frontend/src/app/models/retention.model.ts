export interface RetentionStatus {
  documentId: string;
  documentType: string;
  defaultRetentionDays: number;
  retentionExpiresAt: string | null;
  daysUntilRetention: number | null;
  hasActiveLegalHolds: boolean;
  isEligibleForHardDelete: boolean;
  isSoftDeleted: boolean;
}

export interface RetentionCounts {
  expiredDocuments: number;
  activeLegalHolds: number;
}

export interface RetentionCleanupResult {
  processedCount?: number;
  status?: string;
}
