import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { LegalHoldService } from '../../../services/legal-hold.service';
import { LegalHold } from '../../../models/legal-hold.model';

@Component({
  selector: 'app-legal-holds',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TranslateModule],
  template: `
    <h2>{{ 'legal.holds.title' | translate }}</h2>
    <form [formGroup]="form" (ngSubmit)="placeHold()">
      <input formControlName="caseReference" [attr.placeholder]="'legal.holds.caseReference' | translate" />
      <input formControlName="documentId" [attr.placeholder]="'legal.holds.documentId' | translate" />
      <input formControlName="reason" [attr.placeholder]="'legal.holds.reason' | translate" />
      <button type="submit">{{ 'legal.holds.placeHold' | translate }}</button>
      <button type="button" (click)="refresh()">{{ 'common.refresh' | translate }}</button>
    </form>
    <ul>
      <li *ngFor="let hold of holds()">
        {{ hold.caseReference }} - {{ hold.documentId }}
        <button type="button" (click)="release(hold.id)">{{ 'legal.holds.release' | translate }}</button>
      </li>
    </ul>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LegalHoldsComponent {
  private readonly fb = inject(FormBuilder);
  private readonly legalHoldService = inject(LegalHoldService);

  protected readonly holds = signal<LegalHold[]>([]);
  protected readonly form = this.fb.nonNullable.group({
    caseReference: '',
    documentId: '',
    reason: '',
  });

  constructor() {
    this.refresh();
  }

  protected placeHold(): void {
    const value = this.form.getRawValue();
    if (!value.documentId) {
      return;
    }
    this.legalHoldService.placeLegalHold(value.documentId, value.caseReference, value.reason).subscribe(() => {
      this.refresh();
    });
  }

  protected release(holdId: string): void {
    this.legalHoldService.releaseLegalHold(holdId, 'Released from UI').subscribe(() => this.refresh());
  }

  protected refresh(): void {
    this.legalHoldService.getActiveLegalHolds().subscribe((holds) => this.holds.set(holds));
  }
}
