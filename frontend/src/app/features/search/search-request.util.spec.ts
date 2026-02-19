import { describe, expect, it } from 'vitest';
import { buildSearchRequest } from './search-request.util';

describe('buildSearchRequest', () => {
  it('shouldExpectedBehavior_whenQueryProvided', () => {
    const request = buildSearchRequest('invoice', { documentType: 'invoice' });

    expect(request.documentType).toBe('invoice');
    expect(request.metadata).toEqual({ query: 'invoice' });
  });

  it('shouldExpectedBehavior_whenQueryHasWhitespace', () => {
    const request = buildSearchRequest('  policy  ', {});

    expect(request.metadata).toEqual({ query: 'policy' });
  });

  it('shouldExpectedBehavior_whenNoQueryAndNoMetadata', () => {
    const request = buildSearchRequest('   ', {});

    expect(request.metadata).toBeUndefined();
  });

  it('shouldExpectedBehavior_whenMetadataExists', () => {
    const request = buildSearchRequest('invoice', { metadata: { region: 'eu' } });

    expect(request.metadata).toEqual({ region: 'eu', query: 'invoice' });
  });
});
