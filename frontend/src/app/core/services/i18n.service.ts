import { Injectable, inject } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { BehaviorSubject, Observable, catchError, firstValueFrom, map, of, tap } from 'rxjs';

export interface Language {
  code: string;
  name: string;
  nativeName: string;
}

@Injectable({ providedIn: 'root' })
export class I18nService {
  private readonly translateService = inject(TranslateService);
  private readonly currentLanguageSubject = new BehaviorSubject<string>('en');

  readonly currentLanguage$: Observable<string> = this.currentLanguageSubject.asObservable();

  readonly supportedLanguages: Language[] = [
    { code: 'en', name: 'English', nativeName: 'English' },
    { code: 'de', name: 'German', nativeName: 'Deutsch' },
    { code: 'es', name: 'Spanish', nativeName: 'Espanol' },
    { code: 'fr', name: 'French', nativeName: 'Francais' },
    { code: 'it', name: 'Italian', nativeName: 'Italiano' },
  ];

  initialize(): Promise<void> {
    this.translateService.setDefaultLang('en');

    const preferredLanguage = localStorage.getItem('preferredLanguage');
    const browserLanguage = this.translateService.getBrowserLang();

    const resolvedLanguage = this.resolveLanguage(preferredLanguage, browserLanguage);
    return firstValueFrom(this.setLanguage(resolvedLanguage).pipe(map(() => undefined)));
  }

  setLanguage(languageCode: string): Observable<string> {
    const nextLanguage = this.isSupported(languageCode) ? languageCode : 'en';

    return this.translateService.use(nextLanguage).pipe(
      tap(() => {
        this.currentLanguageSubject.next(nextLanguage);
        localStorage.setItem('preferredLanguage', nextLanguage);
      }),
      catchError(() => {
        if (nextLanguage === 'en') {
          this.currentLanguageSubject.next('en');
          localStorage.setItem('preferredLanguage', 'en');
          return of('en');
        }

        return this.translateService.use('en').pipe(
          tap(() => {
            this.currentLanguageSubject.next('en');
            localStorage.setItem('preferredLanguage', 'en');
          }),
        );
      }),
    );
  }

  private resolveLanguage(preferredLanguage: string | null, browserLanguage: string | undefined): string {
    if (preferredLanguage && this.isSupported(preferredLanguage)) {
      return preferredLanguage;
    }

    if (browserLanguage && this.isSupported(browserLanguage)) {
      return browserLanguage;
    }

    return 'en';
  }

  private isSupported(languageCode: string): boolean {
    return this.supportedLanguages.some((language) => language.code === languageCode);
  }
}
