// Mirrors the backend DTOs in com.poc.backend.api.dto

export interface ProcessDefinition {
  processDefinitionKey: number;
  processDefinitionId: string;
  name: string;
  version: number;
}

export interface ProcessInstance {
  processInstanceKey: number;
  processDefinitionId: string;
  processDefinitionName: string | null;
  state: 'ACTIVE' | 'COMPLETED' | 'CANCELED' | string;
  startDate: string | null;
  endDate: string | null;
}

export interface Task {
  userTaskKey: number;
  name: string;
  elementId: string;
  processDefinitionId: string;
  processName: string | null;
  processInstanceKey: number;
  creationDate: string | null;
  state: string;
}

export interface TaskDetail {
  task: Task;
  formSchema: FormSchema | null;
  variables: Record<string, unknown>;
}

/** Camunda Form schema JSON (rendered by form-js). */
export type FormSchema = Record<string, unknown>;

export interface StartInstanceResponse {
  processInstanceKey: number;
}

export interface ApiError {
  status: number;
  message: string;
}
