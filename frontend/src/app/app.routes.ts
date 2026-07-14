import { Routes } from '@angular/router';
import { roleGuard } from './core/role.guard';
import { ProcessesPage } from './pages/processes-page';
import { ServicesPage } from './pages/services-page';
import { StartProcessPage } from './pages/start-process-page';
import { TaskDetailPage } from './pages/task-detail-page';
import { TasksPage } from './pages/tasks-page';

export const routes: Routes = [
  // Role homes: applicants land on /services; the guard bounces a civil
  // servant hitting /services over to /tasks (see AuthService.homePath).
  { path: '', pathMatch: 'full', redirectTo: 'services' },
  { path: 'services', component: ServicesPage, canActivate: [roleGuard], data: { role: 'applicant' } },
  {
    path: 'services/:processDefinitionKey/:processDefinitionId/start',
    component: StartProcessPage,
    canActivate: [roleGuard],
    data: { role: 'applicant' },
  },
  { path: 'tasks', component: TasksPage, canActivate: [roleGuard], data: { role: 'civil-servant' } },
  {
    path: 'tasks/:userTaskKey',
    component: TaskDetailPage,
    canActivate: [roleGuard],
    data: { role: 'civil-servant' },
  },
  { path: 'processes', component: ProcessesPage, canActivate: [roleGuard] },
];
