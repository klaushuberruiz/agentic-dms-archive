import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { DocumentTypeService } from '../../../services/document-type.service';
import { DocumentType } from '../../../models/document-type.model';

@Component({
  selector: 'app-document-types',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <h2>Document Types</h2>
    <form [formGroup]="form">
      <input formControlName="displayName" placeholder="Display name" />
      <textarea formControlName="metadataSchema" placeholder="JSON schema"></textarea>
      <input formControlName="retentionDays" type="number" placeholder="Retention days" />
      <input formControlName="allowedGroups" placeholder="Allowed groups (comma separated)" />
      <button type="button">Save</button>
    </form>
    <ul><li *ngFor="let type of types()">{{ type.displayName }}</li></ul>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DocumentTypesComponent {
  private readonly fb = inject(FormBuilder);

  protected readonly types = signal<DocumentType[]>([]);
  protected readonly form = this.fb.nonNullable.group({
    displayName: '',
    metadataSchema: '{}',
    retentionDays: 365,
    allowedGroups: '',
  });

  constructor(private readonly documentTypeService: DocumentTypeService) {
    this.documentTypeService.list().subscribe((types) => this.types.set(types));
  }
}
