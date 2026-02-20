import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuditLog } from '../models/audit.model';

@Injectable({ providedIn: 'root' })
export class AuditService {
  private readonly baseUrl = `${environment.apiBaseUrl}/audit`;

  constructor(private readonly http: HttpClient) {}

  getLogs(page = 0, pageSize = 50): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/logs`, {
      params: { page, pageSize },
    });
  }

  getLogDetail(logId: string): Observable<AuditLog> {
    return this.http.get<AuditLog>(`${this.baseUrl}/logs/${logId}`);
  }

  getDocumentAuditTrail(documentId: string, page = 0, pageSize = 50): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/logs/document/${documentId}`, {
      params: { page, pageSize },
    });
  }

  getUserAuditTrail(userId: string, page = 0, pageSize = 50): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/logs/user/${userId}`, {
      params: { page, pageSize },
    });
  }

  getStatistics(startTime?: string, endTime?: string): Observable<any> {
    const params: any = {};
    if (startTime) params.startTime = startTime;
    if (endTime) params.endTime = endTime;
    return this.http.get<any>(`${this.baseUrl}/statistics`, { params });
  }

  exportLogs(startTime?: string, endTime?: string, format: string = 'csv'): Observable<Blob> {
    const params: any = { format };
    if (startTime) params.startTime = startTime;
    if (endTime) params.endTime = endTime;
    return this.http.post(`${this.baseUrl}/export`, {}, { 
      params, 
      responseType: 'blob' 
    });
  }
}
