export interface AuditLog {
  id: string;
  tenantId: string;
  action: string;
  entityType: string;
  entityId: string;
  userId: string;
  clientIp: string;
  correlationId: string;
  details: Record<string, unknown>;
  timestamp: string;
}
