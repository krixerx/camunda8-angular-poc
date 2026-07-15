import { TestBed } from '@angular/core/testing';
import { LanguageService } from './language.service';

describe('LanguageService', () => {
  beforeEach(() => {
    localStorage.removeItem('c8poc.lang');
    document.documentElement.dir = '';
    document.documentElement.lang = '';
  });

  afterEach(() => {
    localStorage.removeItem('c8poc.lang');
  });

  it('defaults to English LTR', () => {
    const service = TestBed.inject(LanguageService);
    TestBed.tick();
    expect(service.locale()).toBe('en');
    expect(document.documentElement.dir).toBe('ltr');
    expect(service.t('nav.services')).toBe('Services');
  });

  it('switching to Arabic flips direction and persists', () => {
    const service = TestBed.inject(LanguageService);
    service.set('ar');
    TestBed.tick();
    expect(document.documentElement.dir).toBe('rtl');
    expect(document.documentElement.lang).toBe('ar');
    expect(localStorage.getItem('c8poc.lang')).toBe('ar');
    expect(service.t('nav.services')).toBe('الخدمات');
  });

  it('restores the persisted language on startup', () => {
    localStorage.setItem('c8poc.lang', 'ar');
    const service = TestBed.inject(LanguageService);
    expect(service.locale()).toBe('ar');
  });
});
