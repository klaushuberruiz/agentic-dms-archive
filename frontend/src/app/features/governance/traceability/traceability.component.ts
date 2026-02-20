import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { TraceabilityItem } from '../../../models/governance.model';

@Component({
  selector: 'app-traceability',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TranslateModule],
  template: `
    <h2>{{ 'governance.traceability.title' | translate }}</h2>
    <form [formGroup]="filters">
      <input formControlName="module" [attr.placeholder]="'common.module' | translate" />
      <input formControlName="approvalStatus" [attr.placeholder]="'governance.traceability.approvalStatus' | translate" />
      <label><input type="checkbox" formControlName="missingOnly" />{{ 'governance.traceability.missingOnly' | translate }}</label>
    </form>
    <ul>
      <li *ngFor="let row of rows()">{{ row.requirementId }} -> {{ row.codePath }} ({{ 'governance.traceability.missing' | translate }}: {{ !row.hasImplementation }})</li>
    </ul>
    <button (click)="exportCsv()">{{ 'governance.traceability.exportCsv' | translate }}</button>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TraceabilityComponent {
  private readonly fb = inject(FormBuilder);

  protected readonly rows = signal<TraceabilityItem[]>([]);
  protected readonly filters = this.fb.nonNullable.group({ module: '', approvalStatus: '', missingOnly: false });

  constructor() {}

  protected exportCsv(): void {
    const csv = this.rows().map((row) => Object.values(row).join(',')).join('\n');
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv' }));
    const a = document.createElement('a');
    a.href = url;
    a.download = 'traceability.csv';
    a.click();
    URL.revokeObjectURL(url);
  }
}
