import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService } from '../core/api.service';
import { ProcessDefinition } from '../core/models';

/** Catalog of deployed processes; starting one navigates to the start page. */
@Component({
  selector: 'app-services-page',
  imports: [RouterLink],
  template: `
    <h1>Services</h1>
    @if (error()) {
      <p class="error">{{ error() }}</p>
    }
    <div class="card-list">
      @for (def of definitions(); track def.processDefinitionKey) {
        <div class="card">
          <div>
            <h2>{{ def.name }}</h2>
            <p class="muted">{{ def.processDefinitionId }} · v{{ def.version }}</p>
          </div>
          <a
            class="button"
            [routerLink]="['/services', def.processDefinitionKey, def.processDefinitionId, 'start']"
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

  readonly definitions = signal<ProcessDefinition[]>([]);
  readonly error = signal<string | null>(null);

  constructor() {
    this.api
      .processDefinitions()
      .then((defs) => this.definitions.set(defs))
      .catch((e) => this.error.set(e?.error?.message ?? 'Failed to load process definitions'));
  }
}
