import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class SearchService {
  private readonly baseUrl = `${environment.apiBaseUrl}/search`;

  constructor(private readonly http: HttpClient) {}
}
