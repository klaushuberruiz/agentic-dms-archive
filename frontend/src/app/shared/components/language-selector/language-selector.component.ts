import { AsyncPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { I18nService } from '../../../core/services/i18n.service';

@Component({
  selector: 'app-language-selector',
  standalone: true,
  imports: [AsyncPipe, TranslateModule],
  template: `
    <label class="language-selector">
      <span>{{ 'common.language' | translate }}</span>
      <select [value]="currentLanguage$ | async" (change)="onLanguageChange($event)">
        @for (language of languages; track language.code) {
          <option [value]="language.code">{{ language.nativeName }}</option>
        }
      </select>
    </label>
  `,
  styles: [
    `
      .language-selector {
        display: inline-flex;
        align-items: center;
        gap: 0.5rem;
      }
    `,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LanguageSelectorComponent {
  private readonly i18nService = inject(I18nService);

  protected readonly languages = this.i18nService.supportedLanguages;
  protected readonly currentLanguage$ = this.i18nService.currentLanguage$;

  protected onLanguageChange(event: Event): void {
    const selectedLanguage = (event.target as HTMLSelectElement).value;
    this.i18nService.setLanguage(selectedLanguage).subscribe();
  }
}
