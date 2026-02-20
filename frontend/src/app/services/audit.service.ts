import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuditLog } from '../models/audit.model';
import { PageResponse } from '../models/api.model';

type AuditStatistics = Record<string, number | string | boolean | null>;

@Injectable({ providedIn: 'root' })
export class AuditService {
  private readonly baseUrl = `${environment.apiBaseUrl}/audit`;

  constructor(private readonly http: HttpClient) {}

  search(filters: { action?: string; userId?: string }): Observable<AuditLog[]> {
    let params = new HttpParams();
    if (filters.action) {
      params = params.set('action', filters.action);
    }
    if (filters.userId) {
      params = params.set('userId', filters.userId);
    }

    return this.http
      .get<{ content?: AuditLog[] } | AuditLog[]>(`${this.baseUrl}/logs`, { params })
      .pipe(map((response) => (Array.isArray(response) ? response : (response.content ?? []))));
  }

  getLogs(page = 0, pageSize = 50): Observable<PageResponse<AuditLog> | AuditLog[]> {
    return this.http.get<PageResponse<AuditLog> | AuditLog[]>(`${this.baseUrl}/logs`, {
      params: { page, pageSize },
    });
  }

  getLogDetail(logId: string): Observable<AuditLog> {
    return this.http.get<AuditLog>(`${this.baseUrl}/logs/${logId}`);
  }

  getDocumentAuditTrail(documentId: string, page = 0, pageSize = 50): Observable<PageResponse<AuditLog> | AuditLog[]> {
    return this.http.get<PageResponse<AuditLog> | AuditLog[]>(`${this.baseUrl}/logs/document/${documentId}`, {
      params: { page, pageSize },
    });
  }

  getUserAuditTrail(userId: string, page = 0, pageSize = 50): Observable<PageResponse<AuditLog> | AuditLog[]> {
    return this.http.get<PageResponse<AuditLog> | AuditLog[]>(`${this.baseUrl}/logs/user/${userId}`, {
      params: { page, pageSize },
    });
  }

  getStatistics(startTime?: string, endTime?: string): Observable<AuditStatistics> {
    const params: Record<string, string> = {};
    if (startTime) params['startTime'] = startTime;
    if (endTime) params['endTime'] = endTime;
    return this.http.get<AuditStatistics>(`${this.baseUrl}/statistics`, { params });
  }

  exportLogs(startTime?: string, endTime?: string, format: string = 'csv'): Observable<Blob> {
    const params: Record<string, string> = { format };
    if (startTime) params['startTime'] = startTime;
    if (endTime) params['endTime'] = endTime;
    return this.http.post(`${this.baseUrl}/export`, {}, { 
      params, 
      responseType: 'blob' 
    });
  }
}
