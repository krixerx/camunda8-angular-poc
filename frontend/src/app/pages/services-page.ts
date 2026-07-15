import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService } from '../core/api.service';
import { ServiceCatalogItem } from '../core/models';

/**
 * Service catalog; starting one navigates to the start page. Cards show editorial
 * CMS content (title, summary) and fall back to engine name/id when there is none.
 */
@Component({
  selector: 'app-services-page',
  imports: [RouterLink],
  template: `
    <h1>Services</h1>
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
            Start
          </a>
        </div>
      } @empty {
        <p class="muted">No processes deployed.</p>
      }
    </div>
  `,
})
export class ServicesPage {
  private readonly api = inject(ApiService);

  readonly services = signal<ServiceCatalogItem[]>([]);
  readonly error = signal<string | null>(null);

  constructor() {
    this.api
      .services()
      .then((items) => this.services.set(items))
      .catch((e) => this.error.set(e?.error?.message ?? 'Failed to load services'));
  }
}
