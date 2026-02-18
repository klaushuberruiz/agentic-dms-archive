import { Component, ChangeDetectionStrategy } from '@angular/core';

@Component({
  selector: 'app-document-list',
  standalone: true,
  template: `<div class="document-list"></div>`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DocumentListComponent {
}
