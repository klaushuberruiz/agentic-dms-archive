import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { DocumentType } from '../models/document-type.model';

@Injectable({ providedIn: 'root' })
export class DocumentTypeService {
  private readonly baseUrl = `${environment.apiBaseUrl}/document-types`;

  constructor(private readonly http: HttpClient) {}

  list(): Observable<DocumentType[]> {
    return this.http.get<DocumentType[]>(this.baseUrl);
  }
}
