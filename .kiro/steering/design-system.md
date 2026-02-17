---
inclusion: always
---
# Design System - AgenticDriverAcademy

## Source
Design system library: `agentic-design-system` (sibling workspace)

## CSS Variables

### Colors
```css
--md-sys-color-primary          /* Primary brand color */
--md-sys-color-on-primary       /* Text on primary */
--md-sys-color-surface          /* Surface background */
--md-sys-color-on-surface       /* Text on surface */
--md-sys-color-error            /* Error states */
--md-sys-color-success          /* Success states */
--surface-glass                 /* Glass morphism effect */
--surface-ground                /* Page background */
```

### Typography
```css
--md-sys-typescale-headline-large-font   /* Page titles */
--md-sys-typescale-body-large-font       /* Body text */
--md-sys-typescale-label-large-font      /* Labels, buttons */
```

### Elevation & Shape
```css
--md-sys-elevation-level1       /* Subtle shadow */
--md-sys-elevation-level2       /* Medium shadow */
--md-sys-elevation-level3       /* Prominent shadow */
--md-sys-shape-corner-small     /* 12px radius */
--md-sys-shape-corner-medium    /* 16px radius */
--md-sys-shape-corner-large     /* 24px radius */
```

## Component Usage

### Glass Card Pattern
```scss
.glass-card {
  background: var(--surface-glass);
  backdrop-filter: blur(10px);
  border-radius: var(--md-sys-shape-corner-medium);
  border: 1px solid var(--md-sys-color-outline-variant);
  box-shadow: var(--md-sys-elevation-level1);
}
```

### Touch Targets
- Minimum 44px for all interactive elements (buttons, icons)

## Rules
- ALWAYS use design tokens (never hardcode colors/spacing)
- Use `mat-icon` for icons
- Use Angular Material components with design system overrides
- Light theme only (no dark mode)
