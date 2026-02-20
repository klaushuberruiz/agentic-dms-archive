import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class LegalHoldService {
  private readonly baseUrl = `${environment.apiBaseUrl}/documents`;

  constructor(private readonly http: HttpClient) {}

  placeLegalHold(documentId: string, caseReference: string, reason: string): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/${documentId}/legal-holds`, {
      caseReference,
      reason,
    });
  }

  getActiveLegalHolds(documentId: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/${documentId}/legal-holds`);
  }

  getLegalHoldHistory(documentId: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/${documentId}/legal-holds/history`);
  }

  releaseLegalHold(holdId: string, releaseReason: string): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/legal-holds/${holdId}/release`, {
      releaseReason,
    });
  }
}
