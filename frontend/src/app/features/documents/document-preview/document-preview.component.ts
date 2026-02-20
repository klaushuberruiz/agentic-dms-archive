import { ChangeDetectionStrategy, Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { PdfViewerComponent } from '../../../shared/components/pdf-viewer/pdf-viewer.component';
import { DocumentService } from '../../../services/document.service';

@Component({
  selector: 'app-document-preview',
  standalone: true,
  imports: [CommonModule, PdfViewerComponent, TranslateModule],
  template: `
    <section>
      <h2>{{ 'documents.preview.title' | translate }}</h2>
      <app-pdf-viewer [src]="secureUrl()" />
    </section>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DocumentPreviewComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly documentService = inject(DocumentService);
  protected readonly secureUrl = signal('');

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      return;
    }
    this.documentService.preview(id).subscribe((blob) => {
      this.secureUrl.set(URL.createObjectURL(blob));
    });
  }

  ngOnDestroy(): void {
    const url = this.secureUrl();
    if (url) {
      URL.revokeObjectURL(url);
    }
  }
}
