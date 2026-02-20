import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { SearchService } from '../../../services/search.service';
import { Document } from '../../../models/document.model';
import { AdvancedSearchComponent } from '../advanced-search/advanced-search.component';
import { SearchRequest } from '../../../models/search.model';
import { buildSearchRequest } from '../search-request.util';
import { SearchBarComponent } from '../../../shared/components/search-bar/search-bar.component';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';

@Component({
  selector: 'app-search-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, AdvancedSearchComponent, TranslateModule, SearchBarComponent, PaginationComponent],
  template: `
    <section>
      <h2>{{ 'search.title' | translate }}</h2>
      <app-search-bar [query]="queryForm.value.query ?? ''" [placeholder]="'search.searchText' | translate" (querySubmitted)="onQuerySubmitted($event)" />
      <app-advanced-search (filtersChanged)="applyFilters($event)" />
      <ul>
        <li *ngFor="let doc of results()">
          <label><input type="checkbox" [value]="doc.id" (change)="toggleSelection(doc.id, $event)"/>{{ doc.id }}</label>
        </li>
      </ul>
      <button (click)="downloadSelected()">{{ 'search.bulkDownload' | translate }}</button>
      <app-pagination [page]="page()" [totalPages]="totalPages()" (pageChanged)="onPageChanged($event)" />
    </section>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SearchPageComponent {
  private readonly fb = inject(FormBuilder);

  protected readonly results = signal<Document[]>([]);
  protected readonly page = signal(0);
  protected readonly totalPages = signal(1);
  private filters: SearchRequest = {};
  private readonly selected = new Set<string>();

  protected readonly queryForm = this.fb.nonNullable.group({ query: '' });

  constructor(private readonly searchService: SearchService) {}

  protected applyFilters(filters: SearchRequest): void {
    this.filters = filters;
  }

  protected search(): void {
    const payload = buildSearchRequest(this.queryForm.value.query ?? '', this.filters);
    payload.page = this.page();

    this.searchService.search(payload).subscribe((result) => {
      this.results.set(result.results);
      this.totalPages.set(result.totalPages);
    });
  }

  protected onQuerySubmitted(query: string): void {
    this.queryForm.patchValue({ query });
    this.page.set(0);
    this.search();
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

  protected onPageChanged(nextPage: number): void {
    this.page.set(nextPage);
    this.search();
  }
}
