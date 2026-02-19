import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RequirementVersionHistoryItem } from '../../../models/governance.model';

@Component({
  selector: 'app-version-history',
  standalone: true,
  imports: [CommonModule],
  template: `
    <h2>Requirement Version History</h2>
    <table>
      <tr><th>ID</th><th>Version</th><th>Status</th><th>Modified</th><th>By</th><th>Summary</th></tr>
      <tr *ngFor="let item of rows()">
        <td>{{ item.requirementId }}</td><td>{{ item.version }}</td><td>{{ item.approvalStatus }}</td>
        <td>{{ item.modifiedAt }}</td><td>{{ item.modifiedBy }}</td><td>{{ item.changeSummary }}</td>
      </tr>
    </table>
    <button (click)="exportCsv()">Export CSV</button>
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
