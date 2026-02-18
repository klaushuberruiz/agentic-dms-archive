export interface Group {
  id: string;
  tenantId: string;
  name: string;
  displayName: string;
  description: string;
  parentGroupId: string | null;
}
