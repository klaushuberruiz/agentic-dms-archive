import { ChangeDetectionStrategy, Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-retention',
  standalone: true,
  imports: [CommonModule],
  template: `
    <h2>Retention Management</h2>
    <p>Documents approaching expiration will be listed here.</p>
    <p>Retention policy overview per document type.</p>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RetentionComponent {}
