import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { ApiService } from '../core/api.service';
import { ProcessInstance } from '../core/models';

/** Overview of process instances and their states. */
@Component({
  selector: 'app-processes-page',
  imports: [DatePipe],
  template: `
    <h1>Processes</h1>
    @if (error()) {
      <p class="error">{{ error() }}</p>
    }
    <table>
      <thead>
        <tr>
          <th>Process</th>
          <th>State</th>
          <th>Started</th>
          <th>Ended</th>
        </tr>
      </thead>
      <tbody>
        @for (pi of instances(); track pi.processInstanceKey) {
          <tr>
            <td>{{ pi.processDefinitionName ?? pi.processDefinitionId }}</td>
            <td>
              <span class="pill" [class.pill-active]="pi.state === 'ACTIVE'">{{ pi.state }}</span>
            </td>
            <td>{{ pi.startDate | date: 'short' }}</td>
            <td>{{ pi.endDate | date: 'short' }}</td>
          </tr>
        } @empty {
          <tr>
            <td colspan="4" class="muted">No process instances.</td>
          </tr>
        }
      </tbody>
    </table>
  `,
})
export class ProcessesPage {
  private readonly api = inject(ApiService);

  readonly instances = signal<ProcessInstance[]>([]);
  readonly error = signal<string | null>(null);

  constructor() {
    this.api
      .processInstances()
      .then((instances) => this.instances.set(instances))
      .catch((e) => this.error.set(e?.error?.message ?? 'Failed to load process instances'));
  }
}
