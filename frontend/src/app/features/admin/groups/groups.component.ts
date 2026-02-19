import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { GroupService } from '../../../services/group.service';
import { Group } from '../../../models/group.model';

@Component({
  selector: 'app-groups',
  standalone: true,
  imports: [CommonModule],
  template: `
    <h2>Groups</h2>
    <ul>
      <li *ngFor="let group of groups()">{{ group.displayName }} (Parent: {{ group.parentGroupId ?? 'none' }})</li>
    </ul>
    <button>Create Group</button>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GroupsComponent {
  protected readonly groups = signal<Group[]>([]);

  constructor(private readonly groupService: GroupService) {
    this.groupService.list().subscribe((groups) => this.groups.set(groups));
  }
}
