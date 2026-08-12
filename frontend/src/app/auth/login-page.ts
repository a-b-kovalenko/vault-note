import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-login-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <main>
      <h1>Sign in to VaultNote</h1>
      <p>The login form will be added in the next step.</p>
    </main>
  `,
})
export class LoginPage {}
