import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuditLog } from '../models/audit.model';

@Injectable({ providedIn: 'root' })
export class AuditService {
  private readonly baseUrl = `${environment.apiBaseUrl}/audit`;

  constructor(private readonly http: HttpClient) {}

  search(filters: Record<string, string>): Observable<AuditLog[]> {
    return this.http.get<AuditLog[]>(this.baseUrl, { params: filters });
  }
}
