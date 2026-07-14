import { inject, Injectable } from '@angular/core';
import Keycloak from 'keycloak-js';

/** Thin facade over the keycloak-js instance: identity, realm roles, logout. */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly keycloak = inject(Keycloak);

  get username(): string {
    return (this.keycloak.tokenParsed?.['preferred_username'] as string) ?? '';
  }

  get roles(): string[] {
    return this.keycloak.tokenParsed?.realm_access?.roles ?? [];
  }

  get isApplicant(): boolean {
    return this.roles.includes('applicant');
  }

  get isCivilServant(): boolean {
    return this.roles.includes('civil-servant');
  }

  /** Landing page for the user's role — also the redirect target for forbidden routes. */
  get homePath(): string {
    return this.isCivilServant && !this.isApplicant ? '/tasks' : '/services';
  }

  logout(): void {
    this.keycloak.logout({ redirectUri: window.location.origin });
  }
}
