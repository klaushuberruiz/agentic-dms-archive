import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class HybridSearchService {
  private readonly baseUrl = `${environment.apiBaseUrl}/search-hybrid`;

  constructor(private readonly http: HttpClient) {}

  search(query: string, page = 0, pageSize = 20): Observable<any> {
    return this.http.get<any>(this.baseUrl, {
      params: { query, page, pageSize },
    });
  }

  searchPost(request: any, page = 0, pageSize = 20): Observable<any> {
    return this.http.post<any>(this.baseUrl, request, {
      params: { page, pageSize },
    });
  }
}
