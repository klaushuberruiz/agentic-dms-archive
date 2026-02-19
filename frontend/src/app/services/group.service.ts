import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Group } from '../models/group.model';

@Injectable({ providedIn: 'root' })
export class GroupService {
  private readonly baseUrl = `${environment.apiBaseUrl}/groups`;

  constructor(private readonly http: HttpClient) {}

  list(): Observable<Group[]> {
    return this.http.get<Group[]>(this.baseUrl);
  }
}
