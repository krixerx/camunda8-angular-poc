import { Routes } from '@angular/router';
import { ProcessesPage } from './pages/processes-page';
import { ServicesPage } from './pages/services-page';
import { StartProcessPage } from './pages/start-process-page';
import { TaskDetailPage } from './pages/task-detail-page';
import { TasksPage } from './pages/tasks-page';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'services' },
  { path: 'services', component: ServicesPage },
  {
    path: 'services/:processDefinitionKey/:processDefinitionId/start',
    component: StartProcessPage,
  },
  { path: 'tasks', component: TasksPage },
  { path: 'tasks/:userTaskKey', component: TaskDetailPage },
  { path: 'processes', component: ProcessesPage },
];
