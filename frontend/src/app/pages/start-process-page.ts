import { Component, effect, inject, input, signal, viewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../core/api.service';
import { LanguageService } from '../core/language.service';
import { FormSchema, ServiceCatalogItem } from '../core/models';
import { settle } from '../core/settle';
import { FormViewer } from '../shared/form-viewer';

/**
 * Renders a process start form (if any) and creates the instance. Editorial catalog
 * content (title, instructions) is shown when the CMS has it; the page works without it.
 * Both the catalog content and the form schema arrive in the active language.
 */
@Component({
  selector: 'app-start-process-page',
  imports: [FormViewer, RouterLink],
  template: `
    <p><a routerLink="/services">{{ lang.t('start.back') }}</a></p>
    <h1>{{ lang.t('start.title') }} {{ catalogItem()?.title ?? processDefinitionId() }}</h1>
    @if (error()) {
      <p class="error">{{ error() }}</p>
    }
    @if (catalogItem(); as item) {
      @if (item.instructions) {
        <p class="instructions">{{ item.instructions }}</p>
      }
      @if (item.whatYouNeed) {
        <h3>{{ lang.t('start.whatYouNeed') }}</h3>
        <p class="instructions">{{ item.whatYouNeed }}</p>
      }
    }
    @if (loaded()) {
      @if (formSchema(); as schema) {
        <app-form-viewer #form [schema]="schema" />
      } @else {
        <p class="muted">{{ lang.t('start.noForm') }}</p>
      }
      <button class="button" [disabled]="submitting()" (click)="start()">
        {{ lang.t('start.submit') }}
      </button>
    }
  `,
  styles: `
    .instructions {
      white-space: pre-line;
    }
  `,
})
export class StartProcessPage {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);
  protected readonly lang = inject(LanguageService);

  // route params (withComponentInputBinding)
  readonly processDefinitionKey = input.required<string>();
  readonly processDefinitionId = input.required<string>();

  readonly formSchema = signal<FormSchema | null>(null);
  readonly catalogItem = signal<ServiceCatalogItem | null>(null);
  readonly loaded = signal(false);
  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);

  private readonly formViewer = viewChild<FormViewer>('form');

  constructor() {
    // reloads content and schema whenever the language changes
    effect(() => {
      this.lang.locale();
      this.load();
    });
  }

  private async load(): Promise<void> {
    // Editorial content is best-effort: a failed catalog fetch never blocks the form.
    this.api
      .services()
      .then((items) =>
        this.catalogItem.set(
          items.find((i) => i.processDefinitionId === this.processDefinitionId()) ?? null,
        ),
      )
      .catch(() => this.catalogItem.set(null));
    try {
      this.formSchema.set(await this.api.startForm(Number(this.processDefinitionKey())));
    } catch (e: any) {
      this.error.set(e?.error?.message ?? this.lang.t('start.formLoadFailed'));
    } finally {
      this.loaded.set(true);
    }
  }

  async start(): Promise<void> {
    const viewer = this.formViewer();
    let variables: Record<string, unknown> = {};
    if (viewer) {
      const data = viewer.submit();
      if (data === null) {
        return; // validation errors shown by form-js
      }
      variables = data;
    }
    this.submitting.set(true);
    this.error.set(null);
    try {
      await this.api.startProcess(this.processDefinitionId(), variables);
      // Camunda's secondary storage flushes asynchronously (~0.5s); without this
      // the task list would not yet contain the task created by the new instance.
      await settle();
      await this.router.navigate(['/tasks']);
    } catch (e: any) {
      this.error.set(e?.error?.message ?? this.lang.t('start.failed'));
      this.submitting.set(false);
    }
  }
}
