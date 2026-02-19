import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

@Component({
  selector: 'app-pdf-viewer',
  standalone: true,
  template: `<iframe class="pdf-viewer" [src]="src" title="PDF preview"></iframe>`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PdfViewerComponent {
  @Input() src = '';
}
