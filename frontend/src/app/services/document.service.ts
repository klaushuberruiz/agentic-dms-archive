import { Injectable } from '@angular/core';
import { HttpClient, HttpEvent } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Document } from '../models/document.model';
import { SearchResult } from '../models/search.model';

@Injectable({ providedIn: 'root' })
export class DocumentService {
  private readonly baseUrl = `${environment.apiBaseUrl}/documents`;

  constructor(private readonly http: HttpClient) {}

  list(page = 0, pageSize = 20): Observable<SearchResult> {
    return this.http.get<SearchResult>(this.baseUrl, {
      params: { page, pageSize },
    });
  }

  getById(documentId: string): Observable<Document> {
    return this.http.get<Document>(`${this.baseUrl}/${documentId}`);
  }

  upload(formData: FormData): Observable<HttpEvent<Document>> {
    return this.http.post<Document>(this.baseUrl, formData, {
      reportProgress: true,
      observe: 'events',
    });
  }

  updateMetadata(documentId: string, metadata: Record<string, unknown>): Observable<Document> {
    return this.http.patch<Document>(`${this.baseUrl}/${documentId}/metadata`, { metadata });
  }

  download(documentId: string): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/${documentId}/download`, { responseType: 'blob' });
  }

  softDelete(documentId: string, reason: string): Observable<void> {
    return this.http.request<void>('delete', `${this.baseUrl}/${documentId}`, {
      body: { reason },
    });
  }

  restore(documentId: string): Observable<Document> {
    return this.http.post<Document>(`${this.baseUrl}/${documentId}/restore`, {});
  }
}
