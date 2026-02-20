import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { RequirementVersionHistoryItem } from '../../../models/governance.model';

@Component({
  selector: 'app-version-history',
  standalone: true,
  imports: [CommonModule, TranslateModule],
  template: `
    <h2>{{ 'governance.versionHistory.title' | translate }}</h2>
    <table>
      <tr>
        <th>{{ 'governance.versionHistory.id' | translate }}</th>
        <th>{{ 'governance.versionHistory.version' | translate }}</th>
        <th>{{ 'governance.versionHistory.status' | translate }}</th>
        <th>{{ 'governance.versionHistory.modified' | translate }}</th>
        <th>{{ 'governance.versionHistory.by' | translate }}</th>
        <th>{{ 'governance.versionHistory.summary' | translate }}</th>
      </tr>
      <tr *ngFor="let item of rows()">
        <td>{{ item.requirementId }}</td><td>{{ item.version }}</td><td>{{ item.approvalStatus }}</td>
        <td>{{ item.modifiedAt }}</td><td>{{ item.modifiedBy }}</td><td>{{ item.changeSummary }}</td>
      </tr>
    </table>
    <button (click)="exportCsv()">{{ 'governance.versionHistory.exportCsv' | translate }}</button>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class VersionHistoryComponent {
  protected readonly rows = signal<RequirementVersionHistoryItem[]>([]);

  protected exportCsv(): void {
    const csv = this.rows().map((row) => Object.values(row).join(',')).join('\n');
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv' }));
    const a = document.createElement('a');
    a.href = url;
    a.download = 'requirement-version-history.csv';
    a.click();
    URL.revokeObjectURL(url);
  }
}
