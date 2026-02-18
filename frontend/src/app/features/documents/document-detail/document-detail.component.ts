import { Component, ChangeDetectionStrategy } from '@angular/core';

@Component({
  selector: 'app-document-detail',
  standalone: true,
  template: `<div class="document-detail"></div>`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DocumentDetailComponent {
}
