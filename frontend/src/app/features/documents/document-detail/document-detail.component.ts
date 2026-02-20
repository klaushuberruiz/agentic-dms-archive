import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { DocumentService } from '../../../services/document.service';
import { Document } from '../../../models/document.model';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-document-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, ReactiveFormsModule, TranslateModule],
  template: `
    <section class="page">
      <h2>{{ 'documents.detail.title' | translate }}</h2>
      @if (document()) {
      <p><strong>{{ 'documents.detail.id' | translate }}:</strong> {{ document()?.id }}</p>
      <p><strong>{{ 'documents.detail.version' | translate }}:</strong> {{ document()?.currentVersion }}</p>
      <p><strong>{{ 'documents.detail.createdBy' | translate }}:</strong> {{ document()?.createdBy }}</p>
      <p><strong>{{ 'documents.detail.createdAt' | translate }}:</strong> {{ document()?.createdAt | date: 'medium' }}</p>
      <pre>{{ document()?.metadata | json }}</pre>
      <form [formGroup]="metadataForm" (ngSubmit)="saveMetadata()">
        <textarea formControlName="metadataJson" rows="8"></textarea>
        <button type="submit">{{ 'documents.detail.saveMetadata' | translate }}</button>
      </form>
      <div class="actions">
        <button type="button" (click)="download()">{{ 'documents.detail.download' | translate }}</button>
        <a [routerLink]="['/documents', documentId(), 'preview']">{{ 'documents.detail.preview' | translate }}</a>
        <button type="button" (click)="softDelete()">{{ 'documents.detail.softDelete' | translate }}</button>
        <button type="button" (click)="restore()">{{ 'documents.detail.restore' | translate }}</button>
      </div>
      } @else {
      <p>{{ 'documents.detail.loading' | translate }}</p>
      }
    </section>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DocumentDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly documentService = inject(DocumentService);
  private readonly fb = inject(FormBuilder);

  protected readonly document = signal<Document | null>(null);
  protected readonly documentId = signal('');
  protected readonly metadataForm = this.fb.nonNullable.group({ metadataJson: '{}' });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      return;
    }
    this.documentId.set(id);
    this.documentService.getById(id).subscribe((doc) => {
      this.document.set(doc);
      this.metadataForm.patchValue({ metadataJson: JSON.stringify(doc.metadata ?? {}, null, 2) });
    });
  }

  protected saveMetadata(): void {
    const id = this.documentId();
    if (!id) {
      return;
    }
    const metadata = this.parseMetadata(this.metadataForm.getRawValue().metadataJson);
    this.documentService.updateMetadata(id, metadata).subscribe((doc) => {
      this.document.set(doc);
    });
  }

  protected download(): void {
    const id = this.documentId();
    if (!id) {
      return;
    }
    this.documentService.download(id).subscribe((blob) => {
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement('a');
      anchor.href = url;
      anchor.download = `${id}.pdf`;
      anchor.click();
      URL.revokeObjectURL(url);
    });
  }

  protected softDelete(): void {
    const id = this.documentId();
    if (!id) {
      return;
    }
    this.documentService.softDelete(id, 'User initiated').subscribe();
  }

  protected restore(): void {
    const id = this.documentId();
    if (!id) {
      return;
    }
    this.documentService.restore(id).subscribe();
  }

  private parseMetadata(raw: string): Record<string, unknown> {
    try {
      return JSON.parse(raw) as Record<string, unknown>;
    } catch {
      return {};
    }
  }
}
