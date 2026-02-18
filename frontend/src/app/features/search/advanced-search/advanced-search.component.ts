import { Component, ChangeDetectionStrategy } from '@angular/core';

@Component({
  selector: 'app-advanced-search',
  standalone: true,
  template: `<div class="advanced-search"></div>`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdvancedSearchComponent {
}
