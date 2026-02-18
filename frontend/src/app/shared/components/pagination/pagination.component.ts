import { Component, ChangeDetectionStrategy } from '@angular/core';

@Component({
  selector: 'app-pagination',
  standalone: true,
  template: `<div class="pagination"></div>`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaginationComponent {
}
