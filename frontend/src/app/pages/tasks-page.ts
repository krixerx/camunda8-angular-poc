import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService } from '../core/api.service';
import { Task } from '../core/models';

/** Worklist of open user tasks. */
@Component({
  selector: 'app-tasks-page',
  imports: [RouterLink, DatePipe],
  template: `
    <div class="page-head">
      <h1>Tasks</h1>
      <button class="button" (click)="refresh()">Refresh</button>
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
              created {{ task.creationDate | date: 'short' }}
            </p>
          </div>
          <a class="button" [routerLink]="['/tasks', task.userTaskKey]">Open</a>
        </div>
      } @empty {
        <p class="muted">No open tasks.</p>
      }
    </div>
  `,
})
export class TasksPage {
  private readonly api = inject(ApiService);

  readonly tasks = signal<Task[]>([]);
  readonly error = signal<string | null>(null);

  constructor() {
    this.refresh();
  }

  refresh(): void {
    this.api
      .openTasks()
      .then((tasks) => this.tasks.set(tasks))
      .catch((e) => this.error.set(e?.error?.message ?? 'Failed to load tasks'));
  }
}
