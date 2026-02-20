import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Document } from '../../../models/document.model';

@Component({
  selector: 'app-document-card',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <article class="document-card" *ngIf="document">
      <h3>{{ document.id }}</h3>
      <p><strong>Version:</strong> {{ document.currentVersion }}</p>
      <p><strong>Size:</strong> {{ document.fileSizeBytes }} bytes</p>
      <p><strong>Created:</strong> {{ document.createdAt | date: 'medium' }}</p>
      <a [routerLink]="['/documents', document.id]">Open</a>
    </article>
  `,
  styles: [
    `
      .document-card {
        border: 1px solid #e5e7eb;
        border-radius: 0.5rem;
        padding: 0.8rem;
      }
      h3 {
        margin: 0 0 0.5rem;
        word-break: break-all;
      }
      p {
        margin: 0.2rem 0;
      }
    `,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DocumentCardComponent {
  @Input({ required: true }) document!: Document;
}
