import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { AuditService } from '../../../services/audit.service';
import { AuditLog } from '../../../models/audit.model';

@Component({
  selector: 'app-audit-logs',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TranslateModule],
  template: `
    <h2>{{ 'admin.auditLogs.title' | translate }}</h2>
    <form [formGroup]="form" (ngSubmit)="search()">
      <input formControlName="action" [attr.placeholder]="'common.action' | translate" />
      <input formControlName="userId" [attr.placeholder]="'common.user' | translate" />
      <button type="submit">{{ 'common.search' | translate }}</button>
    </form>
    <button (click)="export('csv')">{{ 'admin.auditLogs.exportCsv' | translate }}</button>
    <button (click)="export('json')">{{ 'admin.auditLogs.exportJson' | translate }}</button>
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
    this.auditService.exportLogs(undefined, undefined, format).subscribe((blob) => {
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `audit-logs.${format}`;
      a.click();
      URL.revokeObjectURL(url);
    });
  }
}
