import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-document-list',
  standalone: true,
  template: `
    <section class="ads-card ads-grid">
      <h2 class="ads-section-title">Documents</h2>
      <div class="ads-grid ads-grid-2">
        <input class="ads-input" placeholder="Search by title, metadata or id" />
        <select class="ads-select">
          <option>All document types</option>
        </select>
      </div>
      <div class="ads-card">No documents available yet.</div>
    </section>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DocumentListComponent {}
