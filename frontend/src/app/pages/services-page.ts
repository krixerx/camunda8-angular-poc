import { Component, effect, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService } from '../core/api.service';
import { LanguageService } from '../core/language.service';
import { ServiceCatalogItem } from '../core/models';

/**
 * Service catalog; starting one navigates to the start page. Cards show editorial
 * CMS content (title, summary) in the active language and fall back to engine
 * name/id when there is none.
 */
@Component({
  selector: 'app-services-page',
  imports: [RouterLink],
  template: `
    <h1>{{ lang.t('services.title') }}</h1>
    @if (error()) {
      <p class="error">{{ error() }}</p>
    }
    <div class="card-list">
      @for (item of services(); track item.processDefinitionKey) {
        <div class="card">
          <div>
            <h2>{{ item.title ?? item.name }}</h2>
            @if (item.summary) {
              <p>{{ item.summary }}</p>
            }
            @if (item.expectedDuration) {
              <p class="muted">{{ item.expectedDuration }}</p>
            }
            <p class="muted">{{ item.processDefinitionId }} · v{{ item.version }}</p>
          </div>
          <a
            class="button"
            [routerLink]="['/services', item.processDefinitionKey, item.processDefinitionId, 'start']"
          >
            {{ lang.t('services.start') }}
          </a>
        </div>
      } @empty {
        <p class="muted">{{ lang.t('services.empty') }}</p>
      }
    </div>
  `,
})
export class ServicesPage {
  private readonly api = inject(ApiService);
  protected readonly lang = inject(LanguageService);

  readonly services = signal<ServiceCatalogItem[]>([]);
  readonly error = signal<string | null>(null);

  constructor() {
    // re-fetches whenever the language changes — the catalog content is localized
    effect(() => {
      this.lang.locale();
      this.load();
    });
  }

  private load(): void {
    this.api
      .services()
      .then((items) => this.services.set(items))
      .catch((e) => this.error.set(e?.error?.message ?? this.lang.t('services.loadFailed')));
  }
}
