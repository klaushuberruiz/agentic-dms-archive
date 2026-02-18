export interface DocumentType {
  id: string;
  tenantId: string;
  name: string;
  displayName: string;
  description: string;
  metadataSchema: Record<string, unknown>;
  allowedGroups: string[];
  retentionDays: number;
  active: boolean;
}
