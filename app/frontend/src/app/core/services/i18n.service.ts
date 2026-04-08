import { Injectable, signal } from '@angular/core';

import { fr } from '../i18n/fr';
import { en } from '../i18n/en';

export type Language = 'fr' | 'en';

@Injectable({ providedIn: 'root' })
export class I18nService {

  // Attributes

  private readonly _STORAGE_KEY = 'ts_language';
  private readonly _dictionaries: Record<Language, Record<string, string>> = { fr, en };

  readonly currentLanguage = signal<Language>(this._loadLanguage());

  // Methods

  setLanguage(lang: Language): void {
    this.currentLanguage.set(lang);
    localStorage.setItem(this._STORAGE_KEY, lang);
  }

  translate(key: string): string {
    const dict = this._dictionaries[this.currentLanguage()];
    return dict[key] ?? key;
  }

  // Private methods

  private _loadLanguage(): Language {
    const stored = localStorage.getItem(this._STORAGE_KEY);
    return (stored === 'en' || stored === 'fr') ? stored : 'fr';
  }
}
