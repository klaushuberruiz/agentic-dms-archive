import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../environments/environment';
import { LegalHold, PlaceLegalHoldResponse } from '../models/legal-hold.model';

@Injectable({ providedIn: 'root' })
export class LegalHoldService {
  private readonly baseUrl = `${environment.apiBaseUrl}/legal-holds`;

  constructor(private readonly http: HttpClient) {}

  placeLegalHold(documentId: string, caseReference: string, reason: string): Observable<PlaceLegalHoldResponse> {
    return this.http.post<PlaceLegalHoldResponse>(this.baseUrl, {
      documentId,
      caseReference,
      reason,
    });
  }

  getActiveLegalHolds(): Observable<LegalHold[]> {
    return this.http.get<Array<Record<string, unknown>>>(this.baseUrl).pipe(map((items) => items.map((item) => this.mapLegalHold(item))));
  }

  getLegalHoldHistory(documentId: string): Observable<LegalHold[]> {
    return this.http.get<Array<Record<string, unknown>>>(`${this.baseUrl}/document/${documentId}`).pipe(map((items) => items.map((item) => this.mapLegalHold(item))));
  }

  releaseLegalHold(holdId: string, releaseReason: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${holdId}`, { params: { reason: releaseReason } });
  }

  private mapLegalHold(item: Record<string, unknown>): LegalHold {
    const documentRef = item['document'] as Record<string, unknown> | undefined;
    return {
      id: String(item['id']),
      tenantId: String(item['tenantId']),
      documentId: String(item['documentId'] ?? documentRef?.['id'] ?? ''),
      caseReference: String(item['caseReference']),
      reason: String(item['reason']),
      placedAt: String(item['placedAt']),
      placedBy: String(item['placedBy']),
      releasedAt: (item['releasedAt'] as string | null) ?? null,
      releasedBy: (item['releasedBy'] as string | null) ?? null,
      releaseReason: (item['releaseReason'] as string | null) ?? null,
    };
  }
}
