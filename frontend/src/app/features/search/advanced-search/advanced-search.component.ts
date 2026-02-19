import { ChangeDetectionStrategy, Component, inject, EventEmitter, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { SearchRequest } from '../../../models/search.model';

@Component({
  selector: 'app-advanced-search',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <form [formGroup]="form" (ngSubmit)="submit()">
      <input formControlName="dateFrom" type="date" />
      <input formControlName="dateTo" type="date" />
      <input formControlName="documentType" placeholder="Document type" />
      <input formControlName="metadataKey" placeholder="Metadata key" />
      <input formControlName="metadataValue" placeholder="Metadata value" />
      <button type="submit">Apply filters</button>
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
