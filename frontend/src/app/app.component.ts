import { Component, ChangeDetectionStrategy } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { LanguageSelectorComponent } from './shared/components/language-selector/language-selector.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, TranslateModule, LanguageSelectorComponent],
  template: `
    <div class="app-shell">
      <header class="app-header">
        <h1>{{ 'app.title' | translate }}</h1>
        <app-language-selector />
      </header>
      <router-outlet />
    </div>
  `,
  styles: [
    `
      .app-shell {
        padding: 1rem;
      }

      .app-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 1rem;
      }

      h1 {
        margin: 0;
        font-size: 1.25rem;
      }
    `,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AppComponent {
}
