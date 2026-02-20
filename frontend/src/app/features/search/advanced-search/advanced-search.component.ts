import { ChangeDetectionStrategy, Component, inject, EventEmitter, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { SearchRequest } from '../../../models/search.model';

@Component({
  selector: 'app-advanced-search',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TranslateModule],
  template: `
    <form [formGroup]="form" (ngSubmit)="submit()">
      <input formControlName="dateFrom" type="date" />
      <input formControlName="dateTo" type="date" />
      <input formControlName="documentType" [attr.placeholder]="'search.documentType' | translate" />
      <input formControlName="metadataKey" [attr.placeholder]="'search.metadataKey' | translate" />
      <input formControlName="metadataValue" [attr.placeholder]="'search.metadataValue' | translate" />
      <button type="submit">{{ 'search.applyFilters' | translate }}</button>
    </form>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdvancedSearchComponent {
  private readonly fb = inject(FormBuilder);

  @Output() filtersChanged = new EventEmitter<SearchRequest>();

  protected readonly form = this.fb.nonNullable.group({
    dateFrom: '',
    dateTo: '',
    documentType: '',
    metadataKey: '',
    metadataValue: '',
  });

  constructor() {}

  protected submit(): void {
    const { metadataKey, metadataValue, ...rest } = this.form.getRawValue();
    const metadata = metadataKey ? { [metadataKey]: metadataValue } : undefined;
    this.filtersChanged.emit({ ...rest, metadata });
  }
}
