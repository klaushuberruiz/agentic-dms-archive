import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly tokenKey = 'dms.jwt';

  isAuthenticated(): boolean {
    return Boolean(this.getToken());
  }

  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  setToken(token: string): void {
    localStorage.setItem(this.tokenKey, token);
  }

  clearToken(): void {
    localStorage.removeItem(this.tokenKey);
  }

  hasRole(role: string): boolean {
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
