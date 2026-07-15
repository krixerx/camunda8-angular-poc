import { Injectable, effect, signal } from '@angular/core';
import { Locale, MESSAGES, MessageKey } from './i18n';

const STORAGE_KEY = 'c8poc.lang';

/**
 * Active UI language. Persists across reloads and keeps the document's `lang`/`dir`
 * in sync — `dir="rtl"` for Arabic is what flips the whole layout (the stylesheets
 * use logical properties, so no RTL-specific CSS is needed).
 */
@Injectable({ providedIn: 'root' })
export class LanguageService {
  readonly locale = signal<Locale>(readStoredLocale());

  constructor() {
    effect(() => {
      const locale = this.locale();
      localStorage.setItem(STORAGE_KEY, locale);
      document.documentElement.lang = locale;
      document.documentElement.dir = locale === 'ar' ? 'rtl' : 'ltr';
    });
  }

  set(locale: Locale): void {
    this.locale.set(locale);
  }

  /** Chrome string in the active language; reactive when called from a template. */
  t(key: MessageKey): string {
    return MESSAGES[key][this.locale()];
  }
}

function readStoredLocale(): Locale {
  return localStorage.getItem(STORAGE_KEY) === 'ar' ? 'ar' : 'en';
}
