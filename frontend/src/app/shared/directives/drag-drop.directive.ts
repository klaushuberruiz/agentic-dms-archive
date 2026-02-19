import { Directive, EventEmitter, HostListener, Output } from '@angular/core';

@Directive({ selector: '[appDragDrop]', standalone: true })
export class DragDropDirective {
  @Output() fileDropped = new EventEmitter<File>();

  @HostListener('drop', ['$event'])
  protected onDrop(event: DragEvent): void {
    event.preventDefault();
    const file = event.dataTransfer?.files?.item(0);
    if (file) {
      this.fileDropped.emit(file);
    }
  }

  @HostListener('dragover', ['$event'])
  protected onDragOver(event: DragEvent): void {
    event.preventDefault();
  }
}
