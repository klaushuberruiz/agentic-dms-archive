import { Document } from './document.model';

export interface SearchRequest {
  documentType?: string;
  metadata?: Record<string, unknown>;
  dateFrom?: string;
  dateTo?: string;
  includeDeleted?: boolean;
  page?: number;
  pageSize?: number;
}

export interface SearchResult {
  results: Document[];
  totalCount: number;
  page: number;
  pageSize: number;
  totalPages: number;
}
