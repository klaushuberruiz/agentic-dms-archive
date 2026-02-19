import { CanActivateFn } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

export const roleGuard: CanActivateFn = (route) => {
  const role = route.data['role'] as string | undefined;
  if (!role) {
    return true;
  }

  return inject(AuthService).hasRole(role);
};
