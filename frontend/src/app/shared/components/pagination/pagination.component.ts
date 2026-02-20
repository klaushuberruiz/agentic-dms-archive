import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-pagination',
  standalone: true,
  imports: [CommonModule],
  template: `
    <nav class="pagination" *ngIf="totalPages > 1">
      <button type="button" [disabled]="page <= 0" (click)="change(page - 1)">Prev</button>
      <span>Page {{ page + 1 }} / {{ totalPages }}</span>
      <button type="button" [disabled]="page >= totalPages - 1" (click)="change(page + 1)">Next</button>
    </nav>
  `,
  styles: [
    `
      .pagination {
        display: flex;
        align-items: center;
        gap: 0.75rem;
      }
    `,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaginationComponent {
  @Input() page = 0;
  @Input() totalPages = 1;
  @Output() readonly pageChanged = new EventEmitter<number>();

  protected change(nextPage: number): void {
    this.pageChanged.emit(nextPage);
  }
}
