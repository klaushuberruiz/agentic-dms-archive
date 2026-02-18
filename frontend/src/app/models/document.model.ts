export interface Document {
  id: string;
  tenantId: string;
  documentTypeId: string;
  currentVersion: number;
  metadata: Record<string, unknown>;
  blobPath: string;
  fileSizeBytes: number;
  contentHash: string;
  createdAt: string;
  createdBy: string;
  modifiedAt: string | null;
  modifiedBy: string | null;
  deletedAt: string | null;
  deletedBy: string | null;
}
