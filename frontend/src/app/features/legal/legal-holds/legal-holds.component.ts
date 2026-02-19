import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';

@Component({
  selector: 'app-legal-holds',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <h2>Legal Holds</h2>
    <form [formGroup]="form">
      <input formControlName="caseReference" placeholder="Case reference" />
      <input formControlName="documentIds" placeholder="Document IDs (comma separated)" />
      <button type="button">Place Hold</button>
      <button type="button">Release Hold</button>
    </form>
    <p>Bulk hold operations supported through document IDs list.</p>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LegalHoldsComponent {
  private readonly fb = inject(FormBuilder);

  protected readonly form = this.fb.nonNullable.group({ caseReference: '', documentIds: '' });

  constructor() {}
}
