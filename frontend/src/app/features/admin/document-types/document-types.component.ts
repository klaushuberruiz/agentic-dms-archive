import { Component, ChangeDetectionStrategy } from '@angular/core';

@Component({
  selector: 'app-document-types',
  standalone: true,
  template: `<div class="document-types"></div>`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DocumentTypesComponent {
}
