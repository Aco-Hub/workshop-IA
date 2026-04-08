import { Component, signal, inject } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';

import { AuthService } from '../../../core/services/auth.service';
import { TranslatePipe } from '../../../shared/pipes/translate.pipe';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, TranslatePipe],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class LoginComponent {

  // Attributes

  private readonly _auth = inject(AuthService);
  private readonly _router = inject(Router);

  email = signal('');
  password = signal('');
  loading = signal(false);
  error = signal('');

  // Methods

  onSubmit(): void {
    if (!this.email() || !this.password()) {
      return;
    }
    this.loading.set(true);
    this.error.set('');

    this._auth.login({ email: this.email(), password: this.password() }).subscribe({
      next: () => {
        this.loading.set(false);
        this._router.navigate(['/calendar']);
      },
      error: () => {
        this.error.set('auth.invalid_credentials');
        this.loading.set(false);
      }
    });
  }
}
