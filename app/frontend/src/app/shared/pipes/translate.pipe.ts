import { Pipe, PipeTransform, inject } from '@angular/core';

import { I18nService } from '../../core/services/i18n.service';

@Pipe({
  name: 'translate',
  standalone: true,
  pure: false,
})
export class TranslatePipe implements PipeTransform {

  // Attributes

  private readonly _i18n = inject(I18nService);

  // Methods

  transform(key: string): string {
    return this._i18n.translate(key);
  }
}
