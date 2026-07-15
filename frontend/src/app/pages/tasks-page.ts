import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService } from '../core/api.service';
import { LanguageService } from '../core/language.service';
import { Task } from '../core/models';

/** Worklist of open user tasks. */
@Component({
  selector: 'app-tasks-page',
  imports: [RouterLink, DatePipe],
  template: `
    <div class="page-head">
      <h1>{{ lang.t('tasks.title') }}</h1>
      <button class="button" (click)="refresh()">{{ lang.t('tasks.refresh') }}</button>
    </div>
    @if (error()) {
      <p class="error">{{ error() }}</p>
    }
    <div class="card-list">
      @for (task of tasks(); track task.userTaskKey) {
        <div class="card">
          <div>
            <h2>{{ task.name }}</h2>
            <p class="muted">
              {{ task.processName ?? task.processDefinitionId }} ·
              {{ lang.t('tasks.created') }} {{ task.creationDate | date: 'short' }}
            </p>
          </div>
          <a class="button" [routerLink]="['/tasks', task.userTaskKey]">
            {{ lang.t('tasks.open') }}
          </a>
        </div>
      } @empty {
        <p class="muted">{{ lang.t('tasks.empty') }}</p>
      }
    </div>
  `,
})
export class TasksPage {
  private readonly api = inject(ApiService);
  protected readonly lang = inject(LanguageService);

  readonly tasks = signal<Task[]>([]);
  readonly error = signal<string | null>(null);

  constructor() {
    this.refresh();
  }

  refresh(): void {
    this.api
      .openTasks()
      .then((tasks) => this.tasks.set(tasks))
      .catch((e) => this.error.set(e?.error?.message ?? this.lang.t('tasks.loadFailed')));
  }
}
