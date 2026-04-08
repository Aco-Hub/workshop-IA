import { Component, OnInit, inject } from '@angular/core';
import { RouterOutlet, Router, NavigationEnd } from '@angular/router';
import { CommonModule } from '@angular/common';
import { filter } from 'rxjs/operators';

import { AuthService } from './core/services/auth.service';
import { ThemeService } from './core/services/theme.service';
import { SidebarComponent } from './shared/components/sidebar/sidebar';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, SidebarComponent],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App implements OnInit {

  // Attributes

  private readonly _router = inject(Router);
  private readonly _auth = inject(AuthService);
  private readonly _theme = inject(ThemeService);

  showSidebar = false;

  // Lifecycle

  ngOnInit(): void {
    this._theme.setTheme(this._theme.currentTheme());

    this._router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe((event) => {
      const navEnd = event as NavigationEnd;
      const url = navEnd.urlAfterRedirects;
      this.showSidebar = !url.startsWith('/login') && !url.startsWith('/register');
    });

    const url = this._router.url;
    this.showSidebar = !url.startsWith('/login') && !url.startsWith('/register');
  }
}
