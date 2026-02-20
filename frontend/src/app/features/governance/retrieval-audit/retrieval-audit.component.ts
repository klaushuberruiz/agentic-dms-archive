import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-retrieval-audit',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TranslateModule],
  template: `
    <h2>{{ 'governance.retrievalAudit.title' | translate }}</h2>
    <form [formGroup]="filters">
      <input formControlName="from" type="date" />
      <input formControlName="to" type="date" />
      <input formControlName="user" [attr.placeholder]="'common.user' | translate" />
      <input formControlName="requirementId" [attr.placeholder]="'governance.retrievalAudit.requirementId' | translate" />
    </form>
    <p>{{ 'governance.retrievalAudit.description' | translate }}</p>
    <p>{{ 'governance.retrievalAudit.rolesInfo' | translate }}</p>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RetrievalAuditComponent {
  private readonly fb = inject(FormBuilder);

  protected readonly filters = this.fb.nonNullable.group({ from: '', to: '', user: '', requirementId: '' });

  constructor() {}
}
