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
    path: 'forgot-password',
    loadComponent: () =>
      import('./auth/forgot-password-page').then(({ ForgotPasswordPage }) => ForgotPasswordPage),
  },
  {
    path: 'reset-password',
    loadComponent: () =>
      import('./auth/reset-password-page').then(({ ResetPasswordPage }) => ResetPasswordPage),
  },
  {
    path: 'verify-email',
    loadComponent: () =>
      import('./auth/verify-email-page').then(({ VerifyEmailPage }) => VerifyEmailPage),
  },
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'login',
  },
  {
    path: '',
    loadComponent: () =>
      import('./layout/authenticated-shell').then(({ AuthenticatedShell }) => AuthenticatedShell),
    children: [
      {
        path: 'me',
        loadComponent: () => import('./auth/me-page').then(({ MePage }) => MePage),
      },
    ],
  },
  {
    path: '**',
    redirectTo: 'login',
  },
];
