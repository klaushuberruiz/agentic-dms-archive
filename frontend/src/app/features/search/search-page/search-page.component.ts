import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { SearchService } from '../../../services/search.service';
import { Document } from '../../../models/document.model';
import { AdvancedSearchComponent } from '../advanced-search/advanced-search.component';
import { SearchRequest } from '../../../models/search.model';
import { buildSearchRequest } from '../search-request.util';

@Component({
  selector: 'app-search-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, AdvancedSearchComponent, TranslateModule],
  template: `
    <section>
      <h2>{{ 'search.title' | translate }}</h2>
      <form [formGroup]="queryForm" (ngSubmit)="search()">
        <input formControlName="query" [attr.placeholder]="'search.searchText' | translate" />
        <button type="submit">{{ 'common.search' | translate }}</button>
      </form>
      <app-advanced-search (filtersChanged)="applyFilters($event)" />
      <ul>
        <li *ngFor="let doc of results()">
          <label><input type="checkbox" [value]="doc.id" (change)="toggleSelection(doc.id, $event)"/>{{ doc.id }}</label>
        </li>
      </ul>
      <button (click)="downloadSelected()">{{ 'search.bulkDownload' | translate }}</button>
    </section>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SearchPageComponent {
  private readonly fb = inject(FormBuilder);

  protected readonly results = signal<Document[]>([]);
  private filters: SearchRequest = {};
  private readonly selected = new Set<string>();

  protected readonly queryForm = this.fb.nonNullable.group({ query: '' });

  constructor(private readonly searchService: SearchService) {}

  protected applyFilters(filters: SearchRequest): void {
    this.filters = filters;
  }

  protected search(): void {
    const payload = buildSearchRequest(this.queryForm.value.query ?? '', this.filters);

    this.searchService.search(payload).subscribe((result) => this.results.set(result.results));
  }

  protected toggleSelection(documentId: string, event: Event): void {
    const checked = (event.target as HTMLInputElement).checked;
    if (checked) {
      this.selected.add(documentId);
      return;
    }
    this.selected.delete(documentId);
  }

  protected downloadSelected(): void {
    this.searchService.bulkDownload(Array.from(this.selected)).subscribe();
  }
}
