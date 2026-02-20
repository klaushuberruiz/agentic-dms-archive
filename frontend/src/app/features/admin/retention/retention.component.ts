import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { RetentionCounts } from '../../../models/retention.model';
import { RetentionService } from '../../../services/retention.service';

@Component({
  selector: 'app-retention',
  standalone: true,
  imports: [CommonModule, TranslateModule],
  template: `
    <h2>{{ 'admin.retention.title' | translate }}</h2>
    <p>{{ 'admin.retention.expiredDocuments' | translate }}: {{ counts().expiredDocuments }}</p>
    <p>{{ 'admin.retention.documentsWithActiveLegalHolds' | translate }}: {{ counts().activeLegalHolds }}</p>
    <button type="button" (click)="refresh()">{{ 'common.refresh' | translate }}</button>
    <button type="button" (click)="trigger()">{{ 'admin.retention.runCleanup' | translate }}</button>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RetentionComponent {
  protected readonly counts = signal<RetentionCounts>({
    expiredDocuments: 0,
    activeLegalHolds: 0,
  });

  constructor(private readonly retentionService: RetentionService) {
    this.refresh();
  }

  protected refresh(): void {
    this.retentionService.getRetentionCounts().subscribe((counts) => this.counts.set(counts));
  }

  protected trigger(): void {
    this.retentionService.triggerRetentionCleanup().subscribe(() => this.refresh());
  }
}
