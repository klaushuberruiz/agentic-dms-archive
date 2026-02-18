import { Component, ChangeDetectionStrategy } from '@angular/core';

@Component({
  selector: 'app-pdf-viewer',
  standalone: true,
  template: `<div class="pdf-viewer"></div>`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PdfViewerComponent {
}
