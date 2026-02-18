import { Component, ChangeDetectionStrategy } from '@angular/core';

@Component({
  selector: 'app-document-preview',
  standalone: true,
  template: `<div class="document-preview"></div>`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DocumentPreviewComponent {
}
