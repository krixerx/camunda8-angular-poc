import { provideRouter } from '@angular/router';
import { TestBed } from '@angular/core/testing';
import Keycloak from 'keycloak-js';
import { App } from './app';

const keycloakStub = {
  tokenParsed: {
    preferred_username: 'bart',
    realm_access: { roles: ['applicant'] },
  },
  logout: () => Promise.resolve(),
} as unknown as Keycloak;

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([]), { provide: Keycloak, useValue: keycloakStub }],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should render the top navigation', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.brand')?.textContent).toContain('Camunda 8 POC');
  });

  it('should show the logged-in user with role-gated nav', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.user-name')?.textContent).toContain('bart');
    const links = Array.from(compiled.querySelectorAll('nav a')).map((a) => a.textContent?.trim());
    expect(links).toContain('Services');
    expect(links).not.toContain('Tasks');
  });
});
