import { Component, ChangeDetectionStrategy } from '@angular/core';

@Component({
  selector: 'app-search-bar',
  standalone: true,
  template: `<div class="search-bar"></div>`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SearchBarComponent {
}
