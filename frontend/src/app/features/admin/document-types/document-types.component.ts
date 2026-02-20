import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { DocumentTypeService } from '../../../services/document-type.service';
import { DocumentType } from '../../../models/document-type.model';

@Component({
  selector: 'app-document-types',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TranslateModule],
  template: `
    <h2>{{ 'admin.documentTypes.title' | translate }}</h2>
    <form [formGroup]="form" (ngSubmit)="save()">
      <input formControlName="name" [attr.placeholder]="'admin.documentTypes.name' | translate" />
      <input formControlName="displayName" [attr.placeholder]="'admin.documentTypes.displayName' | translate" />
      <textarea formControlName="metadataSchema" [attr.placeholder]="'admin.documentTypes.jsonSchema' | translate"></textarea>
      <input formControlName="retentionDays" type="number" [attr.placeholder]="'admin.documentTypes.retentionDays' | translate" />
      <input formControlName="allowedGroups" [attr.placeholder]="'admin.documentTypes.allowedGroups' | translate" />
      <button type="submit">{{ 'common.save' | translate }}</button>
    </form>
    <ul><li *ngFor="let type of types()">{{ type.displayName }}</li></ul>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DocumentTypesComponent {
  private readonly fb = inject(FormBuilder);

  protected readonly types = signal<DocumentType[]>([]);
  protected readonly form = this.fb.nonNullable.group({
    name: '',
    displayName: '',
    metadataSchema: '{}',
    retentionDays: 365,
    allowedGroups: '',
  });

  constructor(private readonly documentTypeService: DocumentTypeService) {
    this.documentTypeService.list().subscribe((types) => this.types.set(types));
  }

  protected save(): void {
    const value = this.form.getRawValue();
    this.documentTypeService.create({
      name: value.name,
      displayName: value.displayName,
      metadataSchema: this.tryParse(value.metadataSchema),
      retentionDays: value.retentionDays,
      allowedGroups: value.allowedGroups.split(',').map((item) => item.trim()).filter((item) => item.length > 0),
      active: true,
    }).subscribe((created) => this.types.update((current) => [created, ...current]));
  }

  private tryParse(raw: string): Record<string, unknown> {
    try {
      return JSON.parse(raw) as Record<string, unknown>;
    } catch {
      return {};
    }
  }
}
