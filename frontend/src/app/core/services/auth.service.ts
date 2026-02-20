import { Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly tokenKey = 'dms.jwt';
  private token: string | null = null;

  isAuthenticated(): boolean {
    if (environment.disableAuth) {
      return true;
    }
    return Boolean(this.getToken());
  }

  getToken(): string | null {
    if (this.token) {
      return this.token;
    }

    const persistedToken = localStorage.getItem(this.tokenKey);
    if (persistedToken) {
      this.token = persistedToken;
    }

    return persistedToken;
  }

  setToken(token: string): void {
    this.token = token;
    localStorage.setItem(this.tokenKey, token);
  }

  clearToken(): void {
    this.token = null;
    localStorage.removeItem(this.tokenKey);
  }

  hasRole(role: string): boolean {
    if (environment.disableAuth) {
      return true;
    }

    const payload = this.getToken()?.split('.')[1];
    if (!payload) {
      return false;
    }

    try {
      const parsed = JSON.parse(atob(payload)) as { roles?: string[] };
      return Boolean(parsed.roles?.includes(role));
    } catch {
      return false;
    }
  }
}
