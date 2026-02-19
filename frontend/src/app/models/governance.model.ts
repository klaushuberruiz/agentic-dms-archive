export interface RequirementVersionHistoryItem {
  requirementId: string;
  version: string;
  approvalStatus: string;
  modifiedAt: string;
  modifiedBy: string;
  changeSummary: string;
}

export interface TraceabilityItem {
  requirementId: string;
  module: string;
  codePath: string;
  approvalStatus: string;
  hasImplementation: boolean;
}

export interface RetrievalAuditItem {
  requirementId: string;
  version: string;
  user: string;
  timestamp: string;
  toolName: string;
  queryParameters: string;
}
