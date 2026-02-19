import { ChangeDetectionStrategy, Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-document-detail',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section>
      <h2>Document Detail</h2>
      <p>Metadata</p>
      <p>Version history</p>
      <p>Legal hold status</p>
      <div>
        <button>Download</button>
        <button>Preview</button>
        <button>Edit Metadata</button>
        <button>Upload New Version</button>
        <button>Soft Delete</button>
        <button>Restore</button>
      </div>
    </section>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DocumentDetailComponent {}
