import { Component, ChangeDetectionStrategy } from '@angular/core';

@Component({
  selector: 'app-document-card',
  standalone: true,
  template: `<div class="document-card"></div>`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DocumentCardComponent {
}
