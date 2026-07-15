import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { LanguageService } from './language.service';
import {
  FormSchema,
  ProcessDefinition,
  ProcessInstance,
  ServiceCatalogItem,
  StartInstanceResponse,
  Task,
  TaskDetail,
} from './models';

/** Typed client for the backend /api facade — the frontend's only data source. */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly language = inject(LanguageService);

  /** `locale` param for content-bearing endpoints; English is the parameterless default. */
  private localeParams(): Record<string, string> {
    const locale = this.language.locale();
    return locale === 'en' ? {} : { locale };
  }

  processDefinitions(): Promise<ProcessDefinition[]> {
    return firstValueFrom(this.http.get<ProcessDefinition[]>('/api/process-definitions'));
  }

  /** Service catalog: deployed definitions merged with editorial CMS content. */
  services(): Promise<ServiceCatalogItem[]> {
    return firstValueFrom(
      this.http.get<ServiceCatalogItem[]>('/api/services', { params: this.localeParams() }),
    );
  }

  /** Start form schema of a process definition, or null when it has none (HTTP 204). */
  startForm(processDefinitionKey: number): Promise<FormSchema | null> {
    return firstValueFrom(
      this.http.get<FormSchema | null>(`/api/process-definitions/${processDefinitionKey}/form`, {
        params: this.localeParams(),
      }),
    );
  }

  startProcess(
    processDefinitionId: string,
    variables: Record<string, unknown>,
  ): Promise<StartInstanceResponse> {
    return firstValueFrom(
      this.http.post<StartInstanceResponse>(`/api/process-definitions/${processDefinitionId}/start`, {
        variables,
      }),
    );
  }

  processInstances(): Promise<ProcessInstance[]> {
    return firstValueFrom(this.http.get<ProcessInstance[]>('/api/process-instances'));
  }

  openTasks(): Promise<Task[]> {
    return firstValueFrom(this.http.get<Task[]>('/api/tasks'));
  }

  taskDetail(userTaskKey: number): Promise<TaskDetail> {
    return firstValueFrom(
      this.http.get<TaskDetail>(`/api/tasks/${userTaskKey}`, { params: this.localeParams() }),
    );
  }

  completeTask(userTaskKey: number, variables: Record<string, unknown>): Promise<void> {
    return firstValueFrom(
      this.http.post<void>(`/api/tasks/${userTaskKey}/complete`, { variables }),
    );
  }
}
