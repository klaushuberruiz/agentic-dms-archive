import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';

@Component({
  selector: 'app-search-bar',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <form class="search-bar" (ngSubmit)="submit()">
      <input [formControl]="queryControl" [placeholder]="placeholder" />
      <button type="submit">Search</button>
    </form>
  `,
  styles: [
    `
      .search-bar {
        display: grid;
        grid-template-columns: 1fr auto;
        gap: 0.5rem;
      }
      input {
        min-width: 0;
        padding: 0.45rem 0.6rem;
      }
      button {
        padding: 0.45rem 0.8rem;
      }
    `,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SearchBarComponent {
  @Input() placeholder = 'Search documents';
  @Input() set query(value: string) {
    this.queryControl.setValue(value ?? '', { emitEvent: false });
  }
  @Output() readonly querySubmitted = new EventEmitter<string>();

  protected readonly queryControl = new FormControl('', { nonNullable: true });

  protected submit(): void {
    this.querySubmitted.emit(this.queryControl.value.trim());
  }
}
