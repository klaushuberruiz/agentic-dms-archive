import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { DocumentService } from '../../../services/document.service';
import { Document } from '../../../models/document.model';

@Component({
  selector: 'app-document-list',
  standalone: true,
  imports: [CommonModule, RouterLink, TranslateModule],
  template: `
    <section class="dashboard">
      <header class="dashboard-header">
        <div>
          <h2>{{ 'documents.list.title' | translate }}</h2>
          <p>{{ 'documents.list.subtitle' | translate }}</p>
        </div>
        <a routerLink="/documents/upload" class="upload-link">{{ 'documents.list.uploadDocument' | translate }}</a>
      </header>

      @if (loading()) {
        <div class="status">{{ 'documents.list.loading' | translate }}</div>
      } @else if (error()) {
        <div class="status error">
          {{ error() }}
        </div>
      } @else if (documents().length === 0) {
        <div class="status empty">{{ 'documents.list.empty' | translate }}</div>
      } @else {
        <div class="cards">
          @for (doc of documents(); track doc.id) {
            <article class="card">
              <h3>{{ doc.id }}</h3>
              <p><strong>{{ 'documents.list.version' | translate }}:</strong> {{ doc.currentVersion }}</p>
              <p><strong>{{ 'documents.list.size' | translate }}:</strong> {{ doc.fileSizeBytes }} {{ 'documents.list.bytes' | translate }}</p>
              <p><strong>{{ 'documents.list.created' | translate }}:</strong> {{ doc.createdAt | date: 'medium' }}</p>
            </article>
          }
        </div>
      }
    </section>
  `,
  styles: [
    `
      .dashboard {
        padding: 1rem;
      }
      .dashboard-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        gap: 1rem;
        margin-bottom: 1rem;
      }
      .dashboard-header h2 {
        margin: 0 0 0.25rem 0;
      }
      .dashboard-header p {
        margin: 0;
        color: #4b5563;
      }
      .upload-link {
        text-decoration: none;
        background: #0f766e;
        color: #fff;
        padding: 0.5rem 0.9rem;
        border-radius: 0.4rem;
      }
      .status {
        padding: 0.9rem;
        border-radius: 0.4rem;
        background: #f3f4f6;
      }
      .status.error {
        background: #fee2e2;
        color: #991b1b;
      }
      .status.empty {
        background: #ecfeff;
        color: #155e75;
      }
      .cards {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
        gap: 0.9rem;
      }
      .card {
        border: 1px solid #e5e7eb;
        border-radius: 0.5rem;
        padding: 0.9rem;
        background: #fff;
      }
      .card h3 {
        margin: 0 0 0.5rem 0;
        font-size: 0.95rem;
        word-break: break-all;
      }
      .card p {
        margin: 0.2rem 0;
      }
    `,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DocumentListComponent implements OnInit {
  private readonly documentService = inject(DocumentService);
  private readonly translateService = inject(TranslateService);

  protected readonly documents = signal<Document[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.documentService.list(0, 20).subscribe({
      next: (result) => {
        this.documents.set(result.results ?? []);
        this.loading.set(false);
      },
      error: () => {
        this.error.set(this.translateService.instant('documents.list.errors.loadFailed'));
        this.loading.set(false);
      },
    });
  }
}
