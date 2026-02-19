import { SearchRequest } from '../../models/search.model';

export function buildSearchRequest(query: string, filters: SearchRequest): SearchRequest {
  const normalizedQuery = query.trim();
  const metadata = { ...(filters.metadata ?? {}) };

  if (normalizedQuery.length > 0) {
    metadata['query'] = normalizedQuery;
  }

  return {
    ...filters,
    metadata: Object.keys(metadata).length > 0 ? metadata : undefined,
  };
}
