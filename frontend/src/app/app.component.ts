import { Component, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="ads-dashboard-shell">
      <aside class="ads-sidebar">
        <div class="ads-logo">Agentic DMS</div>
        <nav class="ads-sidebar-nav">
          <a routerLink="/documents" routerLinkActive="active">Documents</a>
          <a routerLink="/documents/upload" routerLinkActive="active">Upload</a>
          <a routerLink="/search" routerLinkActive="active">Search</a>
          <a routerLink="/admin/document-types" routerLinkActive="active">Admin</a>
        </nav>
      </aside>

      <div class="ads-main">
        <header class="ads-main-header">
          <h1>Document Dashboard</h1>
          <button class="ads-btn-primary" routerLink="/documents/upload">New Upload</button>
        </header>

        <section class="ads-kpi-grid">
          <article class="ads-kpi-card">
            <p>Total documents</p>
            <h3>1,248</h3>
          </article>
          <article class="ads-kpi-card">
            <p>Pending legal holds</p>
            <h3>14</h3>
          </article>
          <article class="ads-kpi-card">
            <p>Expiring this month</p>
            <h3>37</h3>
          </article>
        </section>

        <main class="ads-content">
          <router-outlet />
        </main>
      </div>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AppComponent {}
