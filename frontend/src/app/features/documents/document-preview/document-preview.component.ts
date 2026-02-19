import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PdfViewerComponent } from '../../../shared/components/pdf-viewer/pdf-viewer.component';

@Component({
  selector: 'app-document-preview',
  standalone: true,
  imports: [CommonModule, PdfViewerComponent],
  template: `
    <section>
      <h2>Document Preview</h2>
      <app-pdf-viewer [src]="secureUrl" />
    </section>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DocumentPreviewComponent {
  @Input() secureUrl = '';
}
