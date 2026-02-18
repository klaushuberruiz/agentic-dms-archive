import { describe, it, expect } from 'vitest';
import { FileSizePipe } from './file-size.pipe';

describe('FileSizePipe', () => {
  const pipe = new FileSizePipe();

  it('should return 0 B for zero bytes', () => {
    expect(pipe.transform(0)).toBe('0 B');
  });

  it('should format bytes correctly', () => {
    expect(pipe.transform(1024)).toBe('1 KB');
  });

  it('should format megabytes correctly', () => {
    expect(pipe.transform(1048576)).toBe('1 MB');
  });
});
