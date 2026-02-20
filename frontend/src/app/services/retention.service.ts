import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class RetentionService {
  private readonly baseUrl = `${environment.apiBaseUrl}/documents`;
  private readonly adminUrl = `${environment.apiBaseUrl}/admin`;

  constructor(private readonly http: HttpClient) {}

  getRetentionStatus(documentId: string): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/${documentId}/retention-status`);
  }

  setRetention(documentId: string, retentionDays: number): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/${documentId}/retention`, {
      retentionDays,
    });
  }

  getRetentionCounts(): Observable<any> {
    return this.http.get<any>(`${this.adminUrl}/retention/count`);
  }

  triggerRetentionCleanup(): Observable<any> {
    return this.http.post<any>(`${this.adminUrl}/retention/process`, {});
  }
}
