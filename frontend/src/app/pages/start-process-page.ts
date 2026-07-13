import { Component, inject, input, signal, viewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../core/api.service';
import { FormSchema } from '../core/models';
import { settle } from '../core/settle';
import { FormViewer } from '../shared/form-viewer';

/** Renders a process start form (if any) and creates the instance. */
@Component({
  selector: 'app-start-process-page',
  imports: [FormViewer, RouterLink],
  template: `
    <p><a routerLink="/services">← Services</a></p>
    <h1>Start: {{ processDefinitionId() }}</h1>
    @if (error()) {
      <p class="error">{{ error() }}</p>
    }
    @if (loaded()) {
      @if (formSchema(); as schema) {
        <app-form-viewer #form [schema]="schema" />
      } @else {
        <p class="muted">This process has no start form.</p>
      }
      <button class="button" [disabled]="submitting()" (click)="start()">Start process</button>
    }
  `,
})
export class StartProcessPage {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);

  // route params (withComponentInputBinding)
  readonly processDefinitionKey = input.required<string>();
  readonly processDefinitionId = input.required<string>();

  readonly formSchema = signal<FormSchema | null>(null);
  readonly loaded = signal(false);
  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);

  private readonly formViewer = viewChild<FormViewer>('form');

  constructor() {
    queueMicrotask(() => this.load());
  }

  private async load(): Promise<void> {
    try {
      this.formSchema.set(await this.api.startForm(Number(this.processDefinitionKey())));
    } catch (e: any) {
      this.error.set(e?.error?.message ?? 'Failed to load start form');
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
      this.error.set(e?.error?.message ?? 'Failed to start process');
      this.submitting.set(false);
    }
  }
}
