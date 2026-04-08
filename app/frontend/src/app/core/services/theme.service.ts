import { Injectable, signal } from '@angular/core';

export type Theme = 'light' | 'dark';

@Injectable({ providedIn: 'root' })
export class ThemeService {

  // Attributes

  private readonly _STORAGE_KEY = 'ts_theme';

  readonly currentTheme = signal<Theme>(this._loadTheme());

  // Constructor

  constructor() {
    this._applyTheme(this.currentTheme());
  }

  // Methods

  setTheme(theme: Theme): void {
    this.currentTheme.set(theme);
    localStorage.setItem(this._STORAGE_KEY, theme);
    this._applyTheme(theme);
  }

  toggleTheme(): void {
    this.setTheme(this.currentTheme() === 'light' ? 'dark' : 'light');
  }

  // Private methods

  private _loadTheme(): Theme {
    const stored = localStorage.getItem(this._STORAGE_KEY);
    return (stored === 'dark' || stored === 'light') ? stored : 'light';
  }

  private _applyTheme(theme: Theme): void {
    document.documentElement.setAttribute('data-theme', theme);
  }
}
