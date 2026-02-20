import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-pdf-viewer',
  standalone: true,
  imports: [TranslateModule],
  template: `<iframe class="pdf-viewer" [src]="src" [title]="'documents.preview.iframeTitle' | translate"></iframe>`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PdfViewerComponent {
  @Input() src = '';
}
