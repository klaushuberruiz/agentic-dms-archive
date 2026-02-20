import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { GroupService } from '../../../services/group.service';
import { Group } from '../../../models/group.model';

@Component({
  selector: 'app-groups',
  standalone: true,
  imports: [CommonModule, TranslateModule, ReactiveFormsModule],
  template: `
    <h2>{{ 'admin.groups.title' | translate }}</h2>
    <form [formGroup]="form" (ngSubmit)="create()">
      <input formControlName="name" [attr.placeholder]="'admin.groups.name' | translate" />
      <input formControlName="displayName" [attr.placeholder]="'admin.documentTypes.displayName' | translate" />
      <textarea formControlName="description" [attr.placeholder]="'admin.groups.description' | translate"></textarea>
      <button type="submit">{{ 'admin.groups.createGroup' | translate }}</button>
    </form>
    <ul>
      <li *ngFor="let group of groups()">
        {{ group.displayName }} ({{ 'admin.groups.parent' | translate }}: {{ group.parentGroupId ?? ('common.none' | translate) }})
      </li>
    </ul>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GroupsComponent {
  private readonly fb = inject(FormBuilder);
  protected readonly groups = signal<Group[]>([]);
  protected readonly form = this.fb.nonNullable.group({
    name: '',
    displayName: '',
    description: '',
    parentGroupId: '',
  });

  constructor(private readonly groupService: GroupService) {
    this.groupService.list().subscribe((groups) => this.groups.set(groups));
  }

  protected create(): void {
    const value = this.form.getRawValue();
    this.groupService.create({
      name: value.name,
      displayName: value.displayName,
      description: value.description,
      parentGroupId: value.parentGroupId || null,
    }).subscribe((group) => this.groups.update((current) => [group, ...current]));
  }
}
