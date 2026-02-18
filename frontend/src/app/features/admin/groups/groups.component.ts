import { Component, ChangeDetectionStrategy } from '@angular/core';

@Component({
  selector: 'app-groups',
  standalone: true,
  template: `<div class="groups"></div>`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GroupsComponent {
}
