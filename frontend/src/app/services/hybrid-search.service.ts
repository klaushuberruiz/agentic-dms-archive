import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { HybridSearchPage, HybridSearchRequest } from '../models/hybrid-search.model';

@Injectable({ providedIn: 'root' })
export class HybridSearchService {
  private readonly baseUrl = `${environment.apiBaseUrl}/search-hybrid`;

  constructor(private readonly http: HttpClient) {}

  search(query: string, page = 0, pageSize = 20): Observable<HybridSearchPage> {
    return this.http.get<HybridSearchPage>(this.baseUrl, {
      params: { query, page, pageSize },
    });
  }

  searchPost(request: HybridSearchRequest, page = 0, pageSize = 20): Observable<HybridSearchPage> {
    return this.http.post<HybridSearchPage>(this.baseUrl, request, {
      params: { page, pageSize },
    });
  }
}
