# Concept: Internationalization (i18n)

How multilanguage support is implemented in this Angular 18 + Spring Boot 3 application.
Reusable as a blueprint for other projects.

---

## Overview

| Layer    | Library                        | Mechanism                                |
|----------|--------------------------------|------------------------------------------|
| Frontend | `@ngx-translate/core` + `@ngx-translate/http-loader` | JSON translation files loaded via HTTP |
| Backend  | Spring `MessageSource`         | `.properties` message bundles            |

Supported languages: English (en), German (de), Spanish (es), French (fr), Italian (it).
Default / fallback language: English.

---

## Frontend Implementation

### 1. Dependencies

```json
// package.json
"@ngx-translate/core": "^15.0.0",
"@ngx-translate/http-loader": "^8.0.0"
```

### 2. App Configuration (`app.config.ts`)

Translation is bootstrapped at the application root using `importProvidersFrom` and an `APP_INITIALIZER` that loads translations before the app renders.

```typescript
import { ApplicationConfig, APP_INITIALIZER, importProvidersFrom, LOCALE_ID } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { TranslateModule, TranslateLoader, MissingTranslationHandler, MissingTranslationHandlerParams } from '@ngx-translate/core';
import { TranslateHttpLoader } from '@ngx-translate/http-loader';
import { registerLocaleData } from '@angular/common';
import localeDe from '@angular/common/locales/de';
import { I18nService } from './core/services/i18n.service';

// Factory: loads JSON files from assets/i18n/{lang}.json
export function HttpLoaderFactory(http: HttpClient) {
  return new TranslateHttpLoader(http, './assets/i18n/', '.json');
}

// Missing keys fall back to the key string itself
export class FallbackMissingTranslationHandler implements MissingTranslationHandler {
  handle(params: MissingTranslationHandlerParams): string {
    return params.key;
  }
}

// Register additional Angular locale data
registerLocaleData(localeDe);

// APP_INITIALIZER ensures translations are loaded before first render
export function initializeI18n(i18nService: I18nService): () => Promise<void> {
  return () => new Promise<void>((resolve) => {
    i18nService.initialize();
    setTimeout(() => resolve(), 100);
  });
}

export const appConfig: ApplicationConfig = {
  providers: [
    // ... other providers
    { provide: LOCALE_ID, useValue: 'de-DE' },

    importProvidersFrom(
      TranslateModule.forRoot({
        defaultLanguage: 'en',
        loader: {
          provide: TranslateLoader,
          useFactory: HttpLoaderFactory,
          deps: [HttpClient]
        },
        missingTranslationHandler: {
          provide: MissingTranslationHandler,
          useClass: FallbackMissingTranslationHandler
        },
        useDefaultLang: true
      })
    ),

    {
      provide: APP_INITIALIZER,
      useFactory: initializeI18n,
      deps: [I18nService],
      multi: true
    }
  ]
};
```

### 3. I18nService (`core/services/i18n.service.ts`)

Central service that wraps `TranslateService` and manages language state.

```typescript
@Injectable({ providedIn: 'root' })
export class I18nService {
  private translateService = inject(TranslateService);
  private currentLanguageSubject = new BehaviorSubject<string>('en');
  currentLanguage$: Observable<string> = this.currentLanguageSubject.asObservable();

  readonly supportedLanguages: Language[] = [
    { code: 'en', name: 'English', nativeName: 'English' },
    { code: 'de', name: 'German',  nativeName: 'Deutsch' },
    { code: 'es', name: 'Spanish', nativeName: 'Español' },
    { code: 'fr', name: 'French',  nativeName: 'Français' },
    { code: 'it', name: 'Italian', nativeName: 'Italiano' }
  ];
```

Language resolution priority:
1. `localStorage.getItem('preferredLanguage')` — saved user preference
2. `translateService.getBrowserLang()` — browser language detection
3. `'en'` — fallback

On language change, the preference is persisted to `localStorage`.
If loading a translation file fails, the service falls back to English automatically.

### 4. Translation Files

Located at `frontend/src/assets/i18n/`:

```
assets/i18n/
├── en.json    # English (default / fallback)
├── de.json    # German
├── es.json    # Spanish
├── fr.json    # French
└── it.json    # Italian
```

Keys use nested dot-notation grouping by feature:

```json
{
  "common": {
    "save": "Save",
    "cancel": "Cancel"
  },
  "nav": {
    "dashboard": "Dashboard",
    "invoices": "Invoices"
  },
  "dashboard": {
    "title": "Billing Dashboard",
    "metrics": {
      "openReceivables": "Open Receivables"
    },
    "errors": {
      "loadFinancialStatus": "Failed to load financial status"
    }
  },
  "validation": {
    "required": "This field is required",
    "minLength": "Minimum length is {{min}} characters"
  }
}
```

Key naming convention:
- `common.*` — shared UI labels (save, cancel, delete, etc.)
- `nav.*` — navigation labels
- `{feature}.*` — feature-specific keys (dashboard, invoices, payments, etc.)
- `{feature}.errors.*` — error messages per feature
- `validation.*` — form validation messages (supports interpolation with `{{param}}`)

### 5. Usage in Components

**Template (translate pipe)** — preferred for static text:

```html
<h1>{{ 'dashboard.title' | translate }}</h1>
<p>{{ 'dashboard.subtitle' | translate }}</p>
```

**TypeScript (TranslateService)** — for dynamic/programmatic use:

```typescript
private translateService = inject(TranslateService);

// In error handlers, notifications, etc.
this.translateService.get('dashboard.errors.loadInvoices').subscribe(msg => {
  this.notificationService.error(msg);
});

// Synchronous (only works if translation is already loaded)
const label = this.translateService.instant('billing.success.batchCompleted', {
  count: 5,
  total: '1250.00'
});
```

**Component setup** — import `TranslateModule` in standalone components:

```typescript
@Component({
  standalone: true,
  imports: [TranslateModule],
  template: `<span>{{ 'common.save' | translate }}</span>`
})
export class MyComponent {}
```

### 6. Language Selector Component

A reusable dropdown component in `shared/components/language-selector/`:

```typescript
@Component({
  selector: 'app-language-selector',
  standalone: true,
  imports: [CommonModule, MatSelectModule, MatFormFieldModule],
  template: `
    <mat-form-field appearance="outline">
      <mat-select [value]="currentLanguage$ | async"
                  (selectionChange)="onLanguageChange($event.value)">
        @for (lang of languages; track lang.code) {
          <mat-option [value]="lang.code">{{ lang.nativeName }}</mat-option>
        }
      </mat-select>
    </mat-form-field>
  `
})
export class LanguageSelectorComponent {
  private i18nService = inject(I18nService);
  languages = this.i18nService.supportedLanguages;
  currentLanguage$ = this.i18nService.currentLanguage$;

  onLanguageChange(newLanguage: string): void {
    this.i18nService.setLanguage(newLanguage).subscribe();
  }
}
```

---

## Backend Implementation

### 1. Message Bundles

Located at `backend/src/main/resources/`:

```
resources/
├── messages.properties       # Default (English)
├── messages_en.properties    # English
└── messages_de.properties    # German
```

Example content (`messages_de.properties`):

```properties
error.unauthorized=Nicht autorisiert
error.unauthorized.detail=Authentifizierung erforderlich, um auf diese Ressource zuzugreifen
error.forbidden=Verboten
error.badrequest=Ungültige Anfrage
error.validation.failed=Validierung fehlgeschlagen
error.notfound=Nicht gefunden
error.internal=Interner Serverfehler
error.unexpected=Ein unerwarteter Fehler ist aufgetreten. Bitte versuchen Sie es später erneut.
```

### 2. Usage in Exception Handler

Spring's `MessageSource` is injected via constructor and resolves messages based on the current request locale (`LocaleContextHolder`):

```java
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex, WebRequest request) {

        ErrorResponse error = new ErrorResponse(
            Instant.now(),
            HttpStatus.UNAUTHORIZED.value(),
            messageSource.getMessage("error.unauthorized", null,
                LocaleContextHolder.getLocale()),
            messageSource.getMessage("error.unauthorized.detail", null,
                LocaleContextHolder.getLocale()),
            request.getDescription(false).replace("uri=", ""),
            null
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }
}
```

Spring auto-configures `MessageSource` to read from `messages*.properties` by default. The locale is resolved from the `Accept-Language` HTTP header via `LocaleContextHolder`.

---

## How to Reuse in Another Application

### Frontend Checklist

1. Install dependencies:
   ```bash
   npm install @ngx-translate/core @ngx-translate/http-loader
   ```

2. Create translation JSON files in `src/assets/i18n/{lang}.json`

3. Configure `TranslateModule.forRoot()` in your app config with `HttpLoaderFactory`

4. Create an `I18nService` that:
   - Registers supported languages
   - Detects browser language
   - Persists preference to `localStorage`
   - Exposes `currentLanguage$` observable
   - Falls back to default language on error

5. Add `APP_INITIALIZER` to load translations before first render

6. Add `FallbackMissingTranslationHandler` so missing keys show the key string (not blank)

7. Import `TranslateModule` in every standalone component that uses the `translate` pipe

8. Create a `LanguageSelectorComponent` for the UI

### Backend Checklist

1. Create `messages.properties` (default) and `messages_{lang}.properties` per language

2. Inject `MessageSource` where localized messages are needed

3. Use `LocaleContextHolder.getLocale()` to resolve the current request locale

4. Spring Boot auto-configures `ResourceBundleMessageSource` — no extra config needed

### Translation Key Conventions

| Prefix          | Purpose                          |
|-----------------|----------------------------------|
| `common.*`      | Shared UI labels                 |
| `nav.*`         | Navigation items                 |
| `{feature}.*`   | Feature-specific text            |
| `{feature}.errors.*` | Feature error messages      |
| `validation.*`  | Form validation messages         |
| `errors.*`      | Global error messages            |

Use `{{paramName}}` for interpolation in both templates and programmatic access.
