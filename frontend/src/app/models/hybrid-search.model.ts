import { PageResponse } from './api.model';

export interface HybridSearchRequest {
  query: string;
}

export interface HybridSearchResult {
  chunkId: string;
  documentId: string;
  sequenceNumber: number;
  content: string;
  tokenCount: number;
  relevanceScore: number;
  searchType: string;
  createdAt: string;
}

export type HybridSearchPage = PageResponse<HybridSearchResult>;
