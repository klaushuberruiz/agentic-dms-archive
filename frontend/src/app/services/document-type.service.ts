import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { DocumentType } from '../models/document-type.model';

@Injectable({ providedIn: 'root' })
export class DocumentTypeService {
  private readonly baseUrl = `${environment.apiBaseUrl}/document-types`;

  constructor(private readonly http: HttpClient) {}

  list(page = 0, pageSize = 20): Observable<any> {
    return this.http.get<any>(this.baseUrl, {
      params: { page, pageSize },
    });
  }

  getActive(): Observable<DocumentType[]> {
    return this.http.get<DocumentType[]>(`${this.baseUrl}/active`);
  }

  getById(typeId: string): Observable<DocumentType> {
    return this.http.get<DocumentType>(`${this.baseUrl}/${typeId}`);
  }

  create(request: Partial<DocumentType>): Observable<DocumentType> {
    return this.http.post<DocumentType>(this.baseUrl, request);
  }

  update(typeId: string, request: Partial<DocumentType>): Observable<DocumentType> {
    return this.http.put<DocumentType>(`${this.baseUrl}/${typeId}`, request);
  }

  deactivate(typeId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${typeId}`);
  }
}
