import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { TraceabilityItem } from '../../../models/governance.model';

@Component({
  selector: 'app-traceability',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <h2>Traceability Dashboard</h2>
    <form [formGroup]="filters">
      <input formControlName="module" placeholder="Module" />
      <input formControlName="approvalStatus" placeholder="Approval status" />
      <label><input type="checkbox" formControlName="missingOnly" />Missing implementation only</label>
    </form>
    <ul>
      <li *ngFor="let row of rows()">{{ row.requirementId }} → {{ row.codePath }} (missing: {{ !row.hasImplementation }})</li>
    </ul>
    <button (click)="exportCsv()">Export CSV</button>
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
