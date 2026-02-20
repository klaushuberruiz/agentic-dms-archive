import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Group } from '../models/group.model';

@Injectable({ providedIn: 'root' })
export class GroupService {
  private readonly baseUrl = `${environment.apiBaseUrl}/groups`;

  constructor(private readonly http: HttpClient) {}

  list(page = 0, pageSize = 20): Observable<any> {
    return this.http.get<any>(this.baseUrl, {
      params: { page, pageSize },
    });
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

  getMembers(groupId: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/${groupId}/members`);
  }
}
