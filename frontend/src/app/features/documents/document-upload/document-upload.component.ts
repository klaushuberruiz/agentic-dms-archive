import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpEventType } from '@angular/common/http';
import { TranslateModule } from '@ngx-translate/core';
import { DragDropDirective } from '../../../shared/directives/drag-drop.directive';
import { DocumentService } from '../../../services/document.service';

@Component({
  selector: 'app-document-upload',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, DragDropDirective, TranslateModule],
  templateUrl: './document-upload.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DocumentUploadComponent {
  private readonly fb = inject(FormBuilder);

  protected readonly uploadProgress = signal(0);
  protected readonly selectedFileName = signal('');

  protected readonly form = this.fb.nonNullable.group({
    documentTypeId: ['', Validators.required],
    metadataJson: ['{}', Validators.required],
    file: [null as File | null, Validators.required],
  });

  constructor(private readonly documentService: DocumentService) {}

  protected onFileSelected(file: File): void {
    this.form.patchValue({ file });
    this.selectedFileName.set(file.name);
  }

  protected onFileInputChange(event: Event): void {
    const input = event.target as HTMLInputElement | null;
    const file = input?.files?.item(0);
    if (!file) {
      return;
    }
    this.onFileSelected(file);
  }

  protected upload(): void {
    if (this.form.invalid || !this.form.value.file) {
      this.form.markAllAsTouched();
      return;
    }
    const metadata = this.parseMetadata(this.form.value.metadataJson ?? '{}');
    this.documentService.upload({
      documentTypeId: this.form.value.documentTypeId ?? '',
      metadata,
    }, this.form.value.file).subscribe((event) => {
      if (event.type === HttpEventType.UploadProgress && event.total) {
        this.uploadProgress.set(Math.round((event.loaded * 100) / event.total));
      }

      if (event.type === HttpEventType.Response) {
        this.uploadProgress.set(100);
      }
    });
  }

  private parseMetadata(raw: string): Record<string, unknown> {
    try {
      return JSON.parse(raw) as Record<string, unknown>;
    } catch {
      return {};
    }
  }
}
