import { ChangeDetectionStrategy, Component } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-unauthorized',
  standalone: true,
  imports: [TranslateModule],
  template: `
    <section>
      <h2>{{ 'unauthorized.title' | translate }}</h2>
      <p>{{ 'unauthorized.message' | translate }}</p>
    </section>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UnauthorizedComponent {}
