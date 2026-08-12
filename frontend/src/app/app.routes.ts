import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./auth/login-page').then(({ LoginPage }) => LoginPage),
  },
  {
    path: 'register',
    loadComponent: () => import('./auth/register-page').then(({ RegisterPage }) => RegisterPage),
  },
  {
    path: 'verify-email',
    loadComponent: () =>
      import('./auth/verify-email-page').then(({ VerifyEmailPage }) => VerifyEmailPage),
  },
  {
    path: 'me',
    loadComponent: () => import('./auth/me-page').then(({ MePage }) => MePage),
  },
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'login',
  },
  {
    path: '**',
    redirectTo: 'login',
  },
];
