import { Component, effect, inject, input, signal, viewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../core/api.service';
import { LanguageService } from '../core/language.service';
import { TaskDetail } from '../core/models';
import { settle } from '../core/settle';
import { FormViewer } from '../shared/form-viewer';

/** Renders a user task's Camunda Form (prefilled from variables) and completes it. */
@Component({
  selector: 'app-task-detail-page',
  imports: [FormViewer, RouterLink],
  template: `
    <p><a routerLink="/tasks">{{ lang.t('task.back') }}</a></p>
    @if (error()) {
      <p class="error">{{ error() }}</p>
    }
    @if (detail(); as d) {
      <h1>{{ d.task.name }}</h1>
      <p class="muted">{{ d.task.processName ?? d.task.processDefinitionId }}</p>
      @if (d.formSchema; as schema) {
        <app-form-viewer #form [schema]="schema" [data]="d.variables" />
      } @else {
        <h2>{{ lang.t('task.variables') }}</h2>
        <table>
          @for (entry of variableEntries(); track entry[0]) {
            <tr>
              <th>{{ entry[0] }}</th>
              <td>{{ entry[1] }}</td>
            </tr>
          }
        </table>
      }
      <button class="button" [disabled]="submitting()" (click)="complete()">
        {{ lang.t('task.complete') }}
      </button>
    }
  `,
})
export class TaskDetailPage {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);
  protected readonly lang = inject(LanguageService);

  // route param (withComponentInputBinding)
  readonly userTaskKey = input.required<string>();

  readonly detail = signal<TaskDetail | null>(null);
  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);

  private readonly formViewer = viewChild<FormViewer>('form');

  constructor() {
    // reloads whenever the language changes — the form schema is locale-aware
    effect(() => {
      this.lang.locale();
      this.load();
    });
  }

  variableEntries(): [string, unknown][] {
    return Object.entries(this.detail()?.variables ?? {});
  }

  private async load(): Promise<void> {
    try {
      this.detail.set(await this.api.taskDetail(Number(this.userTaskKey())));
    } catch (e: any) {
      this.error.set(e?.error?.message ?? this.lang.t('task.loadFailed'));
    }
  }

  async complete(): Promise<void> {
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
      await this.api.completeTask(Number(this.userTaskKey()), variables);
      // Camunda's secondary storage flushes asynchronously (~0.5s); without this
      // the task list would still contain the just-completed task.
      await settle();
      await this.router.navigate(['/tasks']);
    } catch (e: any) {
      this.error.set(e?.error?.message ?? this.lang.t('task.completeFailed'));
      this.submitting.set(false);
    }
  }
}
