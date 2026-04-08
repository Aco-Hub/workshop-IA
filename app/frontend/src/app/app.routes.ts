import { Routes } from '@angular/router';

import { LoginComponent } from './features/auth/login/login';
import { RegisterComponent } from './features/auth/register/register';
import { CalendarComponent } from './features/calendar/calendar';
import { ProjectDetailComponent } from './features/project/project-detail';
import { ClientDetailComponent } from './features/client/client-detail';
import { SettingsComponent } from './features/settings/settings';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  {
    path: '',
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'calendar', pathMatch: 'full' },
      { path: 'calendar', component: CalendarComponent },
      { path: 'calendar/:developerId', component: CalendarComponent },
      { path: 'project/:id', component: ProjectDetailComponent },
      { path: 'client/:id', component: ClientDetailComponent },
      { path: 'settings', component: SettingsComponent },
    ]
  },
  { path: '**', redirectTo: '' }
];
