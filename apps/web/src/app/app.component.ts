import { Component } from "@angular/core";
import { RouterOutlet } from "@angular/router";

@Component({
  selector: "erp-root",
  imports: [RouterOutlet],
  template: `
    <main>
      <h1>ERP Core</h1>
      <p>Fundação técnica pronta para os módulos aprovados do produto.</p>
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
    `,
  ],
})
export class AppComponent {}
