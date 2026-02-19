import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { SearchRequest, SearchResult } from '../models/search.model';

@Injectable({ providedIn: 'root' })
export class SearchService {
  private readonly baseUrl = `${environment.apiBaseUrl}/search`;

  constructor(private readonly http: HttpClient) {}

  search(request: SearchRequest): Observable<SearchResult> {
    return this.http.post<SearchResult>(this.baseUrl, request);
  }

  bulkDownload(documentIds: string[]): Observable<Blob> {
    return this.http.post(`${this.baseUrl}/bulk-download`, { documentIds }, { responseType: 'blob' });
  }
}
