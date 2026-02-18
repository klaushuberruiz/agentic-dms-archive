import { Component, ChangeDetectionStrategy } from '@angular/core';

@Component({
  selector: 'app-audit-logs',
  standalone: true,
  template: `<div class="audit-logs"></div>`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AuditLogsComponent {
}
