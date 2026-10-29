import { Component, inject } from "@angular/core";
import { RouterOutlet } from "@angular/router";
import { AuthService } from "./auth/auth.service";

@Component({
  selector: "erp-root",
  imports: [RouterOutlet],
  template: `
    <main>
      <header>
        <h1>ERP Core</h1>
        <span aria-live="polite">{{ auth.status() }}</span>
      </header>
      <router-outlet />
    </main>
  `,
  styles: [
    `
      :host {
        display: block;
        color: #172b4d;
        font-family: system-ui, sans-serif;
      }
      main {
        margin: 4rem auto;
        max-width: 48rem;
        padding: 0 1.5rem;
      }
      header {
        display: flex;
        align-items: baseline;
        justify-content: space-between;
      }
    `,
  ],
})
export class AppComponent {
  readonly auth = inject(AuthService);
}
