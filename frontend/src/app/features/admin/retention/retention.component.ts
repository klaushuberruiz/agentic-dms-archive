import { Component, ChangeDetectionStrategy } from '@angular/core';

@Component({
  selector: 'app-retention',
  standalone: true,
  template: `<div class="retention"></div>`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RetentionComponent {
}
