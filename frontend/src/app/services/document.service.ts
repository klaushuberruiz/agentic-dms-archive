import { Injectable } from '@angular/core';
import { HttpClient, HttpEvent, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../environments/environment';
import { Document, VersionHistory } from '../models/document.model';
import { SearchResult } from '../models/search.model';

@Injectable({ providedIn: 'root' })
export class DocumentService {
  private readonly baseUrl = `${environment.apiBaseUrl}/documents`;

  constructor(private readonly http: HttpClient) {}

  list(page = 0, pageSize = 20): Observable<SearchResult> {
    return this.http.get<{ content: Document[]; totalElements: number; number: number; size: number; totalPages: number }>(this.baseUrl, {
      params: { page, pageSize },
    }).pipe(map((response) => ({
      results: response.content ?? [],
      totalCount: response.totalElements ?? 0,
      page: response.number ?? page,
      pageSize: response.size ?? pageSize,
      totalPages: response.totalPages ?? 0,
    })));
  }

  upload(request: { documentTypeId: string; metadata: Record<string, unknown>; idempotencyKey?: string }, file: File): Observable<HttpEvent<Document>> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('request', new Blob([JSON.stringify(request)], { type: 'application/json' }));
    return this.http.post<Document>(this.baseUrl, formData, {
      reportProgress: true,
      observe: 'events',
    });
  }

  getById(documentId: string): Observable<Document> {
    return this.http.get<Document>(`${this.baseUrl}/${documentId}`);
  }

  updateMetadata(documentId: string, metadata: Record<string, unknown>): Observable<Document> {
    return this.http.put<Document>(`${this.baseUrl}/${documentId}/metadata`, { metadata });
  }

  download(documentId: string): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/${documentId}/download`, { responseType: 'blob' });
  }

  preview(documentId: string): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/${documentId}/preview`, { responseType: 'blob' });
  }

  getVersions(documentId: string): Observable<VersionHistory[]> {
    return this.http.get<VersionHistory[]>(`${this.baseUrl}/${documentId}/versions`);
  }

  uploadNewVersion(documentId: string, file: File): Observable<HttpEvent<Document>> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<Document>(`${this.baseUrl}/${documentId}/versions`, formData, {
      reportProgress: true,
      observe: 'events',
    });
  }

  getVersion(documentId: string, versionNumber: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/${documentId}/versions/${versionNumber}`, { responseType: 'blob' });
  }

  softDelete(documentId: string, reason?: string): Observable<void> {
    let params = new HttpParams();
    if (reason) {
      params = params.set('reason', reason);
    }
    return this.http.delete<void>(`${this.baseUrl}/${documentId}`, { params });
  }

  hardDelete(documentId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${documentId}/hard`);
  }

  restore(documentId: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/${documentId}/restore`, {});
  }
}
