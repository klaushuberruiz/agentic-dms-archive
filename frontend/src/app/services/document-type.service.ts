import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class DocumentTypeService {
  private readonly baseUrl = `${environment.apiBaseUrl}/document-types`;

  constructor(private readonly http: HttpClient) {}
}
