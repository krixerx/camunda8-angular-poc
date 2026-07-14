import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, RouterStateSnapshot, UrlTree } from '@angular/router';
import { Router } from '@angular/router';
import { AuthGuardData, createAuthGuard } from 'keycloak-angular';
import { AuthService } from './auth.service';

/**
 * Route guard driven by `data.role` (realm role). Routes without `data.role` only require
 * authentication. Forbidden navigation redirects to the user's role home instead of erroring.
 */
const isAccessAllowed = async (
  route: ActivatedRouteSnapshot,
  _state: RouterStateSnapshot,
  authData: AuthGuardData,
): Promise<boolean | UrlTree> => {
  const { authenticated, grantedRoles } = authData;
  if (!authenticated) {
    return false; // onLoad: 'login-required' makes this unreachable in practice
  }
  const requiredRole = route.data['role'] as string | undefined;
  if (!requiredRole || grantedRoles.realmRoles.includes(requiredRole)) {
    return true;
  }
  return inject(Router).parseUrl(inject(AuthService).homePath);
};

export const roleGuard = createAuthGuard<CanActivateFn>(isAccessAllowed);
