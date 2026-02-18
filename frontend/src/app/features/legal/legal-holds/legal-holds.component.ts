import { Component, ChangeDetectionStrategy } from '@angular/core';

@Component({
  selector: 'app-legal-holds',
  standalone: true,
  template: `<div class="legal-holds"></div>`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LegalHoldsComponent {
}
