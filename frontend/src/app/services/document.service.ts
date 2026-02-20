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
    return this.http.put<Document>(`${this.baseUrl}/${documentId}/metadata`, { metadata });
  }

  download(documentId: string): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/${documentId}/download`, { responseType: 'blob' });
  }

  preview(documentId: string): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/${documentId}/preview`, { responseType: 'blob' });
  }

  getVersions(documentId: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/${documentId}/versions`);
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
    const params = reason ? { reason } : {};
    return this.http.delete<void>(`${this.baseUrl}/${documentId}`, { params });
  }

  hardDelete(documentId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${documentId}/hard`);
  }

  restore(documentId: string): Observable<Document> {
    return this.http.post<Document>(`${this.baseUrl}/${documentId}/restore`, {});
  }
}
