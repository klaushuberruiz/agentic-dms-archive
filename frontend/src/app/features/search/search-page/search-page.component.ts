import { Component, ChangeDetectionStrategy } from '@angular/core';

@Component({
  selector: 'app-search-page',
  standalone: true,
  template: `<div class="search-page"></div>`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SearchPageComponent {
}
