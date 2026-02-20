import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../environments/environment';
import { Group } from '../models/group.model';
import { PageResponse } from '../models/api.model';

@Injectable({ providedIn: 'root' })
export class GroupService {
  private readonly baseUrl = `${environment.apiBaseUrl}/groups`;

  constructor(private readonly http: HttpClient) {}

  list(page = 0, pageSize = 20): Observable<Group[]> {
    return this.http.get<PageResponse<Group> | Group[]>(this.baseUrl, {
      params: { page, pageSize },
    }).pipe(map((response) => (Array.isArray(response) ? response : (response.content ?? []))));
  }

  getById(groupId: string): Observable<Group> {
    return this.http.get<Group>(`${this.baseUrl}/${groupId}`);
  }

  getHierarchy(groupId: string): Observable<Group> {
    return this.http.get<Group>(`${this.baseUrl}/${groupId}/hierarchy`);
  }

  create(request: Partial<Group>): Observable<Group> {
    return this.http.post<Group>(this.baseUrl, request);
  }

  update(groupId: string, request: Partial<Group>): Observable<Group> {
    return this.http.put<Group>(`${this.baseUrl}/${groupId}`, request);
  }

  delete(groupId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${groupId}`);
  }

  addMember(groupId: string, userId: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/${groupId}/members/${userId}`, {});
  }

  removeMember(groupId: string, userId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${groupId}/members/${userId}`);
  }

  getMembers(groupId: string): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseUrl}/${groupId}/members`);
  }
}
