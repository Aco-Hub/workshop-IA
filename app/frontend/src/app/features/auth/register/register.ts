import { Component, OnInit, signal, inject } from '@angular/core';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';

import { AuthService } from '../../../core/services/auth.service';
import { TranslatePipe } from '../../../shared/pipes/translate.pipe';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [FormsModule, RouterLink, TranslatePipe],
  templateUrl: './register.html',
  styleUrl: './register.scss',
})
export class RegisterComponent implements OnInit {

  // Attributes

  private readonly _auth = inject(AuthService);
  private readonly _router = inject(Router);
  private readonly _route = inject(ActivatedRoute);

  inviteToken = signal('');
  username = signal('');
  password = signal('');
  title = signal('');
  discordLink = signal('');
  loading = signal(false);
  error = signal('');

  // Lifecycle

  ngOnInit(): void {
    const token = this._route.snapshot.queryParamMap.get('token') || '';
    this.inviteToken.set(token);
  }

  // Methods

  onSubmit(): void {
    if (!this.inviteToken()) {
      this.error.set('auth.invite_required');
      return;
    }
    if (!this.username() || !this.password()) {
      return;
    }

    this.loading.set(true);
    this.error.set('');

    this._auth.register({
      token: this.inviteToken(),
      username: this.username(),
      password: this.password(),
      title: this.title() || undefined,
      discordLink: this.discordLink() || undefined,
    }).subscribe({
      next: () => {
        this.loading.set(false);
        this._router.navigate(['/calendar']);
      },
      error: () => {
        this.error.set('auth.register_error');
        this.loading.set(false);
      }
    });
  }
}
