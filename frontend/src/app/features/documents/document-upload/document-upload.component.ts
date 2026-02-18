import { Component, ChangeDetectionStrategy } from '@angular/core';

@Component({
  selector: 'app-document-upload',
  standalone: true,
  template: `<div class="document-upload"></div>`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DocumentUploadComponent {
}
