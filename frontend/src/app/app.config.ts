import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import {
  AutoRefreshTokenService,
  createInterceptorCondition,
  INCLUDE_BEARER_TOKEN_INTERCEPTOR_CONFIG,
  IncludeBearerTokenCondition,
  includeBearerTokenInterceptor,
  provideKeycloak,
  UserActivityService,
  withAutoRefreshToken,
} from 'keycloak-angular';

import { routes } from './app.routes';

// SPA calls /api same-origin (nginx proxy in Docker, ng-serve proxy in dev),
// so the bearer condition matches the relative URL.
const apiCondition = createInterceptorCondition<IncludeBearerTokenCondition>({
  urlPattern: /^\/api(\/.*)?$/i,
  bearerPrefix: 'Bearer',
});

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    // Whole app is behind login (citizen portal semantics): unauthenticated
    // visitors bounce straight to the Keycloak login page.
    provideKeycloak({
      config: {
        url: 'http://localhost:8180',
        realm: 'camunda-poc',
        clientId: 'poc-frontend',
      },
      initOptions: {
        onLoad: 'login-required',
        pkceMethod: 'S256',
      },
      features: [withAutoRefreshToken({ onInactivityTimeout: 'logout', sessionTimeout: 300000 })],
      providers: [AutoRefreshTokenService, UserActivityService],
    }),
    { provide: INCLUDE_BEARER_TOKEN_INTERCEPTOR_CONFIG, useValue: [apiCondition] },
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withFetch(), withInterceptors([includeBearerTokenInterceptor])),
  ],
};
