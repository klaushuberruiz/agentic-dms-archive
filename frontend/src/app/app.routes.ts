import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'documents',
  },
  {
    path: 'unauthorized',
    loadComponent: () => import('./features/core/unauthorized/unauthorized.component').then((m) => m.UnauthorizedComponent),
  },
  {
    path: 'documents',
    canActivate: [authGuard],
    loadComponent: () => import('./features/documents/document-list/document-list.component').then((m) => m.DocumentListComponent),
  },
  {
    path: 'documents/upload',
    canActivate: [authGuard],
    loadComponent: () => import('./features/documents/document-upload/document-upload.component').then((m) => m.DocumentUploadComponent),
  },
  {
    path: 'documents/:id',
    canActivate: [authGuard],
    loadComponent: () => import('./features/documents/document-detail/document-detail.component').then((m) => m.DocumentDetailComponent),
  },
  {
    path: 'documents/:id/preview',
    canActivate: [authGuard],
    loadComponent: () => import('./features/documents/document-preview/document-preview.component').then((m) => m.DocumentPreviewComponent),
  },
  {
    path: 'search',
    canActivate: [authGuard],
    loadComponent: () => import('./features/search/search-page/search-page.component').then((m) => m.SearchPageComponent),
  },
  {
    path: 'admin/document-types',
    canActivate: [authGuard, roleGuard],
    data: { role: 'administrator' },
    loadComponent: () => import('./features/admin/document-types/document-types.component').then((m) => m.DocumentTypesComponent),
  },
  {
    path: 'admin/groups',
    canActivate: [authGuard, roleGuard],
    data: { role: 'administrator' },
    loadComponent: () => import('./features/admin/groups/groups.component').then((m) => m.GroupsComponent),
  },
  {
    path: 'admin/retention',
    canActivate: [authGuard, roleGuard],
    data: { role: 'administrator' },
    loadComponent: () => import('./features/admin/retention/retention.component').then((m) => m.RetentionComponent),
  },
  {
    path: 'admin/audit-logs',
    canActivate: [authGuard, roleGuard],
    data: { role: 'compliance_officer' },
    loadComponent: () => import('./features/admin/audit-logs/audit-logs.component').then((m) => m.AuditLogsComponent),
  },
  {
    path: 'legal/holds',
    canActivate: [authGuard, roleGuard],
    data: { role: 'legal_officer' },
    loadComponent: () => import('./features/legal/legal-holds/legal-holds.component').then((m) => m.LegalHoldsComponent),
  },
  {
    path: 'governance/version-history',
    canActivate: [authGuard, roleGuard],
    data: { role: 'compliance_officer' },
    loadComponent: () => import('./features/governance/version-history/version-history.component').then((m) => m.VersionHistoryComponent),
  },
  {
    path: 'governance/traceability',
    canActivate: [authGuard, roleGuard],
    data: { role: 'compliance_officer' },
    loadComponent: () => import('./features/governance/traceability/traceability.component').then((m) => m.TraceabilityComponent),
  },
  {
    path: 'governance/retrieval-audit',
    canActivate: [authGuard, roleGuard],
    data: { role: 'compliance_officer' },
    loadComponent: () => import('./features/governance/retrieval-audit/retrieval-audit.component').then((m) => m.RetrievalAuditComponent),
  },
  {
    path: '**',
    redirectTo: 'documents',
  },
];
