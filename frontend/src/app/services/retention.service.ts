import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { RetentionCleanupResult, RetentionCounts, RetentionStatus } from '../models/retention.model';

@Injectable({ providedIn: 'root' })
export class RetentionService {
  private readonly baseUrl = `${environment.apiBaseUrl}/documents`;
  private readonly adminUrl = `${environment.apiBaseUrl}/admin`;

  constructor(private readonly http: HttpClient) {}

  getRetentionStatus(documentId: string): Observable<RetentionStatus> {
    return this.http.get<RetentionStatus>(`${this.baseUrl}/${documentId}/retention-status`);
  }

  setRetention(documentId: string, retentionDays: number): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/${documentId}/retention`, {
      retentionDays,
    });
  }

  getRetentionCounts(): Observable<RetentionCounts> {
    return this.http.get<RetentionCounts>(`${this.adminUrl}/retention/count`);
  }

  triggerRetentionCleanup(): Observable<RetentionCleanupResult> {
    return this.http.post<RetentionCleanupResult>(`${this.adminUrl}/retention/process`, {});
  }
}
