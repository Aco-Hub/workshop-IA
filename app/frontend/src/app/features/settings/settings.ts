import { Component, inject } from '@angular/core';

import { ThemeService } from '../../core/services/theme.service';
import { I18nService } from '../../core/services/i18n.service';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './settings.html',
  styleUrl: './settings.scss',
})
export class SettingsComponent {

  // Attributes

  readonly theme = inject(ThemeService);
  readonly i18n = inject(I18nService);

  // Methods

  setLanguage(lang: 'fr' | 'en'): void {
    this.i18n.setLanguage(lang);
  }

  setTheme(t: 'light' | 'dark'): void {
    this.theme.setTheme(t);
  }
}
