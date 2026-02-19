import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { AuditService } from '../../../services/audit.service';
import { AuditLog } from '../../../models/audit.model';

@Component({
  selector: 'app-audit-logs',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <h2>Audit Logs</h2>
    <form [formGroup]="form" (ngSubmit)="search()">
      <input formControlName="action" placeholder="Action" />
      <input formControlName="userId" placeholder="User" />
      <button type="submit">Search</button>
    </form>
    <button (click)="export('csv')">Export CSV</button>
    <button (click)="export('json')">Export JSON</button>
    <pre>{{ logs() | json }}</pre>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AuditLogsComponent {
  private readonly fb = inject(FormBuilder);

  protected readonly logs = signal<AuditLog[]>([]);
  protected readonly form = this.fb.nonNullable.group({ action: '', userId: '' });

  constructor(private readonly auditService: AuditService) {}

  protected search(): void {
    this.auditService.search(this.form.getRawValue()).subscribe((logs) => this.logs.set(logs));
  }

  protected export(format: 'csv' | 'json'): void {
    const blob = new Blob([JSON.stringify(this.logs())], { type: format === 'json' ? 'application/json' : 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `audit-logs.${format}`;
    a.click();
    URL.revokeObjectURL(url);
  }
}
