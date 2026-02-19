import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';

@Component({
  selector: 'app-retrieval-audit',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <h2>Retrieval Audit Dashboard</h2>
    <form [formGroup]="filters">
      <input formControlName="from" type="date" />
      <input formControlName="to" type="date" />
      <input formControlName="user" placeholder="User" />
      <input formControlName="requirementId" placeholder="Requirement ID" />
    </form>
    <p>Tracks requirement retrieval frequency and audit events.</p>
    <p>Visible only to compliance officer or administrator roles.</p>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RetrievalAuditComponent {
  private readonly fb = inject(FormBuilder);

  protected readonly filters = this.fb.nonNullable.group({ from: '', to: '', user: '', requirementId: '' });

  constructor() {}
}
